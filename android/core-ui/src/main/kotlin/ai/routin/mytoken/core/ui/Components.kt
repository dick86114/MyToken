package ai.routin.mytoken.core.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * MyToken 基础控件。
 *
 * 材质规则（设计文档 §6.2）：实色表面 + 1px hairline；只有浮层允许阴影。
 * 颜色只来自 [MyTokenThemeTokens]，业务代码不再拼 alpha 或自带色值。
 */

/** 按钮的语义色调。 */
enum class MyTokenButtonTone { Primary, Neutral, Destructive }

private data class ButtonToneColors(
    val base: Color,
    val content: Color,
    val container: Color,
)

@Composable
private fun buttonToneColors(tone: MyTokenButtonTone): ButtonToneColors {
    val colors = MyTokenThemeTokens.colors
    return when (tone) {
        // 黄色主按钮统一使用深墨字，避免白字在票面黄上失去对比。
        MyTokenButtonTone.Primary -> ButtonToneColors(colors.actionFill, colors.reportTicketInk, colors.actionFill)
        MyTokenButtonTone.Neutral -> ButtonToneColors(colors.ink, colors.ink, colors.subtle)
        MyTokenButtonTone.Destructive -> ButtonToneColors(colors.criticalFill, colors.onAction, colors.criticalFill)
    }
}

/** 实色按钮：12dp 圆角、48dp 触控高度；Primary 只给当前区块的主操作。 */
@Composable
fun MyTokenButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: MyTokenButtonTone = MyTokenButtonTone.Neutral,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val toneColors = buttonToneColors(tone)
    val shape = RoundedCornerShape(MyTokenRadius.inner)
    val contentAlpha = if (enabled) 1f else 0.45f
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .defaultMinSize(minHeight = MyTokenSize.touch)
            .myTokenPressScale(interactionSource, pressedScale = 0.97f)
            .clip(shape)
            .background(toneColors.container)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    tint = toneColors.content.copy(alpha = contentAlpha),
                    modifier = Modifier.size(18.dp),
                )
            }
            Text(
                text = text,
                style = MyTokenType.label,
                fontWeight = FontWeight.SemiBold,
                color = toneColors.content.copy(alpha = contentAlpha),
                maxLines = 1,
            )
        }
    }
}

/**
 * 开关：46×28 视觉轨道，外层 48×48 触控盒。
 * 无障碍语义与 Material Switch 一致（Role.Switch + contentDescription）。
 */
@Composable
fun MyTokenSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    enabled: Boolean = true,
) {
    val colors = MyTokenThemeTokens.colors
    val darkTheme = LocalMyTokenDarkTheme.current
    // 开关统一使用报告票面黄；浅色主题用白色滑块，深色主题保持黑滑块。
    val thumbColor = if (darkTheme) colors.reportTicketInk else colors.surface
    val trackColor by animateColorAsState(
        targetValue = if (checked) colors.reportTicket else colors.track,
        animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
        label = "myTokenSwitchTrack",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 3.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
        label = "myTokenSwitchThumbOffset",
    )
    Box(
        modifier = modifier
            .size(MyTokenSize.touch)
            .alpha(if (enabled) 1f else 0.45f)
            .toggleable(
                value = checked,
                role = Role.Switch,
                enabled = enabled,
                onValueChange = { onCheckedChange?.invoke(it) },
            )
            .semantics { contentDescription?.let { this.contentDescription = it } },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(MyTokenSize.switchWidth)
                .height(MyTokenSize.switchHeight)
                .clip(RoundedCornerShape(MyTokenRadius.pill))
                .background(trackColor),
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.5.dp)
                    .padding(start = thumbOffset)
                    .requiredSize(23.dp)
                    .clip(RoundedCornerShape(MyTokenRadius.pill))
                    .background(thumbColor),
            )
        }
    }
}

