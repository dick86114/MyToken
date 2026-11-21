package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun AllowedModelsSection(models: List<String>, modifier: Modifier = Modifier) {
    var copiedModelID by remember { mutableStateOf<String?>(null) }
    var expanded by remember(models) { mutableStateOf(false) }
    val clipboard = LocalClipboardManager.current
    val colors = MyTokenThemeTokens.colors

    LaunchedEffect(copiedModelID) {
        if (copiedModelID != null) {
            delay(1_200)
            copiedModelID = null
        }
    }

    Row(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = "允许模型",
            style = MyTokenType.caption,
            color = colors.muted,
        )
        if (models.isNotEmpty()) {
            Text(
                text = "${models.size} 个 · 点击复制",
                style = MyTokenType.caption,
                color = colors.muted,
            )
        }
    }

    if (models.isEmpty()) {
        Text(
            text = "接口未返回",
            style = MyTokenType.caption,
            color = colors.ink,
            modifier = modifier.padding(top = 8.dp),
        )
        return
    }

    FlowRow(
        modifier = modifier.fillMaxWidth().padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val visibleModels = if (expanded) models else models.take(1)
        visibleModels.forEach { modelID ->
            ModelIDChip(
                modelID = modelID,
                isCopied = copiedModelID == modelID,
                onCopy = { copied ->
                    clipboard.setText(AnnotatedString(copied))
                    copiedModelID = copied
                },
            )
        }
        if (!expanded && models.size > 1) {
            ModelOverflowChip(
                label = "更多 ${models.size - 1}",
                testTag = "detail_models_more",
                onClick = { expanded = true },
            )
        }
    }
    if (expanded && models.size > 1) {
        Text(
            text = "收起",
            style = MyTokenType.caption,
            color = colors.muted,
            modifier = Modifier
                .testTag("detail_models_collapse")
                .clip(RoundedCornerShape(6.dp))
                .clickable(role = Role.Button) { expanded = false }
                .padding(horizontal = 6.dp, vertical = 4.dp),
        )
    }
}

@Composable
private fun ModelOverflowChip(label: String, testTag: String, onClick: () -> Unit) {
    val colors = MyTokenThemeTokens.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = colors.subtle,
        border = BorderStroke(1.dp, colors.line),
        modifier = Modifier.testTag(testTag),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MyTokenType.caption,
            color = colors.actionText,
        )
    }
}

/** 模型 chip：subtle 容器 + hairline，选中复制后 actionText + 勾。 */
@Composable
private fun ModelIDChip(
    modelID: String,
    isCopied: Boolean,
    onCopy: (String) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val contentColor = if (isCopied) colors.actionText else colors.ink
    val description = if (isCopied) "已复制模型 ID $modelID" else "复制模型 ID $modelID"

    Surface(
        onClick = { onCopy(modelID) },
        shape = RoundedCornerShape(8.dp),
        color = colors.subtle,
        border = BorderStroke(1.dp, if (isCopied) colors.actionFill else colors.line),
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = modelID,
                style = MyTokenType.caption.copy(fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (isCopied) {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = contentColor,
                )
            }
        }
    }
}
