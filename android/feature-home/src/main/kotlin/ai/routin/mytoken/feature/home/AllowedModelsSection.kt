package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenRadius
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
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
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()

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

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val availableWidth = constraints.maxWidth.toFloat() - with(density) { 1.dp.toPx() }
        val chipPadding = with(density) { 20.dp.toPx() }
        val chipSpacing = with(density) { 6.dp.toPx() }
        val chipWidths = remember(models, availableWidth, density) {
            models.map { modelID ->
                val textWidth = textMeasurer.measure(
                    text = AnnotatedString(modelID),
                    style = MyTokenType.caption.copy(fontFamily = FontFamily.Monospace),
                ).size.width.toFloat()
                (textWidth + chipPadding).coerceAtMost(availableWidth)
            }
        }
        val fitCount = remember(chipWidths, availableWidth, chipSpacing) {
            fitModelsInLines(chipWidths, availableWidth, chipSpacing, maxLines = 2)
        }

        Column(modifier = Modifier.fillMaxWidth()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                maxLines = if (expanded) Int.MAX_VALUE else 2,
            ) {
                val visibleModels = if (expanded) models else models.take(fitCount)
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
            }
            if (!expanded && fitCount < models.size) {
                ModelViewMoreButton(
                    modifier = Modifier.padding(top = 8.dp),
                    onClick = { expanded = true },
                )
            } else if (expanded && models.size > fitCount) {
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
    }
}

/** 按实际 chip 宽度贪心排版，返回能放进 [maxLines] 行的模型数量。 */
private fun fitModelsInLines(
    widths: List<Float>,
    availableWidth: Float,
    spacing: Float,
    maxLines: Int,
): Int {
    if (availableWidth <= 0f || maxLines <= 0) return 0
    var line = 1
    var used = 0f
    var count = 0
    for (width in widths) {
        val needed = if (used == 0f) width else used + spacing + width
        if (needed <= availableWidth) {
            used = needed
            count++
        } else {
            if (line >= maxLines) break
            line++
            used = width
            count++
        }
    }
    return count
}

@Composable
private fun ModelViewMoreButton(modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colors = MyTokenThemeTokens.colors
    Text(
        text = "查看更多",
        style = MyTokenType.caption,
        color = colors.actionText,
        modifier = modifier
            .testTag("detail_models_more")
            .clip(RoundedCornerShape(6.dp))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 4.dp),
    )
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
