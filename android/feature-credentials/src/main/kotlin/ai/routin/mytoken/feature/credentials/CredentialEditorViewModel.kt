package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.alerts.CredentialAlertThresholdStore
import ai.routin.mytoken.domain.model.Credential
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.CredentialMetadataKey
import ai.routin.mytoken.domain.model.CredentialSecret
import ai.routin.mytoken.domain.model.ProviderId
import ai.routin.mytoken.domain.repository.CredentialRepository
import ai.routin.mytoken.domain.usage.UsageProvider
import ai.routin.mytoken.domain.usage.UsageProviderException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Whole-screen state rendered by [CredentialEditorScreen]. */
data class CredentialEditorUiState(
    val isLoading: Boolean = true,
    val isNew: Boolean = true,
    val providerId: ProviderId = ProviderId.Routin,
    val credentialKind: CredentialKind = CredentialKind.BearerApiKey,
    val name: String = "",
    val apiKey: String = "",
    val accessKeyID: String = "",
    val secretAccessKey: String = "",
    val region: String = "",
    val baseURL: String = "",
    val userID: String = "",
    val xiaomiUsageKind: String = "api",
    val websiteURL: String = "",
    val balanceWarningThreshold: String = "",
    val customAlertThresholdEnabled: Boolean = false,
    val alertThresholdLowPercent: Int = AlertThresholds.DEFAULT.lowPercent,
    val alertThresholdHighPercent: Int = AlertThresholds.DEFAULT.highPercent,
    val globalAlertThresholdLowPercent: Int = AlertThresholds.DEFAULT.lowPercent,
    val globalAlertThresholdHighPercent: Int = AlertThresholds.DEFAULT.highPercent,
    val isSecretVisible: Boolean = false,
    val isSaving: Boolean = false,
    val isTestingConnection: Boolean = false,
    val testResultMessage: String? = null,
    val testSucceeded: Boolean? = null,
    val validationError: String? = null,
    val saveCompleted: Boolean = false,
)

/**
 * Editor state holder for a single credential ([credentialId] == null means "new").
 * Secrets are only held in memory for the form; persistence goes through
 * [CredentialRepository.save] so the encrypted secret store stays the single owner.
 */
