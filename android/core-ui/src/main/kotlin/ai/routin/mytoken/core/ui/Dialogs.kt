package ai.routin.mytoken.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** 弹窗语义色只用于标题图标、状态点和破坏性操作，不额外染色正文。 */
enum class MyTokenDialogTone {
    Info,
    Success,
    Warning,
    Danger,
}

/** 弹窗图标的文字色：把 fill 留给圆点，避免小图标对比度不足。 */
@Composable
internal fun MyTokenDialogTone.markerColor(): Color {
    val colors = MyTokenThemeTokens.colors
    return when (this) {
        MyTokenDialogTone.Info -> colors.ink
        MyTokenDialogTone.Success -> colors.positiveText
        MyTokenDialogTone.Warning -> colors.warningText
        MyTokenDialogTone.Danger -> colors.criticalText
    }
}

/** 弹窗圆点的填充色。 */
@Composable
internal fun MyTokenDialogTone.fillColor(): Color {
    val colors = MyTokenThemeTokens.colors
    return when (this) {
        MyTokenDialogTone.Info -> colors.statusDotNormal
        MyTokenDialogTone.Success -> colors.positiveFill
        MyTokenDialogTone.Warning -> colors.warningFill
        MyTokenDialogTone.Danger -> colors.criticalFill
    }
}

/**
 * 全局弹窗壳。
 *
 * 结构固定为“图标 + 标题 + 可选副标题 / 分区内容 / 底部操作”，让说明、错误与确认弹窗
 * 共享同一套空间层级；正文不铺满整张弹窗，也不使用装饰性分割线。
 */
@Composable
fun MyTokenDialog(
    title: String,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Outlined.Info,
    tone: MyTokenDialogTone = MyTokenDialogTone.Info,
    content: @Composable ColumnScope.() -> Unit,
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val colors = MyTokenThemeTokens.colors
    val accent = tone.markerColor()
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            MyTokenSurface(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 420.dp)
                    .testTag("my_token_dialog"),
                surfaceRole = MyTokenVisualPolicy.SurfaceRole.Modal,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(MyTokenRadius.inner))
                                .background(colors.subtle)
                                .border(1.dp, colors.line, RoundedCornerShape(MyTokenRadius.inner)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(21.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = title,
                                style = MyTokenType.detailTitle,
                                color = colors.ink,
                            )
                            if (subtitle != null) {
                                Text(
                                    text = subtitle,
                                    modifier = Modifier.padding(top = 3.dp),
                                    style = MyTokenType.label,
                                    color = colors.muted,
                                )
                            }
                        }
                    }

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        content = content,
                    )

                    if (actions != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions,
                        )
                    }
                }
            }
        }
    }
}

/** 弹窗中的信息分组：subtle 底 + hairline 承载关系，组与组之间靠留白区分。 */
@Composable
fun MyTokenDialogSection(
    title: String? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.subtle)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 13.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        if (title != null) {
            Text(
                text = title,
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = colors.muted,
            )
        }
        content()
    }
}

/** 一行说明：标签在前，补充信息在后；色点用于颜色图例，其他场景保持空白。 */
@Composable
fun MyTokenDialogRow(
    label: String,
    detail: String? = null,
    markerColor: Color? = null,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        if (markerColor != null) {
            Box(
                modifier = Modifier
                    .padding(top = 5.dp)
                    .size(7.dp)
                    .clip(CircleShape)
                    .background(markerColor),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MyTokenType.body,
                fontWeight = FontWeight.SemiBold,
                color = colors.ink,
            )
            if (detail != null) {
                Text(
                    text = detail,
                    modifier = Modifier.padding(top = 1.dp),
                    style = MyTokenType.label,
                    color = colors.muted,
                )
            }
        }
    }
}

/** 短状态说明，用于错误原因、导出位置等需要背景承托的内容。 */
@Composable
fun MyTokenDialogStatus(
    message: String,
    modifier: Modifier = Modifier,
    tone: MyTokenDialogTone = MyTokenDialogTone.Info,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.subtle)
            .border(1.dp, colors.line, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(7.dp)
                .clip(CircleShape)
                .background(tone.fillColor()),
        )
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = MyTokenType.label,
            color = colors.ink,
        )
    }
}
