package ai.routin.mytoken.core.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 玻璃控件统一设计规则：
 * - 普通按钮和内层控件 10dp 圆角，卡片和窗口壳 14dp 圆角；
 * - 全局唯一强调色 = 主题 primary，破坏性操作 = 主题 error；
 * - 明暗两套主题都按底色亮度自动切换，文字对比度满足 WCAG AA。
 */

/** 按钮的语义色调。 */
enum class GlassButtonTone { Primary, Neutral, Destructive }

private data class GlassToneColors(
    val base: Color,
    val content: Color,
    val container: Color,
) {
    companion object {
        @Composable
        fun of(tone: GlassButtonTone): GlassToneColors {
            val scheme = MaterialTheme.colorScheme
            return when (tone) {
                GlassButtonTone.Primary -> GlassToneColors(scheme.primary, scheme.onPrimary, scheme.primary)
                GlassButtonTone.Neutral -> GlassToneColors(scheme.onSurface, scheme.onSurface, scheme.surfaceVariant)
                GlassButtonTone.Destructive -> GlassToneColors(scheme.error, scheme.error, scheme.error.copy(alpha = 0.12f))
            }
        }
    }
}

@Composable
private fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * 方案 A 按钮：实色底、12dp 圆角。
 * [GlassButtonTone.Primary] 只给当前区块的主操作，避免一屏多个强强调。
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: GlassButtonTone = GlassButtonTone.Neutral,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val toneColors = GlassToneColors.of(tone)
    val shape = RoundedCornerShape(MyTokenVisualPolicy.buttonRadiusDp)
    val contentAlpha = if (enabled) 1f else 0.45f
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .clip(shape)
            .background(toneColors.container)
            .clickable(
                interactionSource = interactionSource,
                indication = ripple(color = toneColors.base),
                enabled = enabled,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 11.dp),
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
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = toneColors.content.copy(alpha = contentAlpha),
            )
        }
    }
}

/**
 * 液态玻璃开关：胶囊轨道 + 滑动圆点，选中态使用主题强调色。
 * 无障碍语义与 Material Switch 一致（Role.Switch + contentDescription）。
 */
@Composable
fun GlassSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val dark = isDarkTheme()
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val trackColor by animateColorAsState(
        targetValue = if (checked) {
            primary
        } else {
            onSurface.copy(alpha = if (dark) 0.22f else 0.13f)
        },
        animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
        label = "glassSwitchTrack",
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) Color.White else Color.White.copy(alpha = if (dark) 0.88f else 0.98f),
        label = "glassSwitchThumb",
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 3.dp,
        animationSpec = spring(dampingRatio = 1f, stiffness = 700f),
        label = "glassSwitchThumbOffset",
    )
    val trackWidth = 46.dp
    val trackHeight = 28.dp
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(trackHeight)
            .clip(RoundedCornerShape(100))
            .background(trackColor)
            .toggleable(
                value = checked,
                role = Role.Switch,
                onValueChange = { onCheckedChange?.invoke(it) },
            )
            .semantics { contentDescription?.let { this.contentDescription = it } },
    ) {
        Box(
            modifier = Modifier
                .padding(top = 2.5.dp)
                .padding(start = thumbOffset)
                .requiredSize(23.dp)
                .clip(RoundedCornerShape(100))
                .background(thumbColor),
        )
    }
}

/** 与整体玻璃语言统一的 FilterChip 配色：选中态用强调色而不是默认的绿系 secondaryContainer。 */
@Composable
fun glassFilterChipColors(selected: Boolean) = FilterChipDefaults.filterChipColors(
    containerColor = Color.Transparent,
    labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isDarkTheme()) 0.26f else 0.14f),
    selectedLabelColor = MaterialTheme.colorScheme.primary,
)

@Composable
fun glassFilterChipBorder(selected: Boolean) = FilterChipDefaults.filterChipBorder(
    enabled = true,
    selected = selected,
    borderColor = Color.Transparent,
    selectedBorderColor = Color.Transparent,
    borderWidth = 0.dp,
    selectedBorderWidth = 0.dp,
)

/**
 * 液态玻璃输入框：玻璃容器 + 透明输入底；聚焦时显示强调色描边。
 * 保留 Material OutlinedTextField 的完整编辑行为（密钥遮罩、trailing 图标、testTag）。
 */
@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    contentPadding: Dp = 6.dp,
    singleLine: Boolean = true,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    trailingIcon: @Composable (() -> Unit)? = null,
    supportingText: String? = null,
) {
    LiquidGlassSurface(
        surfaceRole = MyTokenVisualPolicy.SurfaceRole.Control,
        shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            label = { Text(text = label) },
            singleLine = singleLine,
            visualTransformation = visualTransformation,
            trailingIcon = trailingIcon,
            supportingText = supportingText?.let { text ->
                {
                    Text(
                        text = text,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            },
            shape = RoundedCornerShape(MyTokenVisualPolicy.buttonRadiusDp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                unfocusedBorderColor = Color.Transparent,
                disabledBorderColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.primary,
                focusedLabelColor = MaterialTheme.colorScheme.primary,
                unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            // modifier（含 testTag）落在输入框本体上，语义（RequestFocus 等）才能被测试命中。
            modifier = modifier
                .fillMaxWidth()
                .padding(start = contentPadding, end = contentPadding),
        )
    }
}

/** Compose 材质实现：使用色调、圆角和层级表达表面，不依赖装饰性描边。 */
@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    surfaceRole: MyTokenVisualPolicy.SurfaceRole = MyTokenVisualPolicy.SurfaceRole.Control,
    shape: RoundedCornerShape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val material = MyTokenVisualPolicy.materialFor(surfaceRole)
    val solidBase = when (surfaceRole) {
        MyTokenVisualPolicy.SurfaceRole.Metric -> MaterialTheme.colorScheme.surfaceVariant
        MyTokenVisualPolicy.SurfaceRole.Card -> MaterialTheme.colorScheme.surfaceVariant
        else -> MaterialTheme.colorScheme.surface
    }
    val backgroundBrush = remember(isDark, material, solidBase) {
        if (material == MyTokenVisualPolicy.Material.WindowGlass) {
            Brush.verticalGradient(
                listOf(
                    (if (isDark) MyTokenPalette.darkCanvas else MyTokenPalette.lightCanvas)
                        .copy(alpha = if (isDark) 0.88f else 0.78f),
                    (if (isDark) MyTokenPalette.darkSurface else MyTokenPalette.lightSurface)
                        .copy(alpha = if (isDark) 0.72f else 0.64f),
                ),
            )
        } else {
            null
        }
    }
    Box(
        modifier = modifier
            .clip(shape)
            .then(
                if (backgroundBrush != null) {
                    Modifier.background(brush = backgroundBrush)
                } else {
                    Modifier.background(solidBase)
                },
            ),
        contentAlignment = contentAlignment,
    ) {
        content()
    }
}

@Composable
fun SectionCard(
    title: String,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column {
                Text(
                    text = title,
                    style = MyTokenType.sectionTitle,
                )
                if (supportingText != null) {
                    Text(
                        text = supportingText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            content()
        }
    }
}

@Composable
fun SettingRow(
    title: String,
    supportingText: String? = null,
    trailing: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
            )
            if (supportingText != null) {
                Text(
                    text = supportingText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        trailing()
    }
}
