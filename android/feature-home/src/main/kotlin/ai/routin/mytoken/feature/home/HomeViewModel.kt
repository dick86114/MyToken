package ai.routin.mytoken.feature.home

import ai.routin.mytoken.domain.model.AppError
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageCardDensity
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.repository.CredentialRepository
import ai.routin.mytoken.domain.usage.CredentialUsageState
import ai.routin.mytoken.domain.usage.RefreshCredentialsUseCase
import ai.routin.mytoken.domain.usage.RefreshStatus
import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Data freshness buckets, mirroring the macOS popover freshness semantics. */
enum class FreshnessLevel { NEVER, JUST_NOW, RECENT, OLD, EXPIRED }

/** Outcome of a credential retry, including the Xiaomi-specific recovery path. */
sealed interface CredentialRetryResult {
    data object Completed : CredentialRetryResult
    data object NeedsLogin : CredentialRetryResult
    data object Failed : CredentialRetryResult
}

@Immutable
data class Freshness(val level: FreshnessLevel, val text: String)

/** Formats snapshot age into user-visible freshness labels (刚刚/相对时间/已过期/从未刷新). */
object FreshnessFormatter {
    fun of(fetchedAt: Instant?, now: Instant): Freshness {
        if (fetchedAt == null) return Freshness(FreshnessLevel.NEVER, "从未刷新")
        val duration = Duration.between(fetchedAt, now)
        val minutes = duration.toMinutes()
        return when {
            minutes < 2 -> Freshness(FreshnessLevel.JUST_NOW, "刚刚更新")
            minutes < 60 -> Freshness(FreshnessLevel.RECENT, "${minutes}分钟前更新")
            duration.toHours() < 24 -> Freshness(FreshnessLevel.OLD, "${duration.toHours()}小时前更新")
            else -> Freshness(FreshnessLevel.EXPIRED, "数据已过期")
        }
    }
}

/** Presentation state of one credential card on the home screen. */
@Immutable
data class CredentialCardUi(
    val credential: Credential,
    val status: RefreshStatus,
    val snapshot: UsageSnapshot?,
    val isStale: Boolean,
    val error: AppError?,
    val freshness: Freshness,
)

/** One collapsible provider section on the home screen. */
@Immutable
data class ProviderGroupUi(
    val providerId: ProviderId,
    val displayName: String,
    val isCollapsed: Boolean,
    val cards: List<CredentialCardUi>,
)

/** Whole-screen state rendered by [HomeScreen]. Single source: [HomeViewModel.state]. */
@Immutable
data class HomeUiState(
    val isLoading: Boolean = true,
    val usageCardDensity: UsageCardDensity = UsageCardDensity.FULL,
    val cards: List<CredentialCardUi> = emptyList(),
    val groups: List<ProviderGroupUi> = emptyList(),
    val credentialCount: Int = 0,
    val isRefreshingAll: Boolean = false,
    val lastUpdatedAt: Instant? = null,
    val lastUpdatedText: String? = null,
)

/**
 * Home dashboard state holder. Unidirectional flow:
 * CredentialRepository + RefreshCredentialsUseCase states -> [state] (StateFlow) -> Compose.
 */
