package ai.routin.mytoken.feature.credentials

import ai.routin.mytoken.core.ui.MyTokenAdaptiveContent
import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenSegmented
import ai.routin.mytoken.core.ui.MyTokenSection
import ai.routin.mytoken.core.ui.MyTokenSize
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenTextField
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.ProviderLogoPlate
import ai.routin.mytoken.domain.model.CredentialKind
import ai.routin.mytoken.domain.model.ProviderId
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

/**
 * Credential editor. Fields are shown dynamically per [CredentialKind]:
 * Bearer API Key (Routin/NewAPI), API Key (DeepSeek/GLM), or
 * Access Key / Secret Access Key (Volcengine). Secrets are masked by default with a
 * temporary reveal toggle; validation messages align with the macOS editor.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CredentialEditorScreen(
    state: CredentialEditorUiState,
    onBack: () -> Unit,
    onProviderChange: (ProviderId) -> Unit,
    onNameChange: (String) -> Unit,
    onApiKeyChange: (String) -> Unit,
    onAccessKeyIDChange: (String) -> Unit,
    onSecretAccessKeyChange: (String) -> Unit,
    onRegionChange: (String) -> Unit,
    onBaseURLChange: (String) -> Unit,
    onUserIDChange: (String) -> Unit,
    onXiaomiUsageKindChange: (String) -> Unit,
    onWebsiteURLChange: (String) -> Unit,
    onToggleSecretVisible: () -> Unit,
    onTestConnection: () -> Unit,
    onSave: () -> Unit,
    layoutMode: MyTokenLayoutMode = MyTokenLayoutMode.Compact,
) {
    val colors = MyTokenThemeTokens.colors
    var providerSheetOpen by remember { mutableStateOf(false) }
    Scaffold(
        topBar = {
            EditorTopBar(
                title = if (state.isNew) "新增凭证" else "编辑凭证",
                onBack = onBack,
            )
        },
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.canvas)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    MyTokenButton(
                        onClick = onTestConnection,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isTestingConnection,
                        text = if (state.isTestingConnection) "测试中…" else "测试连接",
                    )
                    MyTokenButton(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = !state.isSaving,
                        tone = MyTokenButtonTone.Primary,
                        text = if (state.isSaving) "保存中…" else "保存",
                    )
                }
            }
        },
    ) { padding ->
        if (state.isLoading) {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {}
            return@Scaffold
        }
        MyTokenAdaptiveContent(
            maxWidth = 720.dp,
            horizontalPadding = 16.dp,
            modifier = Modifier.padding(padding).fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
                    .testTag("editor_form"),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (state.isNew) {
                    Text(
                        text = "选择供应商，填写最少的字段。",
                        style = MyTokenType.caption,
                        color = colors.muted,
                    )
                    ProviderRow(
                        providerId = state.providerId,
                        kind = state.credentialKind,
                        onClick = { providerSheetOpen = true },
                    )
                } else {
                    Text(
                        text = "${ProviderNames.displayName(state.providerId)} · ${kindLabel(state.credentialKind)}",
                        style = MyTokenType.caption,
                        color = colors.muted,
                        modifier = Modifier.testTag("editor_provider_static"),
                    )
                }

                MyTokenSection(title = "基本信息") {
                    MyTokenTextField(
                        value = state.name,
                        onValueChange = onNameChange,
                        label = "名称",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_name"),
                    )
                }

                MyTokenSection(title = "凭证") {
                    if (state.providerId == ProviderId.Xiaomi) {
                        Text(
                            text = "查询方式",
                            style = MyTokenType.label,
                            color = colors.muted,
                        )
                        MyTokenSegmented(
                            options = listOf("API 按量", "Token Plan"),
                            selectedIndex = if (state.xiaomiUsageKind == "plan") 1 else 0,
                            onSelect = { index ->
                                onXiaomiUsageKindChange(if (index == 1) "plan" else "api")
                            },
                        )
                        SecretField(
                            label = "API Key（sk- / tp-）",
                            value = state.apiKey,
                            onValueChange = onApiKeyChange,
                            isVisible = state.isSecretVisible,
                            onToggleVisible = onToggleSecretVisible,
                            testTag = "editor_api_key",
                        )
                        Text(
                            text = "API 按量使用 sk- 开头的普通 API Key；Token Plan 使用 tp- 开头的专用 API Key。",
                            style = MyTokenType.caption,
                            color = colors.muted,
                        )
                    } else {
                        when (state.credentialKind) {
                            CredentialKind.BearerApiKey -> SecretField(
                                label = "API Key（Bearer Token）",
                                value = state.apiKey,
                                onValueChange = onApiKeyChange,
                                isVisible = state.isSecretVisible,
                                onToggleVisible = onToggleSecretVisible,
                                testTag = "editor_api_key",
                            )
                            CredentialKind.ApiKey -> SecretField(
                                label = "API Key",
                                value = state.apiKey,
                                onValueChange = onApiKeyChange,
                                isVisible = state.isSecretVisible,
                                onToggleVisible = onToggleSecretVisible,
                                testTag = "editor_api_key",
                            )
                            CredentialKind.AccessKeyPair -> {
                                MyTokenTextField(
                                    value = state.accessKeyID,
                                    onValueChange = onAccessKeyIDChange,
                                    label = "Access Key ID",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("editor_access_key_id"),
                                )
                                SecretField(
                                    label = "Secret Access Key",
                                    value = state.secretAccessKey,
                                    onValueChange = onSecretAccessKeyChange,
                                    isVisible = state.isSecretVisible,
                                    onToggleVisible = onToggleSecretVisible,
                                    testTag = "editor_secret_access_key",
                                )
                                MyTokenTextField(
                                    value = state.region,
                                    onValueChange = onRegionChange,
                                    label = "区域（默认 cn-beijing）",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("editor_region"),
                                )
                            }
                        }
                        if (state.providerId == ProviderId.NewAPI) {
                            MyTokenTextField(
                                value = state.baseURL,
                                onValueChange = onBaseURLChange,
                                label = "接口地址",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_base_url"),
                            )
                            MyTokenTextField(
                                value = state.userID,
                                onValueChange = onUserIDChange,
                                label = "用户 ID",
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("editor_user_id"),
                            )
                        }
                    }
                    state.validationError?.let { message ->
                        Text(
                            text = message,
                            style = MyTokenType.caption,
                            color = colors.criticalText,
                        )
                    }
                }

                MyTokenSection(title = "更多（可选）") {
                    MyTokenTextField(
                        value = state.websiteURL,
                        onValueChange = onWebsiteURLChange,
                        label = "官网地址（可选）",
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("editor_website_url"),
                    )
                }

                MyTokenSection(title = "安全说明") {
                    Text(
                        text = "凭证加密保存在本机 Keystore，不会上传",
                        style = MyTokenType.caption,
                        color = colors.muted,
                    )
                }

                state.testResultMessage?.let { message ->
                    TestResultLine(
                        message = message,
                        succeeded = state.testSucceeded == true,
                    )
                }
                if (state.testSucceeded == false) {
                    Text(
                        text = "测试失败仍可保存，失败原因不会丢失已填内容",
                        style = MyTokenType.caption,
                        color = colors.muted,
                    )
                }
            }
        }
    }

    if (providerSheetOpen) {
        ProviderPickerSheet(
            selected = state.providerId,
            onSelect = onProviderChange,
            onDismiss = { providerSheetOpen = false },
        )
    }
}

@Composable
private fun EditorTopBar(title: String, onBack: () -> Unit) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.canvas)
            .statusBarsPadding()
            .height(MyTokenSize.header),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(MyTokenSize.touch)
                .clickable(onClick = onBack)
                .semantics { contentDescription = "返回" },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = null,
                tint = colors.ink,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = title,
            style = MyTokenType.section,
            color = colors.ink,
        )
    }
}

@Composable
private fun ProviderRow(
    providerId: ProviderId,
    kind: CredentialKind,
    onClick: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    MyTokenSurface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .testTag("editor_provider_row"),
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card,
        shape = RoundedCornerShape(MyTokenRadius.row),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            ProviderLogoPlate(
                painter = painterResource(providerLogoRes(providerId)),
                contentDescription = ProviderNames.displayName(providerId),
                size = 40.dp,
                shape = RoundedCornerShape(10.dp),
                contentPadding = 4.dp,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = ProviderNames.displayName(providerId),
                    style = MyTokenType.body,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = kindLabel(kind),
                    style = MyTokenType.caption,
                    color = colors.muted,
                    maxLines = 1,
                )
            }
            Text(
                text = "更换",
                style = MyTokenType.label,
                color = colors.actionText,
            )
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = colors.muted,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProviderPickerSheet(
    selected: ProviderId,
    onSelect: (ProviderId) -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    // 先播放收起动画再通知选择，避免 dialog 窗口被直接移除而残留。
    val dismissThen: (() -> Unit) -> Unit = { action ->
        scope.launch {
            sheetState.hide()
        }.invokeOnCompletion {
            action()
        }
    }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        shape = RoundedCornerShape(topStart = MyTokenRadius.sheetTop, topEnd = MyTokenRadius.sheetTop),
        dragHandle = {
            Box(modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)) {
                Box(
                    modifier = Modifier
                        .size(width = 32.dp, height = 4.dp)
                        .clip(RoundedCornerShape(MyTokenRadius.pill))
                        .background(colors.line),
                )
            }
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp)
                .testTag("editor_provider_sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "选择供应商",
                    style = MyTokenType.section,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = "取消",
                    style = MyTokenType.label,
                    color = colors.muted,
                    modifier = Modifier
                        .clip(RoundedCornerShape(MyTokenRadius.pill))
                        .clickable { dismissThen(onDismiss) }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            }
            ProviderId.entries.chunked(2).forEach { rowProviders ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowProviders.forEach { provider ->
                        ProviderTile(
                            provider = provider,
                            isSelected = provider == selected,
                            // 选择立即生效，面板随后播放收起动画并关闭。
                            onClick = {
                                onSelect(provider)
                                dismissThen(onDismiss)
                            },
                            modifier = Modifier.weight(1f),
                        )
                    }
                    if (rowProviders.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun ProviderTile(
    provider: ProviderId,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(MyTokenRadius.inner)
    Row(
        modifier = modifier
            .heightIn(min = 64.dp)
            .clip(shape)
            .background(colors.surface)
            .border(if (isSelected) 1.dp else 1.dp, if (isSelected) colors.actionFill else colors.line, shape)
            .clickable(onClick = onClick)
            .testTag("editor_provider_tile_${provider.rawValue}")
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ProviderLogoPlate(
            painter = painterResource(providerLogoRes(provider)),
            contentDescription = null,
            size = 26.dp,
            shape = RoundedCornerShape(7.dp),
            contentPadding = 2.dp,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 8.dp),
        ) {
            Text(
                text = ProviderNames.displayName(provider),
                style = MyTokenType.label,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = kindLabel(defaultKind(provider)),
                style = MyTokenType.caption,
                color = colors.muted,
                maxLines = 1,
            )
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = null,
                tint = colors.actionText,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@Composable
private fun TestResultLine(message: String, succeeded: Boolean) {
    val colors = MyTokenThemeTokens.colors
    val tone = if (succeeded) colors.positiveText else colors.criticalText
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("editor_test_result"),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(tone, RoundedCornerShape(MyTokenRadius.pill)),
        )
        Text(
            text = message,
            style = MyTokenType.caption,
            color = tone,
        )
    }
}

@Composable
private fun SecretField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    isVisible: Boolean,
    onToggleVisible: () -> Unit,
    testTag: String,
) {
    MyTokenTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        visualTransformation = if (isVisible) {
            VisualTransformation.None
        } else {
            PasswordVisualTransformation()
        },
        trailingIcon = {
            IconButton(onClick = onToggleVisible) {
                Icon(
                    imageVector = if (isVisible) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                    contentDescription = if (isVisible) "隐藏密钥" else "显示密钥",
                )
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag),
    )
}