class CredentialEditorViewModel(
    private val repository: CredentialRepository,
    private val providers: Map<ProviderId, UsageProvider>,
    private val alertThresholdStore: CredentialAlertThresholdStore,
    private val globalAlertThresholds: AlertThresholds = AlertThresholds.DEFAULT,
    private val credentialId: UUID? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CredentialEditorUiState(
            alertThresholdLowPercent = globalAlertThresholds.lowPercent,
            alertThresholdHighPercent = globalAlertThresholds.highPercent,
            globalAlertThresholdLowPercent = globalAlertThresholds.lowPercent,
            globalAlertThresholdHighPercent = globalAlertThresholds.highPercent,
        ),
    )
    val state: StateFlow<CredentialEditorUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val existing = credentialId?.let { id ->
                repository.observeCredentials().first().firstOrNull { it.id == id }
            }
            if (existing == null) {
                _state.update { it.copy(isLoading = false, isNew = true) }
                return@launch
            }
            val secret = try {
                repository.readSecret(existing.id)
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                null
            }
            val usageKind = existing.metadata[CredentialMetadataKey.UsageKind] ?: "api"
            val supportsAlertThreshold = CredentialEditorValidation.supportsPercentAlertThreshold(
                existing.providerId,
                usageKind,
            )
            val alertThreshold = if (supportsAlertThreshold) {
                try {
                    alertThresholdStore.overrides.first()[existing.id]
                } catch (error: CancellationException) {
                    throw error
                } catch (_: Throwable) {
                    null
                }
            } else {
                null
            }
            val prefill = prefilledSecret(existing.credentialKind, secret)
            _state.update { current ->
                current.copy(
                    isLoading = false,
                    isNew = false,
                    providerId = existing.providerId,
                    credentialKind = existing.credentialKind,
                    name = existing.name,
                    apiKey = prefill.apiKey,
                    accessKeyID = prefill.accessKeyID,
                    secretAccessKey = prefill.secretAccessKey,
                    region = existing.metadata[CredentialMetadataKey.Region].orEmpty(),
                    baseURL = existing.metadata[CredentialMetadataKey.BaseURL].orEmpty(),
                    userID = existing.metadata[CredentialMetadataKey.UserID].orEmpty(),
                    xiaomiUsageKind = usageKind,
                    websiteURL = existing.metadata[CredentialMetadataKey.WebsiteURL].orEmpty(),
                    balanceWarningThreshold =
                        existing.metadata[CredentialMetadataKey.BalanceWarningThreshold].orEmpty(),
                    customAlertThresholdEnabled = alertThreshold != null,
                    alertThresholdLowPercent =
                        alertThreshold?.lowPercent ?: globalAlertThresholds.lowPercent,
                    alertThresholdHighPercent =
                        alertThreshold?.highPercent ?: globalAlertThresholds.highPercent,
                )
            }
        }
    }

    private fun prefilledSecret(
        kind: CredentialKind,
        secret: CredentialSecret?,
    ): CredentialEditorUiState = when (kind) {
        CredentialKind.BearerApiKey -> CredentialEditorUiState(
            apiKey = (secret as? CredentialSecret.BearerToken)?.token.orEmpty(),
        )
        CredentialKind.ApiKey -> CredentialEditorUiState(
            apiKey = (secret as? CredentialSecret.ApiKey)?.key.orEmpty(),
        )
        CredentialKind.AccessKeyPair -> CredentialEditorUiState(
            accessKeyID = (secret as? CredentialSecret.AccessKeyPair)?.accessKeyID.orEmpty(),
            secretAccessKey = (secret as? CredentialSecret.AccessKeyPair)?.secretAccessKey.orEmpty(),
        )
    }

    fun setProviderId(providerId: ProviderId) {
        _state.update {
            it.copy(
                providerId = providerId,
                credentialKind = defaultKind(providerId),
                testResultMessage = null,
                testSucceeded = null,
                validationError = null,
            )
        }
    }

    fun setName(value: String) = _state.update { it.copy(name = value) }
    fun setApiKey(value: String) = _state.update { state ->
        val normalized = value.trimStart().lowercase()
        val usageKind = if (state.providerId == ProviderId.Xiaomi) {
            when {
                normalized.startsWith("tp-") -> "plan"
                normalized.startsWith("sk-") -> "api"
                else -> state.xiaomiUsageKind
            }
        } else {
            state.xiaomiUsageKind
        }
        state.copy(apiKey = value, xiaomiUsageKind = usageKind)
    }
    fun setAccessKeyID(value: String) = _state.update { it.copy(accessKeyID = value) }
    fun setSecretAccessKey(value: String) = _state.update { it.copy(secretAccessKey = value) }
    fun setRegion(value: String) = _state.update { it.copy(region = value) }
    fun setBaseURL(value: String) = _state.update { it.copy(baseURL = value) }
    fun setUserID(value: String) = _state.update { it.copy(userID = value) }
    fun setXiaomiUsageKind(value: String) = _state.update { it.copy(xiaomiUsageKind = value) }
    fun setWebsiteURL(value: String) = _state.update { it.copy(websiteURL = value) }
    fun setBalanceWarningThreshold(value: String) =
        _state.update { it.copy(balanceWarningThreshold = value) }

    fun setCustomAlertThresholdEnabled(enabled: Boolean) =
        _state.update { it.copy(customAlertThresholdEnabled = enabled) }

    /** Normalizes to 1..99 with a one-point gap; the slider is the only input source. */
    fun setAlertThresholdRange(lowPercent: Int, highPercent: Int) {
        val low = lowPercent.coerceIn(
            AlertThresholds.MIN_PERCENT,
            AlertThresholds.MAX_PERCENT - 1,
        )
        val high = highPercent.coerceIn(low + 1, AlertThresholds.MAX_PERCENT)
        _state.update {
            it.copy(
                alertThresholdLowPercent = low,
                alertThresholdHighPercent = high,
            )
        }
    }

    fun toggleSecretVisible() {
        _state.update { it.copy(isSecretVisible = !it.isSecretVisible) }
    }

    fun save() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, validationError = null) }
            val validated = try {
                CredentialEditorValidation.validate(_state.value.providerId, formFields())
            } catch (error: CredentialValidationException) {
                _state.update { it.copy(isSaving = false, validationError = requireNotNull(error.message)) }
                return@launch
            }
            val state = _state.value
            val supportsThresholds = CredentialEditorValidation.supportsPercentAlertThreshold(
                providerId = state.providerId,
                usageKind = state.xiaomiUsageKind,
            )
            // Slider inputs are already normalized; switching custom off (or a
            // provider without percent metrics) clears the override.
            val alertThresholds = if (supportsThresholds && state.customAlertThresholdEnabled) {
                runCatching {
                    AlertThresholds(
                        lowPercent = state.alertThresholdLowPercent,
                        highPercent = state.alertThresholdHighPercent,
                    )
                }.getOrNull()
            } else {
                null
            }
            try {
                val existing = credentialId?.let { id ->
                    repository.observeCredentials().first().firstOrNull { it.id == id }
                }
                val id = existing?.id ?: UUID.randomUUID()
                val sortOrder = existing?.sortOrder
                    ?: (repository.observeCredentials().first().maxOfOrNull { it.sortOrder }?.plus(1) ?: 0)
                val credential = Credential(
                    id = id,
                    providerId = _state.value.providerId,
                    credentialKind = validated.credentialKind,
                    name = validated.name,
                    isEnabled = existing?.isEnabled ?: true,
                    sortOrder = sortOrder,
                    metadata = validated.metadata,
                )
                repository.save(credential, validated.secret)
                persistAlertThresholds(id, alertThresholds)
                _state.update { it.copy(isSaving = false, saveCompleted = true) }
            } catch (error: CancellationException) {
                throw error
            } catch (_: Throwable) {
                _state.update { it.copy(isSaving = false, validationError = "保存失败，请重试") }
            }
        }
    }

    /**
     * Thresholds are notification-only: a local persistence hiccup must not turn a
     * successful credential save into a visible failure, so this step is best-effort.
     * Custom off (or providers without percent metrics) clears any stale override.
     */
    private suspend fun persistAlertThresholds(credentialId: UUID, thresholds: AlertThresholds?) {
        try {
            if (thresholds != null) {
                alertThresholdStore.setOverride(credentialId, thresholds)
            } else {
                alertThresholdStore.clearOverride(credentialId)
            }
        } catch (error: CancellationException) {
            throw error
        } catch (_: Throwable) {
            // Non-fatal: the credential itself is already saved.
        }
    }

    /**
     * Connection test for the currently entered form values. A failed test is only a
     * result message — saving remains possible (aligned with the macOS editor).
     */
    fun testConnection() {
        if (_state.value.isTestingConnection) return
        viewModelScope.launch {
            _state.update { it.copy(isTestingConnection = true, testResultMessage = null, testSucceeded = null) }
            val result: Result<*> = runCatching {
                val validated = CredentialEditorValidation.validate(_state.value.providerId, formFields())
                val provider = providers[_state.value.providerId]
                    ?: throw UsageProviderException.InvalidCredential("暂不支持该供应商的用量查询")
                val probe = Credential(
                    id = credentialId ?: UUID.randomUUID(),
                    providerId = _state.value.providerId,
                    credentialKind = validated.credentialKind,
                    name = validated.name,
                    metadata = validated.metadata,
                )
                provider.fetchUsage(probe, validated.secret).getOrThrow()
            }
            result.fold(
                onSuccess = {
                    _state.update {
                        it.copy(isTestingConnection = false, testSucceeded = true, testResultMessage = "连接成功")
                    }
                },
                onFailure = { error ->
                    if (error is CancellationException) throw error
                    _state.update {
                        it.copy(
                            isTestingConnection = false,
                            testSucceeded = false,
                            testResultMessage = error.message ?: "连接失败，请稍后重试",
                        )
                    }
                },
            )
        }
    }

    private fun formFields(): CredentialEditorValidation.FormFields {
        val s = _state.value
        return CredentialEditorValidation.FormFields(
            name = s.name,
            apiKey = s.apiKey,
            accessKeyID = s.accessKeyID,
            secretAccessKey = s.secretAccessKey,
            region = s.region,
            newAPIBaseURL = s.baseURL,
            newAPIUserID = s.userID,
            xiaomiUsageKind = s.xiaomiUsageKind,
            websiteURL = s.websiteURL,
            balanceWarningThreshold = s.balanceWarningThreshold,
        )
    }

}

/** 各供应商默认凭证类型，编辑页与 ViewModel 共用一份。 */
internal fun defaultKind(providerId: ProviderId): CredentialKind = when (providerId) {
    ProviderId.Routin, ProviderId.NewAPI, ProviderId.CommandCode, ProviderId.OpenCode,
    ProviderId.Xiaomi -> CredentialKind.BearerApiKey
    ProviderId.DeepSeek, ProviderId.Glm -> CredentialKind.ApiKey
    ProviderId.Volcengine -> CredentialKind.AccessKeyPair
}