/**
 * 单值滑块：主题黄色轨道/滑块，触摸高度沿用 [MyTokenSize.touch]。
 * 容器补充用途与当前值语义，滑块自身的 ProgressBarRangeInfo 仍由 Material 提供。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTokenSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueDescription: String? = null,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = MyTokenThemeTokens.colors
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.actionFill,
        activeTrackColor = colors.actionFill,
        activeTickColor = colors.actionFill,
        inactiveTrackColor = colors.line,
        inactiveTickColor = colors.line,
        disabledThumbColor = colors.actionFill,
        disabledActiveTrackColor = colors.actionFill,
        disabledInactiveTrackColor = colors.line,
        disabledActiveTickColor = colors.actionFill,
        disabledInactiveTickColor = colors.line,
    )
    val interactionSource = remember { MutableInteractionSource() }
    Slider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = MyTokenSize.touch)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                this.contentDescription = contentDescription
                valueDescription?.let { this.stateDescription = it }
                if (!enabled) disabled()
            },
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors,
        interactionSource = interactionSource,
        thumb = {
            SliderDefaults.Thumb(
                interactionSource = interactionSource,
                modifier = Modifier.semantics {
                    this.contentDescription = "$contentDescription 当前值"
                },
                colors = sliderColors,
                enabled = enabled,
            )
        },
    )
}

/**
 * 双滑块：用于低/高阈值等区间配置。低值始终小于等于高值由调用方归一化。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTokenRangeSlider(
    value: ClosedFloatingPointRange<Float>,
    onValueChange: (ClosedFloatingPointRange<Float>) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    contentDescription: String,
    modifier: Modifier = Modifier,
    steps: Int = 0,
    valueDescription: String? = null,
    enabled: Boolean = true,
    onValueChangeFinished: (() -> Unit)? = null,
) {
    val colors = MyTokenThemeTokens.colors
    val sliderColors = SliderDefaults.colors(
        thumbColor = colors.actionFill,
        activeTrackColor = colors.actionFill,
        activeTickColor = colors.actionFill,
        inactiveTrackColor = colors.line,
        inactiveTickColor = colors.line,
        disabledThumbColor = colors.actionFill,
        disabledActiveTrackColor = colors.actionFill,
        disabledInactiveTrackColor = colors.line,
        disabledActiveTickColor = colors.actionFill,
        disabledInactiveTickColor = colors.line,
    )
    val startInteractionSource = remember { MutableInteractionSource() }
    val endInteractionSource = remember { MutableInteractionSource() }
    RangeSlider(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .heightIn(min = MyTokenSize.touch)
            .alpha(if (enabled) 1f else 0.45f)
            .semantics {
                this.contentDescription = contentDescription
                valueDescription?.let { this.stateDescription = it }
                if (!enabled) disabled()
            },
        enabled = enabled,
        valueRange = valueRange,
        steps = steps,
        onValueChangeFinished = onValueChangeFinished,
        colors = sliderColors,
        startInteractionSource = startInteractionSource,
        endInteractionSource = endInteractionSource,
        startThumb = {
            SliderDefaults.Thumb(
                interactionSource = startInteractionSource,
                modifier = Modifier.semantics {
                    this.contentDescription = "$contentDescription 低值"
                },
                colors = sliderColors,
                enabled = enabled,
            )
        },
        endThumb = {
            SliderDefaults.Thumb(
                interactionSource = endInteractionSource,
                modifier = Modifier.semantics {
                    this.contentDescription = "$contentDescription 高值"
                },
                colors = sliderColors,
                enabled = enabled,
            )
        },
    )
}

/** 筛选 chip：未选中 subtle，选中 ink；不使用强调色底。 */
@Composable
fun myTokenFilterChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = MyTokenThemeTokens.colors.subtle,
    labelColor = MyTokenThemeTokens.colors.muted,
    selectedContainerColor = MyTokenThemeTokens.colors.ink,
    selectedLabelColor = MyTokenThemeTokens.colors.surface,
)

@Composable
fun myTokenFilterChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = MyTokenThemeTokens.colors.line,
    selectedBorderColor = Color.Transparent,
    borderWidth = 1.dp,
    selectedBorderWidth = 0.dp,
)