class HomeViewModel(
    private val repository: CredentialRepository,
    private val refreshUseCase: RefreshCredentialsUseCase,
    private val credentialOrderIds: Flow<List<String>> = emptyFlow(),
    usageCardDensityFlow: Flow<UsageCardDensity> = flowOf(UsageCardDensity.FULL),
    private val onUsageCardDensityChange: suspend (UsageCardDensity) -> Unit = {},
    private val clock: Clock = Clock.systemUTC(),
    refreshOnStart: Boolean = true,
    private val retryOnFailure: Boolean = false,
    private val nowTickIntervalMillis: Long? = DEFAULT_NOW_TICK_MILLIS,
    // Ticking happens off the main dispatcher: an infinite delay loop on the
    // main looper keeps Robolectric/Compose idle detection from ever settling.
    nowTickDispatcher: CoroutineDispatcher = Dispatchers.Default,
) : ViewModel() {

    private val collapsedGroups = MutableStateFlow<Set<ProviderId>>(emptySet())
    private val isRefreshingAll = MutableStateFlow(false)
    private val nowTickCounter = MutableStateFlow(0L)

    /** Relative-time re-render tick counter; exposed for tests. */
    internal val nowTick: StateFlow<Long> = nowTickCounter.asStateFlow()

    private val nowTickEnabled = MutableStateFlow(true)

    /** 卡片密度：来自本地显示设置，切换后立即反映到首页。 */
    val usageCardDensity: StateFlow<UsageCardDensity> = usageCardDensityFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UsageCardDensity.FULL,
        )

    fun setUsageCardDensity(density: UsageCardDensity) {
        viewModelScope.launch { onUsageCardDensityChange(density) }
    }

    private val orderedCredentialsFlow = combine(
        repository.observeCredentials(),
        credentialOrderIds,
    ) { credentials, orderIds ->
        orderCredentials(credentials, orderIds)
    }

    /**
     * Lifecycle-aware ticker switch: the app layer pauses ticking while the
     * activity is not resumed so backgrounded windows do no re-render work.
     */
    fun setNowTickEnabled(enabled: Boolean) {
        nowTickEnabled.value = enabled
    }

    /** Credential IDs whose refresh is currently in flight (guarded by [inFlightMutex]). */
    private val inFlight = mutableSetOf<UUID>()
    private val inFlightMutex = Mutex()

    /** Atomically claims [id]; returns false when a refresh for it is already running. */
    private suspend fun tryBeginRefresh(id: UUID): Boolean = inFlightMutex.withLock {
        inFlight.add(id)
    }

    private suspend fun endRefresh(id: UUID) {
        inFlightMutex.withLock { inFlight.remove(id) }
    }

    init {
        // Re-render relative freshness labels periodically (when resumed);
        // disabled in tests via null interval.
        if (nowTickIntervalMillis != null) {
            viewModelScope.launch(nowTickDispatcher) {
                while (isActive) {
                    delay(nowTickIntervalMillis)
                    if (nowTickEnabled.value) {
                        nowTickCounter.update { it + 1 }
                    }
                }
            }
        }
        viewModelScope.launch {
            // Show last-known data (Ready + stale) before any network round trip.
            refreshUseCase.restoreFromCache()
            if (refreshOnStart) {
                refreshAll(retryOnFailure)
            }
        }
    }

    val state: StateFlow<HomeUiState> =
        combine(
            orderedCredentialsFlow,
            refreshUseCase.states,
            collapsedGroups,
            isRefreshingAll,
            nowTick,
        ) { credentials, usageStates, collapsed, refreshingAll, _ ->
            buildState(credentials, usageStates, collapsed, refreshingAll)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = HomeUiState(isLoading = true),
        )

    /**
     * Refreshes every enabled credential. Duplicate suppression:
     * - a second [refreshAll] while one is running is a no-op (CAS on the flag);
     * - a credential already being refreshed (by this call or by [refreshCredential])
     *   is skipped, so no duplicate concurrent provider request is issued.
     */
    fun refreshAll() {
        refreshAll(retryOnFailure)
    }

    fun refreshAll(retryOnFailure: Boolean) {
        if (!isRefreshingAll.compareAndSet(expect = false, update = true)) return
        viewModelScope.launch {
            try {
                val credentials = repository.observeCredentials().first()
                coroutineScope {
                    credentials.forEach { credential ->
                        if (tryBeginRefresh(credential.id)) {
                            launch {
                                try {
                                    refreshWithRetry(credential, retryOnFailure)
                                } finally {
                                    endRefresh(credential.id)
                                }
                            }
                        }
                    }
                }
            } finally {
                isRefreshingAll.value = false
            }
        }
    }

    /**
     * Uses the same phone-local order persisted by the credential page. IDs that
     * have not been ordered yet keep repository order after the ordered prefix.
     */
    private fun orderCredentials(
        credentials: List<Credential>,
        orderIds: List<String>,
    ): List<Credential> {
        val byId = credentials.associateBy { it.id.toString() }
        val ordered = orderIds
            .asSequence()
            .distinct()
            .mapNotNull(byId::get)
            .toList()
        val orderedIdSet = ordered.mapTo(mutableSetOf()) { it.id.toString() }
        val remainder = credentials.filter { it.id.toString() !in orderedIdSet }
        return ordered + remainder
    }

    /**
     * Refreshes one credential; failures keep its last snapshot and mark it stale.
     * A call while the same credential is already refreshing is a no-op.
     */
    fun refreshCredential(credential: Credential) {
        viewModelScope.launch { refreshCredentialAndAwait(credential) }
    }

    /**
     * Refreshes one credential and suspends until its state settles. Used by the
     * retry flow when it must inspect the result before deciding whether to open
     * the Xiaomi login dialog.
     */
    suspend fun refreshCredentialAndAwait(credential: Credential) {
        if (tryBeginRefresh(credential.id)) {
            try {
                refreshWithRetry(credential, retryOnFailure)
            } finally {
                endRefresh(credential.id)
            }
        }
    }

    /**
     * Retries one credential. Xiaomi MiMo first refreshes the saved secret from
     * the current WebView cookie; a missing cookie or an authentication failure
     * asks the UI to open the login dialog. Other providers keep the normal
     * single-credential refresh path.
     */
    suspend fun retryCredentialAndAwait(
        credential: Credential,
        cookieReader: suspend () -> String?,
    ): CredentialRetryResult {
        if (credential.providerId != ProviderId.Xiaomi) {
            refreshCredentialAndAwait(credential)
            return CredentialRetryResult.Completed
        }

        val cookie = cookieReader()?.trim().orEmpty()
        if (cookie.isEmpty()) {
            return CredentialRetryResult.NeedsLogin
        }

        try {
            repository.save(credential, CredentialSecret.BearerToken(cookie))
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Throwable) {
            return CredentialRetryResult.Failed
        }

        refreshCredentialAndAwait(credential)
        return if (refreshUseCase.states.value[credential.id]?.error is AppError.Authentication) {
            CredentialRetryResult.NeedsLogin
        } else {
            CredentialRetryResult.Completed
        }
    }

    /** Saves the cookie captured by the login dialog, then refreshes the credential. */
    suspend fun completeXiaomiLoginAndAwait(
        credential: Credential,
        cookie: String,
    ): CredentialRetryResult {
        val normalized = cookie.trim()
        if (normalized.isEmpty()) {
            return CredentialRetryResult.NeedsLogin
        }
        return try {
            repository.save(credential, CredentialSecret.BearerToken(normalized))
            refreshCredentialAndAwait(credential)
            CredentialRetryResult.Completed
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error
        } catch (_: Throwable) {
            CredentialRetryResult.Failed
        }
    }

    private suspend fun refreshWithRetry(credential: Credential, retryOnFailure: Boolean) {
        refreshUseCase.refresh(credential)
        if (retryOnFailure && refreshUseCase.states.value[credential.id]?.status == RefreshStatus.Failed) {
            delay(FAILURE_RETRY_DELAY_MILLIS)
            refreshUseCase.refresh(credential)
        }
    }

    fun toggleGroup(providerId: ProviderId) {
        collapsedGroups.update { current ->
            if (providerId in current) current - providerId else current + providerId
        }
    }

    private fun buildState(
        credentials: List<Credential>,
        usageStates: Map<UUID, CredentialUsageState>,
        collapsed: Set<ProviderId>,
        isRefreshingAll: Boolean,
    ): HomeUiState {
        val now = clock.instant()
        val allCards = credentials.map { it.toCardUi(usageStates[it.id], now) }
        val groups = ProviderId.entries
            .filter { providerId -> credentials.any { it.providerId == providerId } }
            .map { providerId ->
                val cards = credentials
                    .filter { it.providerId == providerId }
                    .map { it.toCardUi(usageStates[it.id], now) }
                ProviderGroupUi(
                    providerId = providerId,
                    displayName = ProviderCatalog.displayName(providerId),
                    isCollapsed = providerId in collapsed,
                    cards = cards,
                )
            }
        val lastUpdatedAt = usageStates.values.mapNotNull { it.snapshot?.fetchedAt }.maxOrNull()
        return HomeUiState(
            isLoading = false,
            cards = allCards,
            groups = groups,
            credentialCount = credentials.size,
            isRefreshingAll = isRefreshingAll,
            lastUpdatedAt = lastUpdatedAt,
            lastUpdatedText = lastUpdatedAt?.let { FreshnessFormatter.of(it, now).text },
        )
    }

    private fun Credential.toCardUi(
        usageState: CredentialUsageState?,
        now: Instant,
    ): CredentialCardUi {
        val snapshot = usageState?.snapshot
        val status = if (!isEnabled) RefreshStatus.Disabled else usageState?.status ?: RefreshStatus.Loading
        // A failed refresh keeps the last successful snapshot; that data is stale regardless
        // of its fetch time because the authoritative refresh did not succeed.
        val freshness = if (usageState?.status == RefreshStatus.Failed && snapshot != null) {
            Freshness(FreshnessLevel.EXPIRED, "数据已过期")
        } else {
            FreshnessFormatter.of(snapshot?.fetchedAt, now)
        }
        return CredentialCardUi(
            credential = this,
            status = status,
            snapshot = snapshot,
            isStale = usageState?.isStale ?: false,
            error = usageState?.error,
            freshness = freshness,
        )
    }

    private companion object {
        const val DEFAULT_NOW_TICK_MILLIS: Long = 30_000L
        const val FAILURE_RETRY_DELAY_MILLIS: Long = 1_000L
    }
}
