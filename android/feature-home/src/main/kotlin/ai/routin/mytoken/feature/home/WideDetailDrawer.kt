package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.alerts.AlertThresholds
import ai.routin.mytoken.core.ui.MyTokenEdgeDrawerSide
import ai.routin.mytoken.core.ui.MyTokenLayoutMode
import ai.routin.mytoken.core.ui.MyTokenSurface
import ai.routin.mytoken.core.ui.MyTokenThemeTokens
import ai.routin.mytoken.core.ui.MyTokenVisualPolicy
import ai.routin.mytoken.core.ui.myTokenDrawerSwipeToDismiss
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import java.util.UUID
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * 大屏详情浮层：像设置页一样从右侧滑出，悬浮在首页之上。
 *
 * 宽度与设置抽屉一致（窗口 88%，上限 420dp）；带遮罩，点击浮层外关闭。
 * 首页不缩放、不位移、不缩窄；支持右滑、下拉顶部手柄、✕ 与系统返回关闭。
 * 所有位移/透明度只在 graphicsLayer 中读取，滑动时不会触发整页重组。
 *
 * 面板四角统一 28dp：Compose 的裁剪命中测试在部分环境对四角半径不一致的形状
 * 会漏掉子节点点击，浮层本身完整可点比贴边直角更重要。
 */
@Composable
internal fun WideDetailDrawer(
    selectedCard: CredentialCardUi?,
    layoutMode: MyTokenLayoutMode,
    globalAlertThresholds: AlertThresholds,
    alertThresholdOverrides: Map<UUID, AlertThresholds>,
    onClose: () -> Unit,
    onRefresh: (UUID) -> Unit,
    onEdit: (UUID) -> Unit,
    onDelete: (CredentialCardUi) -> Unit,
    modifier: Modifier = Modifier,
) {
    var lastCard by remember { mutableStateOf(selectedCard) }
    LaunchedEffect(selectedCard) {
        if (selectedCard != null) lastCard = selectedCard
    }

    val progress = remember { Animatable(if (selectedCard != null) 1f else 0f) }
    var dragging by remember { mutableStateOf(false) }
    var dragProgress by remember { mutableFloatStateOf(progress.value) }
    var releaseVelocity by remember { mutableFloatStateOf(0f) }
    val handleOffset = remember { Animatable(0f) }
    var handleDragging by remember { mutableStateOf(false) }
    var handleDragPx by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val handleClosePx = with(density) { 72.dp.toPx() }
    val maxHandleDragPx = with(density) { 240.dp.toPx() }

    LaunchedEffect(selectedCard, dragging) {
        if (dragging) return@LaunchedEffect
        val open = selectedCard != null
        if (open && lastCard == null) lastCard = selectedCard
        val velocity = releaseVelocity
        releaseVelocity = 0f
        progress.animateTo(
            targetValue = if (open) 1f else 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = 0.0005f,
            ),
            initialVelocity = velocity,
        )
        if (!open && !dragging) {
            lastCard = null
            dragProgress = 0f
        }
    }

    val shownCard = selectedCard ?: lastCard ?: return

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val drawerWidth = WideHomeSpec.drawerWidth(maxWidth)
        val panelWidthPx = with(density) { drawerWidth.toPx() }
        val isDark = MyTokenThemeTokens.colors.canvas.luminance() < 0.5f
        val scrimColor = MyTokenThemeTokens.colors.scrim
        val scrimAlpha = if (isDark) 0.46f else 0.24f

        Box(modifier = Modifier.fillMaxSize()) {
            // 遮罩与设置抽屉同一套色值；开合进度只在图层阶段读取。
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 遮罩固定浓度、不参与开合动画：否则每帧重画整屏会把下方
                    // 全部卡片一起重绘，详情开合会明显掉帧。面板自身仍有滑入/滑出。
                    .drawBehind {
                        drawRect(color = scrimColor, alpha = scrimAlpha)
                    }
                    .then(
                        if (selectedCard != null) {
                            Modifier
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onClose,
                                )
                                .semantics { contentDescription = "关闭详情" }
                        } else {
                            Modifier
                        },
                    )
                    .testTag("home_wide_detail_scrim"),
            )
            MyTokenSurface(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(drawerWidth)
                    .fillMaxHeight()
                .myTokenDrawerSwipeToDismiss(
                    enabled = selectedCard != null && !handleDragging,
                    side = MyTokenEdgeDrawerSide.End,
                    drawerWidthPx = panelWidthPx,
                    onStart = {
                        dragging = true
                        dragProgress = progress.value
                        releaseVelocity = 0f
                    },
                    onDelta = { delta ->
                        dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                    },
                    onCancel = {
                        val current = dragProgress
                        scope.launch {
                            progress.snapTo(current)
                            dragging = false
                            releaseVelocity = 0f
                        }
                    },
                    onEnd = { _, velocity ->
                        val projectedPx = (velocity / 1000f) * 0.998f / (1f - 0.998f)
                        val projectedProgress = dragProgress - projectedPx / panelWidthPx
                        val shouldClose = dragProgress < 0.70f || projectedProgress < 0.82f
                        val progressVelocity = (-velocity / panelWidthPx).coerceIn(-8f, 8f)
                        val current = dragProgress
                        scope.launch {
                            progress.snapTo(current)
                            dragging = false
                            releaseVelocity = progressVelocity
                            if (shouldClose) onClose()
                        }
                    },
                )
                .graphicsLayer {
                    val p = if (dragging) dragProgress else progress.value
                    translationX = (1f - p) * panelWidthPx
                    translationY = if (handleDragging) handleDragPx else handleOffset.value
                    alpha = (p * 12f).coerceIn(0f, 1f)
                }
                .semantics {
                    contentDescription = "凭证详情，${shownCard.credential.name}"
                }
                .testTag("home_wide_detail"),
            // 抽屉是“页面”，与背后页面同用 canvas 底色，靠 1dp 描边和阴影分隔。
            surfaceRole = MyTokenVisualPolicy.SurfaceRole.Drawer,
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(
                        WindowInsets.systemBars.only(
                            WindowInsetsSides.Top +
                                WindowInsetsSides.Bottom +
                                WindowInsetsSides.End,
                        ),
                    ),
            ) {
                DrawerHandle(
                    enabled = selectedCard != null,
                    onDragStart = {
                        handleDragging = true
                        handleDragPx = 0f
                    },
                    onDrag = { dy ->
                        handleDragPx = (handleDragPx + dy).coerceIn(0f, maxHandleDragPx)
                    },
                    onDragCancel = {
                        scope.launch {
                            handleOffset.snapTo(handleDragPx)
                            handleDragging = false
                            handleDragPx = 0f
                            handleOffset.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    },
                    onDragEnd = { velocity ->
                        val shouldClose = handleDragPx > handleClosePx || velocity > 1_200f
                        val current = handleDragPx
                        scope.launch {
                            handleOffset.snapTo(current)
                            handleDragging = false
                            handleDragPx = 0f
                            if (shouldClose) onClose()
                            handleOffset.animateTo(
                                0f,
                                spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMediumLow,
                                ),
                            )
                        }
                    },
                )
                // 切换卡片时重置滚动位置与分享弹窗，详情从顶部开始。
                key(shownCard.credential.id) {
                    CredentialDetailPane(
                        card = shownCard,
                        layoutMode = layoutMode,
                        globalAlertThresholds = globalAlertThresholds,
                        alertThresholdOverride = alertThresholdOverrides[shownCard.credential.id],
                        onClose = onClose,
                        onRefresh = { onRefresh(shownCard.credential.id) },
                        onEdit = { onEdit(shownCard.credential.id) },
                        onDelete = { onDelete(shownCard) },
                    )
                }
            }
        }
    }
    }
}

