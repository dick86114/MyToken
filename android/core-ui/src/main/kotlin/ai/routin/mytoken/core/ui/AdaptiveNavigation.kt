package ai.routin.mytoken.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** 玻璃侧栏：中等宽度使用图标栏，展开宽度使用带标签的宽侧栏。 */
@Composable
fun LiquidGlassNavigationRail(
    items: List<GlassNavItem>,
    expanded: Boolean,
    modifier: Modifier = Modifier,
) {
    val railWidth = if (expanded) 160.dp else 72.dp
    Box(
        modifier = modifier
            .fillMaxHeight()
            .windowInsetsPadding(
                WindowInsets.safeDrawing.only(
                    WindowInsetsSides.Start + WindowInsetsSides.Vertical,
                ),
            )
            .padding(8.dp)
            .testTag(if (expanded) "navigation_rail_expanded" else "navigation_rail"),
    ) {
        LiquidGlassSurface(
            modifier = Modifier
                .width(railWidth)
                .fillMaxHeight(),
            surfaceRole = MyTokenVisualPolicy.SurfaceRole.Window,
            shape = RoundedCornerShape(MyTokenVisualPolicy.outerRadiusDp),
            contentAlignment = Alignment.TopCenter,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                items.forEach { item ->
                    val contentColor = if (item.selected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                    val interactionSource = remember { MutableInteractionSource() }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(MyTokenVisualPolicy.buttonRadiusDp))
                            .clickable(
                                interactionSource = interactionSource,
                                indication = null,
                                onClick = item.onClick,
                            )
                            .padding(horizontal = 10.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = if (expanded) {
                            Arrangement.spacedBy(12.dp)
                        } else {
                            Arrangement.Center
                        },
                    ) {
                        Icon(
                            imageVector = item.icon,
                            contentDescription = item.contentDescription,
                            tint = contentColor,
                            modifier = Modifier.size(24.dp),
                        )
                        if (expanded) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = contentColor,
                            )
                        }
                    }
                }
            }
        }
    }
}

/** 根导航壳：一级页面按窗口宽度切换底栏或侧栏，非一级页面隐藏主导航。 */
@Composable
fun MyTokenNavigationScaffold(
    layoutMode: MyTokenLayoutMode,
    showNavigation: Boolean,
    items: List<GlassNavItem>,
    content: @Composable (PaddingValues) -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showNavigation && layoutMode == MyTokenLayoutMode.Compact) {
                LiquidGlassBottomBar(items = items)
            }
        },
    ) { padding ->
        if (showNavigation && layoutMode != MyTokenLayoutMode.Compact) {
            Row(modifier = Modifier.fillMaxSize()) {
                LiquidGlassNavigationRail(
                    items = items,
                    expanded = layoutMode == MyTokenLayoutMode.Expanded,
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                ) {
                    content(padding)
                }
            }
        } else {
            content(padding)
        }
    }
}