/**
 * 输入框：标签在框上方，48dp 高度，subtle 底 + 1px 描边。
 * 保留 Material OutlinedTextField 的完整编辑行为（密钥遮罩、trailing 图标、testTag）。
 */
@Composable
fun MyTokenTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 6.dp,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
    enabled: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
) {
    val colors = MyTokenThemeTokens.colors
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MyTokenType.label,
            color = colors.muted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = singleLine,
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            shape = RoundedCornerShape(MyTokenRadius.inner),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = colors.subtle,
                unfocusedContainerColor = colors.subtle,
                focusedBorderColor = colors.actionFill,
                unfocusedBorderColor = colors.line,
                disabledBorderColor = colors.line,
                cursorColor = colors.actionFill,
                focusedLabelColor = colors.actionText,
                unfocusedLabelColor = colors.muted,
            ),
            // modifier（含 testTag）落在输入框本体上，语义（RequestFocus 等）才能被测试命中。
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = MyTokenSize.touch)
                .padding(horizontal = contentPadding),
        )
        if (supportingText != null) {
            Text(
                text = supportingText,
                modifier = Modifier.padding(top = 4.dp),
                style = MyTokenType.caption,
                color = colors.muted,
            )
        }
    }
}

/** 实色表面：卡片/输入框用 surface/subtle + hairline；Modal 才投影。 */
@Composable
fun MyTokenSurface(
    modifier: Modifier = Modifier,
    surfaceRole: MyTokenVisualPolicy.SurfaceRole = MyTokenVisualPolicy.SurfaceRole.Control,
    shape: RoundedCornerShape = RoundedCornerShape(MyTokenRadius.card),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    val background = when (surfaceRole) {
        MyTokenVisualPolicy.SurfaceRole.Window,
        MyTokenVisualPolicy.SurfaceRole.Card,
        MyTokenVisualPolicy.SurfaceRole.Modal,
        -> colors.surface

        MyTokenVisualPolicy.SurfaceRole.Control,
        MyTokenVisualPolicy.SurfaceRole.Metric,
        -> colors.subtle
    }
    val resolved = modifier
        .then(
            if (MyTokenVisualPolicy.allowsShadow(surfaceRole)) {
                Modifier.shadow(elevation = 24.dp, shape = shape)
            } else {
                Modifier
            },
        )
        .clip(shape)
        .background(background)
        .then(
            if (surfaceRole == MyTokenVisualPolicy.SurfaceRole.Window) {
                Modifier
            } else {
                Modifier.border(1.dp, colors.line, shape)
            },
        )
    Box(modifier = resolved, contentAlignment = contentAlignment) {
        content()
    }
}

/** 分节卡片：surface + hairline + 20dp 圆角，无阴影、无内部分割线。 */
@Composable
fun MyTokenSection(
    title: String,
    modifier: Modifier = Modifier,
    action: (@Composable () -> Unit)? = null,
    contentPadding: Dp = MyTokenSpacing.cardFull,
    contentSpacing: Dp = MyTokenSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 2.dp, end = 2.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MyTokenType.sectionLabel,
                color = colors.muted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            action?.invoke()
        }
        MyTokenSurface(
            modifier = Modifier.fillMaxWidth(),
            surfaceRole = MyTokenVisualPolicy.SurfaceRole.Card,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(contentPadding),
                verticalArrangement = Arrangement.spacedBy(contentSpacing),
            ) {
                content()
            }
        }
    }
}

/** 兼容壳：旧设置页仍按 SectionCard 组织内容。 */
@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    contentPadding: Dp = MyTokenSpacing.cardFull,
    contentSpacing: Dp = MyTokenSpacing.md,
    content: @Composable ColumnScope.() -> Unit,
) {
    MyTokenSection(
        title = title,
        modifier = modifier,
        contentPadding = contentPadding,
        contentSpacing = contentSpacing,
    ) {
        if (supportingText != null) {
            Text(
                text = supportingText,
                style = MyTokenType.caption,
                color = MyTokenThemeTokens.colors.muted,
            )
        }
        content()
    }
}

