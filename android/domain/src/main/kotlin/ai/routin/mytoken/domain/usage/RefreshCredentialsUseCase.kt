package ai.routin.mytoken.domain.usage

import ai.routin.mytoken.domain.model.AppError
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.model.UsageSnapshot
import ai.routin.mytoken.domain.repository.CredentialRepository
import java.time.Clock
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Per-credential refresh status. */
enum class RefreshStatus { Loading, Ready, Failed, Disabled }

/**
 * State of a single credential's usage refresh. A failed refresh keeps the last
 * successful snapshot and marks it stale; no fabricated metric values are produced.
 */
data class CredentialUsageState(
    val status: RefreshStatus,
    val snapshot: UsageSnapshot? = null,
    val isStale: Boolean = false,
    val error: AppError? = null,
)

/**
 * Refreshes usage for all enabled credentials, each with an independent state.
 * Concurrency across credentials is limited to [maxConcurrency] simultaneous fetches.
 */
class RefreshCredentialsUseCase(
    private val repository: CredentialRepository,
    private val providers: Map<ProviderId, UsageProvider>,
    private val clock: Clock = Clock.systemUTC(),
    private val maxConcurrency: Int = DEFAULT_MAX_CONCURRENCY,
    private val healthThresholdSource: UsageHealthThresholdSource = DefaultUsageHealthThresholdSource,
) {
    init {
        require(maxConcurrency >= 1) { "maxConcurrency must be positive" }
    }

    private val semaphore = Semaphore(maxConcurrency)
    private val inFlightMutex = Mutex()
    private val inFlight = mutableMapOf<UUID, CompletableDeferred<Unit>>()
    private val _states = MutableStateFlow<Map<UUID, CredentialUsageState>>(emptyMap())
    val states: StateFlow<Map<UUID, CredentialUsageState>> = _states.asStateFlow()

    /** Refreshes every credential; disabled credentials are reported as [RefreshStatus.Disabled]. */
    suspend fun refreshAll() {
        val credentials = repository.observeCredentials().first()
        coroutineScope {
            credentials.forEach { credential ->
                if (credential.isEnabled) {
                    launch { refresh(credential) }
                } else {
                    setDisabled(credential.id)
                }
            }
        }
    }

    /**
     * Fills per-credential states from the repository's cached snapshots so the
     * UI shows last-known data (Ready + stale) right after process start instead
     * of a wall of "刷新中" placeholders. Never overwrites a state that already
     * carries a snapshot (e.g. an in-flight refresh result); a missing or
     * unreadable cache entry is silently skipped.
     */
    suspend fun restoreFromCache() {
        val credentials = try {
            repository.observeCredentials().first()
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            return
        }
        credentials.forEach { credential ->
            if (!credential.isEnabled) return@forEach
            val cached = try {
                repository.cachedSnapshot(credential.id)
            } catch (cause: Throwable) {
                if (cause is CancellationException) throw cause
                null
            } ?: return@forEach
            val normalized = normalize(credential.id, cached)
            update(credential.id) { previous ->
                if (previous.snapshot == null) {
                    CredentialUsageState(
                        status = RefreshStatus.Ready,
                        snapshot = normalized,
                        isStale = true
                    )
                } else {
                    previous
                }
            }
        }
    }

    /**
     * Recomputes health states for every already-loaded snapshot using the source's
     * current thresholds, without another network refresh. Status, stale flags and
     * fetchedAt are preserved; changed snapshots are written back to the cache
     * (best-effort) so a cold start keeps the same colors.
     */
    suspend fun reapplyHealthThresholds() {
        val credentials = try {
            repository.observeCredentials().first()
        } catch (cause: Throwable) {
            if (cause is CancellationException) throw cause
            return
        }
        if (credentials.isEmpty()) return
        val credentialsById = credentials.associateBy { it.id }
        val changed = mutableMapOf<UUID, UsageSnapshot>()

        _states.update { current ->
            changed.clear()
            if (current.isEmpty()) return@update current
            current.mapValues { (id, state) ->
                val snapshot = state.snapshot ?: return@mapValues state
                val credential = credentialsById[id] ?: return@mapValues state
                val normalized = normalize(credential.id, snapshot)
                if (normalized == snapshot) {
                    state
                } else {
                    changed[id] = normalized
                    state.copy(snapshot = normalized)
                }
            }
        }

        for ((id, snapshot) in changed) {
            try {
                repository.cacheSnapshot(snapshot)
            } catch (cause: CancellationException) {
                throw cause
            } catch (_: Throwable) {
                // Non-fatal: the in-memory state is already correct for this session.
            }
        }
    }

    /** Refreshes a single credential, preserving the previous snapshot on failure. */
    suspend fun refresh(credential: Credential) {
        if (!credential.isEnabled) {
            setDisabled(credential.id)
            return
        }
        val (deferred, isOwner) = inFlightMutex.withLock {
            inFlight[credential.id]?.let { return@withLock it to false }
            CompletableDeferred<Unit>().also { inFlight[credential.id] = it } to true
        }
        if (!isOwner) {
            deferred.join()
            return
        }
        try {
            performRefresh(credential)
        } finally {
            inFlightMutex.withLock {
                if (inFlight[credential.id] === deferred) {
                    inFlight.remove(credential.id)
                }
            }
            deferred.complete(Unit)
        }
    }

    private suspend fun performRefresh(credential: Credential) {
        if (!credential.isEnabled) {
            setDisabled(credential.id)
            return
        }
        update(credential.id) { previous ->
            CredentialUsageState(
                status = RefreshStatus.Loading,
                snapshot = previous.snapshot,
                isStale = previous.isStale
            )
        }
        semaphore.withPermit {
            try {
                val provider = providers[credential.providerId]
                    ?: throw UsageProviderException.InvalidCredential(
                        "暂不支持该供应商的用量查询"
                    )
                val secret = repository.readSecret(credential.id)
                    ?: throw AppError.Storage("凭证密钥缺失，请重新导入")
                val snapshot = normalize(
                    credential.id,
                    provider.fetchUsage(credential, secret).getOrThrow(),
                )
                // Best-effort cache write: a failed cache write must not turn a
                // successful provider fetch into a refresh failure.
                try {
                    repository.cacheSnapshot(snapshot)
                } catch (cacheError: Throwable) {
                    if (cacheError is CancellationException) throw cacheError
                    println("MyToken: failed to cache usage snapshot: ${cacheError.message}")
                }
                update(credential.id) { previous ->
                    CredentialUsageState(
                        status = RefreshStatus.Ready,
                        snapshot = snapshot,
                        isStale = false
                    )
                }
            } catch (error: CancellationException) {
                // 取消不是失败：保持 Loading 状态并向上传播取消。
                throw error
            } catch (error: Throwable) {
                update(credential.id) { previous ->
                    CredentialUsageState(
                        status = RefreshStatus.Failed,
                        snapshot = previous.snapshot,
                        isStale = previous.snapshot != null,
                        error = mapError(error)
                    )
                }
            }
        }
    }

    private fun normalize(credentialId: UUID, snapshot: UsageSnapshot): UsageSnapshot =
        UsageHealthNormalizer.normalize(snapshot, healthThresholdSource.thresholdsFor(credentialId))

    private fun setDisabled(id: UUID) {
        update(id) { CredentialUsageState(status = RefreshStatus.Disabled) }
    }

    private fun update(id: UUID, transform: (CredentialUsageState) -> CredentialUsageState) {
        // Atomic CAS loop: concurrent refreshes of different credentials must not
        // lose each other's state writes (read-modify-write on `.value` is not safe here).
        _states.update { current ->
            current + (id to transform(current[id] ?: CredentialUsageState(RefreshStatus.Loading)))
        }
    }

    private fun mapError(error: Throwable): AppError = when (error) {
        is AppError -> error
        is UsageProviderException.Unauthorized -> AppError.Authentication(error.message)
        is UsageProviderException.RateLimited -> AppError.Network(error.message)
        is UsageProviderException.Transport -> AppError.Network(error.message)
        is UsageProviderException.ProviderUnavailable -> AppError.Network(error.message)
        is UsageProviderException.ProviderMessage -> providerMessageError(error)
        is UsageProviderException.InvalidResponse -> AppError.Decode(error.message)
        is UsageProviderException.InvalidCredential -> AppError.Unknown(error.message)
        else -> AppError.Unknown(error.message ?: "刷新用量失败")
    }

    private fun providerMessageError(error: UsageProviderException.ProviderMessage): AppError {
        val message = error.message.orEmpty()
        val normalized = message.lowercase()
        val isAuthenticationFailure = normalized.contains("未登录") ||
            normalized.contains("unauthorized") ||
            normalized.contains("invalid token") ||
            normalized.contains("认证失败") ||
            normalized.contains("登录已失效")
        return if (isAuthenticationFailure) {
            AppError.Authentication(message)
        } else {
            AppError.Network(message)
        }
    }

    companion object {
        const val DEFAULT_MAX_CONCURRENCY: Int = 4
    }
}
