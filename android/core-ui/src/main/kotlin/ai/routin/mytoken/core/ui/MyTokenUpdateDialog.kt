package ai.routin.mytoken.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 发现新版本时共用的弹窗：展示 Release notes 并直接确认下载/安装。 */
@Composable
fun MyTokenUpdateDialog(
    version: String?,
    releaseNotes: String?,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    confirmTag: String = "update_confirm",
) {
    val colors = MyTokenThemeTokens.colors
    val notes = releaseNotes?.trim().orEmpty()
    MyTokenDialog(
        title = version?.takeIf { it.isNotBlank() }?.let { "发现新版本 $it" } ?: "发现新版本",
        subtitle = "确认后将开始下载并安装",
        icon = Icons.Outlined.Star,
        tone = MyTokenDialogTone.Info,
        onDismissRequest = onDismiss,
        modifier = modifier,
        content = {
            MyTokenDialogSection(title = "更新日志") {
                if (notes.isBlank()) {
                    Text(
                        text = "此版本没有提供更新说明。",
                        style = MyTokenType.caption,
                        color = colors.muted,
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState())
                            .testTag("update_release_notes"),
                        verticalArrangement = Arrangement.spacedBy(3.dp),
                    ) {
                        UpdateReleaseNotes(notes)
                    }
                }
            }
        },
        actions = {
            MyTokenButton(
                text = "稍后",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                tone = MyTokenButtonTone.Neutral,
            )
            MyTokenButton(
                text = confirmLabel,
                onClick = onConfirm,
                modifier = Modifier
                    .weight(1f)
                    .testTag(confirmTag),
                tone = MyTokenButtonTone.Primary,
            )
        },
    )
}

/** Release notes 的轻量 Markdown 子集：标题加粗、列表转圆点，其他按行展示。 */
@Composable
private fun UpdateReleaseNotes(markdown: String) {
    val colors = MyTokenThemeTokens.colors
    markdown.replace("\r\n", "\n").split('\n').forEach { rawLine ->
        val heading = rawLine.replaceFirst(Regex("^#{1,6}\\s+"), "")
        val isHeading = heading != rawLine
        val line = if (isHeading) {
            heading
        } else {
            rawLine.replaceFirst(Regex("^\\s*[-*+]\\s+"), "• ")
        }
        if (line.isNotBlank()) {
            Text(
                text = line.replace("**", "").replace("`", ""),
                style = MyTokenType.caption,
                fontWeight = if (isHeading) FontWeight.Bold else FontWeight.Normal,
                color = if (isHeading) colors.ink else colors.muted,
                lineHeight = 18.sp,
            )
        }
    }
}