/** 顶部下拉手柄；只在手柄区域接管纵向手势，详情内容仍可正常滚动。 */
@Composable
private fun DrawerHandle(
    enabled: Boolean,
    onDragStart: () -> Unit,
    onDrag: (Float) -> Unit,
    onDragCancel: () -> Unit,
    onDragEnd: (Float) -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .then(
                if (enabled) {
                    Modifier.pointerInput(Unit) {
                        val touchSlop = viewConfiguration.touchSlop
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            val tracker = VelocityTracker()
                            tracker.addPosition(down.uptimeMillis, down.position)
                            var totalX = 0f
                            var totalY = 0f
                            var started = false
                            var aborted = false
                            var canceled = false
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.id == down.id }
                                if (change == null) {
                                    canceled = true
                                    break
                                }
                                if (change.isConsumed) {
                                    if (started) canceled = true else aborted = true
                                    break
                                }
                                val dx = change.position.x - change.previousPosition.x
                                val dy = change.position.y - change.previousPosition.y
                                totalX += dx
                                totalY += dy
                                tracker.addPosition(change.uptimeMillis, change.position)
                                if (!started && !aborted) {
                                    if (abs(totalY) > touchSlop && abs(totalY) > abs(totalX)) {
                                        if (totalY > 0f) {
                                            started = true
                                            onDragStart()
                                        } else {
                                            aborted = true
                                        }
                                    } else if (abs(totalX) > touchSlop && abs(totalX) > abs(totalY)) {
                                        aborted = true
                                    }
                                } else if (started) {
                                    onDrag(dy)
                                    change.consume()
                                }
                                if (!change.pressed) break
                            }
                            if (started) {
                                if (canceled || aborted) {
                                    onDragCancel()
                                } else {
                                    onDragEnd(tracker.calculateVelocity().y)
                                }
                            }
                        }
                    }
                } else {
                    Modifier
                },
            )
            .testTag("home_wide_detail_handle"),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(colors.line),
        )
    }
}