/** 通用行：图标 + 标题/说明 + 值 + 尾部插槽；可点击时最小 48dp。 */
@Composable
fun MyTokenRow(
    icon: ImageVector?,
    title: String,
    supportingText: String? = null,
    value: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    iconPlate: Boolean = false,
    iconContent: (@Composable () -> Unit)? = null,
    iconTint: Color = MyTokenThemeTokens.colors.muted,
    iconSize: Dp? = null,
) {
    val colors = MyTokenThemeTokens.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (onClick != null) MyTokenSize.touch else MyTokenSize.settingsRowMin)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MyTokenSpacing.md),
    ) {
        val iconSlot: (@Composable () -> Unit)? = when {
            icon != null -> {
                {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(
                            iconSize ?: if (iconPlate) 15.dp else 18.dp,
                        ),
                    )
                }
            }
            else -> iconContent
        }
        iconSlot?.let { slot ->
            if (iconPlate) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(colors.subtle, RoundedCornerShape(8.dp))
                        .border(1.dp, colors.line, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    slot()
                }
            } else {
                slot()
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MyTokenType.body,
                fontWeight = FontWeight(550),
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    modifier = Modifier.padding(top = 1.dp),
                    style = MyTokenType.caption,
                    color = colors.muted,
                    maxLines = 2,
                )
            }
        }
        if (value != null) {
            Text(
                text = value,
                style = MyTokenType.caption,
                fontWeight = FontWeight(550),
                color = colors.muted,
                maxLines = 1,
            )
        }
        trailing?.invoke()
    }
}

/** 兼容壳：旧设置页的 SettingRow。 */
@Composable
fun SettingRow(
    title: String,
    supportingText: String? = null,
    icon: ImageVector? = null,
    iconContent: (@Composable () -> Unit)? = null,
    trailing: @Composable () -> Unit,
    iconTint: Color = MyTokenThemeTokens.colors.muted,
    iconSize: Dp? = null,
) {
    MyTokenRow(
        icon = icon,
        title = title,
        supportingText = supportingText,
        trailing = trailing,
        iconContent = iconContent,
        iconTint = iconTint,
        iconSize = iconSize,
    )
}

/** 三段/多段选择器：subtle 轨道 + surface 选中块。 */
@Composable
fun MyTokenSegmented(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val shape = RoundedCornerShape(MyTokenRadius.inner)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp)
            .clip(shape)
            .background(colors.subtle)
            .border(1.dp, colors.line, shape)
            .padding(4.dp)
            .selectableGroup(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { index, label ->
            val selected = index == selectedIndex
            val itemShape = RoundedCornerShape(9.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(itemShape)
                    .then(
                        if (selected) {
                            Modifier
                                .background(colors.surface)
                                .border(1.dp, colors.line, itemShape)
                        } else {
                            Modifier
                        },
                    )
                    .selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onSelect(index) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = MyTokenType.label,
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (selected) colors.actionText else colors.muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** 状态 = 圆点 + 文字；正常状态保持中性。 */
enum class MyTokenStatusTone { Neutral, Positive, Warning, Critical }

@Composable
fun MyTokenStatus(
    text: String,
    tone: MyTokenStatusTone,
    modifier: Modifier = Modifier,
) {
    val colors = MyTokenThemeTokens.colors
    val dot = when (tone) {
        MyTokenStatusTone.Neutral -> colors.statusDotNormal
        MyTokenStatusTone.Positive -> colors.positiveFill
        MyTokenStatusTone.Warning -> colors.warningFill
        MyTokenStatusTone.Critical -> colors.criticalFill
    }
    val content = when (tone) {
        MyTokenStatusTone.Neutral -> colors.muted
        MyTokenStatusTone.Positive -> colors.positiveText
        MyTokenStatusTone.Warning -> colors.warningText
        MyTokenStatusTone.Critical -> colors.criticalText
    }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MyTokenSpacing.xs),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(dot),
        )
        Text(
            text = text,
            style = MyTokenType.caption,
            color = content,
            maxLines = 1,
        )
    }
}
