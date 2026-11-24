package ai.routin.mytoken.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

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
    downloading: Boolean = false,
    downloadProgress: Float? = null,
    needsInstallPermission: Boolean = false,
) {
    val colors = MyTokenThemeTokens.colors
    val notes = releaseNotes?.trim().orEmpty()
    MyTokenDialog(
        title = version?.takeIf { it.isNotBlank() }?.let { "发现新版本 $it" } ?: "发现新版本",
        subtitle = when {
            downloading -> "正在下载新版本，完成后会自动打开安装器"
            needsInstallPermission -> "安装包已下载，请先允许安装未知来源应用"
            else -> "确认后将开始下载并安装"
        },
        icon = Icons.Outlined.Star,
        tone = MyTokenDialogTone.Info,
        onDismissRequest = { if (!downloading) onDismiss() },
        modifier = modifier,
        content = {
            if (downloading) {
                MyTokenDialogSection(title = "下载进度") {
                    UpdateDownloadProgress(progress = downloadProgress)
                }
            } else {
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
            }
        },
        actions = {
            if (downloading) {
                MyTokenButton(
                    text = "正在下载…",
                    onClick = {},
                    modifier = Modifier.weight(1f),
                    tone = MyTokenButtonTone.Neutral,
                    enabled = false,
                )
            } else {
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
            }
        },
    )
}

@Composable
private fun UpdateDownloadProgress(progress: Float?) {
    val colors = MyTokenThemeTokens.colors
    val clamped = (progress ?: 0f).coerceIn(0f, 1f)
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "正在下载更新",
                modifier = Modifier.weight(1f),
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            Text(
                text = if (progress == null) "连接中…" else "${(clamped * 100).roundToInt()}%",
                modifier = Modifier.testTag("update_download_percent"),
                style = MyTokenType.caption,
                color = colors.muted,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(colors.line)
                .testTag("update_download_progress"),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(clamped)
                    .fillMaxHeight()
                    .background(colors.actionFill),
            )
        }
    }
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
