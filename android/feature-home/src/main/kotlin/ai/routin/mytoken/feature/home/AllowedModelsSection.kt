package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenType
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Layers
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

@Composable
internal fun AllowedModelsSection(models: List<String>, modifier: Modifier = Modifier) {
    if (models.isEmpty()) return
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

    val copyModel: (String) -> Unit = { modelID ->
        clipboard.setText(AnnotatedString(modelID))
        copiedModelID = modelID
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = "允许模型",
                style = MyTokenType.caption,
                color = colors.muted,
            )
            Text(
                text = if (models.size == 1 || expanded) {
                    "${models.size} 个 · 点击复制"
                } else {
                    "${models.size} 个"
                },
                style = MyTokenType.caption,
                color = colors.muted,
            )
        }

        when {
            models.size == 1 -> ModelGridItem(
                modelID = models.single(),
                isCopied = copiedModelID == models.single(),
                onCopy = { copyModel(models.single()) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            !expanded -> ModelDeck(
                models = models,
                onExpand = { expanded = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            )

            else -> {
                ModelGrid(
                    models = models,
                    copiedModelID = copiedModelID,
                    onCopy = copyModel,
                    modifier = Modifier.padding(top = 8.dp),
                )
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

/** 折叠态：三张叠放的卡片，露出第一个模型和总数。 */
@Composable
private fun ModelDeck(
    models: List<String>,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(14.dp)

    Box(modifier = modifier.height(82.dp)) {
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp)
                .fillMaxWidth(0.88f)
                .height(60.dp)
                .clip(shape)
                .background(colors.subtle)
                .border(1.dp, colors.line, shape),
        )
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 10.dp)
                .fillMaxWidth(0.94f)
                .height(60.dp)
                .clip(shape)
                .background(colors.subtle)
                .border(1.dp, colors.line, shape),
        )
        Surface(
            onClick = onExpand,
            shape = shape,
            // 分节卡（卡片层）之上的内部小卡，属于浮层。
            color = colors.overlay,
            border = BorderStroke(1.dp, colors.line),
            modifier = Modifier
                .fillMaxWidth()
                .height(60.dp)
                .testTag("detail_models_more")
                .semantics { contentDescription = "展开 ${models.size} 个允许模型" },
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.subtle)
                        .border(1.dp, colors.line, RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Layers,
                        contentDescription = null,
                        tint = colors.muted,
                        modifier = Modifier.size(16.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = models.first(),
                        style = MyTokenType.caption.copy(fontFamily = FontFamily.Monospace),
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "另有 ${models.size - 1} 个模型 · 点击展开",
                        style = MyTokenType.caption,
                        color = colors.muted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.Filled.ExpandMore,
                    contentDescription = null,
                    tint = colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun ModelGrid(
    models: List<String>,
    copiedModelID: String?,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        models.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                pair.forEach { modelID ->
                    ModelGridItem(
                        modelID = modelID,
                        isCopied = copiedModelID == modelID,
                        onCopy = { onCopy(modelID) },
                        modifier = Modifier.weight(1f),
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ModelGridItem(
    modelID: String,
    isCopied: Boolean,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val contentColor = if (isCopied) colors.actionText else colors.ink
    val description = if (isCopied) "已复制模型 ID $modelID" else "复制模型 ID $modelID"

    Surface(
        onClick = onCopy,
        shape = RoundedCornerShape(10.dp),
        color = colors.subtle,
        border = BorderStroke(1.dp, if (isCopied) colors.actionFill else colors.line),
        modifier = modifier
            .height(40.dp)
            .semantics { contentDescription = description },
    ) {
        Row(
            modifier = Modifier.fillMaxSize().padding(horizontal = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = modelID,
                modifier = Modifier.weight(1f),
                style = MyTokenType.caption.copy(fontFamily = FontFamily.Monospace),
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = if (isCopied) Icons.Filled.Check else Icons.Outlined.ContentCopy,
                contentDescription = null,
                tint = if (isCopied) colors.actionText else colors.muted,
                modifier = Modifier.size(13.dp),
            )
        }
    }
}
