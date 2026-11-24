package ai.routin.mytoken.core.ui

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import kotlin.math.abs

enum class MyTokenEdgeDrawerSide {
    Start,
    End,
}

/**
 * 首页上方的双侧抽屉容器。
 *
 * 动画值只在 graphicsLayer 中读取，抽屉位移、首页缩放和遮罩透明度都不会触发整页
 * 重组；左/右边缘的横向手势与已打开抽屉的内侧抓握共用同一套进度模型。
 */
@Composable
fun MyTokenEdgeDrawerHost(
    drawerSide: MyTokenEdgeDrawerSide?,
    onDismissRequest: () -> Unit,
    startDrawer: @Composable () -> Unit,
    endDrawer: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var mountedSide by remember { mutableStateOf(drawerSide) }
    val progress = remember { Animatable(if (drawerSide == null) 0f else 1f) }
    var dragging by remember { mutableStateOf(false) }
    var dragSide by remember { mutableStateOf<MyTokenEdgeDrawerSide?>(null) }
    var dragProgress by remember { mutableFloatStateOf(progress.value) }
    var releaseVelocity by remember { mutableFloatStateOf(0f) }
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current

    suspend fun animateTo(target: Float, velocity: Float = 0f) {
        progress.animateTo(
            targetValue = target,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = 0.0005f,
            ),
            initialVelocity = velocity,
        )
    }

    // 所有开合动画只在这里串行执行。手势结束只提交目标状态，避免快速反向滑动时
    // 多个动画协程互相取消后留下半开、半关或不可见遮罩。
    LaunchedEffect(drawerSide, dragging) {
        if (dragging) return@LaunchedEffect
        val requestedSide = drawerSide
        if (requestedSide == null && mountedSide == null) return@LaunchedEffect

        if (requestedSide != null && mountedSide != requestedSide) {
            mountedSide = requestedSide
            dragSide = null
            dragProgress = 0f
            progress.snapTo(0f)
        }

        val velocity = releaseVelocity
        releaseVelocity = 0f
        animateTo(
            target = if (requestedSide != null) 1f else 0f,
            velocity = velocity,
        )

        if (requestedSide == null && !dragging) {
            dragSide = null
            mountedSide = null
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val drawerWidth = minOf(maxWidth * 0.88f, 420.dp)
        val drawerWidthPx = with(density) { drawerWidth.toPx() }
        val contentShiftPx = with(density) { 8.dp.toPx() }
        val side by remember {
            derivedStateOf {
                when {
                    dragging -> dragSide ?: drawerSide ?: mountedSide
                    drawerSide != null -> drawerSide
                    mountedSide != null && progress.value > 0.001f -> mountedSide
                    else -> null
                }
            }
        }
        fun beginDismissDrag() {
            dragging = true
            dragProgress = progress.value
            dragSide = null
            releaseVelocity = 0f
        }

        fun finishDismissDrag(sideValue: MyTokenEdgeDrawerSide, velocity: Float) {
            val outwardVelocity = if (sideValue == MyTokenEdgeDrawerSide.Start) -velocity else velocity
            val projectedPx = (outwardVelocity / 1000f) * 0.998f / (1f - 0.998f)
            val projectedProgress = dragProgress - projectedPx / drawerWidthPx
            val shouldClose = dragProgress < 0.70f || projectedProgress < 0.82f
            val progressVelocity = (
                if (sideValue == MyTokenEdgeDrawerSide.Start) {
                    velocity / drawerWidthPx
                } else {
                    -velocity / drawerWidthPx
                }
                ).coerceIn(-8f, 8f)
            val current = dragProgress
            scope.launch {
                progress.snapTo(current)
                dragging = false
                releaseVelocity = progressVelocity
                if (shouldClose) {
                    onDismissRequest()
                }
            }
        }

        fun cancelDismissDrag() {
            val current = dragProgress
            scope.launch {
                progress.snapTo(current)
                dragging = false
                releaseVelocity = 0f
            }
        }

        fun dismissDrawer() {
            dragging = false
            releaseVelocity = 0f
            onDismissRequest()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    val p = if (dragging) dragProgress else progress.value
                    val scale = 1f - 0.015f * p
                    scaleX = scale
                    scaleY = scale
                    translationX = contentShiftPx * p *
                        if (side == MyTokenEdgeDrawerSide.End) -1f else 1f
                    transformOrigin = TransformOrigin.Center
                },
        ) {
            content()
        }

        val activeSide = side
        if (activeSide != null) {
            val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
            val scrimBaseAlpha = if (isDark) 0.46f else 0.24f
            val scrimColor = if (isDark) Color.Black else Color(0xFF17222C)
            val scrimDismissModifier = if (drawerSide != null) {
                Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = ::dismissDrawer,
                    )
                    .semantics { contentDescription = "关闭侧边页" }
            } else {
                Modifier
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = scrimBaseAlpha * if (dragging) dragProgress else progress.value
                    }
                    .background(scrimColor)
                    // 收起过程中完全不挂点击节点，让触摸直接穿透到首页手势层。
                    .then(scrimDismissModifier)
                    .testTag(
                        if (activeSide == MyTokenEdgeDrawerSide.Start) {
                            "edge_drawer_scrim_start"
                        } else {
                            "edge_drawer_scrim_end"
                        }
                    ),
            )

            val drawerShape = if (activeSide == MyTokenEdgeDrawerSide.Start) {
                RoundedCornerShape(topEnd = 28.dp, bottomEnd = 28.dp)
            } else {
                RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
            }

            MyTokenSurface(
                modifier = Modifier
                    .align(
                        if (activeSide == MyTokenEdgeDrawerSide.Start) {
                            Alignment.CenterStart
                        } else {
                            Alignment.CenterEnd
                        },
                    )
                    .width(drawerWidth)
                    .fillMaxHeight()
                    .drawerSwipeToDismiss(
                        enabled = drawerSide != null,
                        side = activeSide,
                        drawerWidthPx = drawerWidthPx,
                        onStart = ::beginDismissDrag,
                        onDelta = { delta ->
                            dragProgress = (dragProgress + delta).coerceIn(0f, 1f)
                        },
                        onCancel = ::cancelDismissDrag,
                        onEnd = ::finishDismissDrag,
                    )
                    .graphicsLayer {
                        val p = if (dragging) dragProgress else progress.value
                        val direction =
                            if (activeSide == MyTokenEdgeDrawerSide.Start) -1f else 1f
                        translationX = (1f - p) * drawerWidthPx * direction
                        alpha = (p * 12f).coerceIn(0f, 1f)
                    }
                    .testTag(
                        if (activeSide == MyTokenEdgeDrawerSide.Start) {
                            "edge_drawer_start"
                        } else {
                            "edge_drawer_end"
                        }
                    ),
                surfaceRole = MyTokenVisualPolicy.SurfaceRole.Modal,
                shape = drawerShape,
                contentAlignment = Alignment.TopStart,
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    if (activeSide == MyTokenEdgeDrawerSide.Start) {
                        startDrawer()
                    } else {
                        endDrawer()
                    }
                }
                Box(
                    modifier = Modifier
                        .align(
                            if (activeSide == MyTokenEdgeDrawerSide.Start) {
                                Alignment.CenterEnd
                            } else {
                                Alignment.CenterStart
                            },
                        )
                        // 16dp 与抽屉页边距同宽，3dp 手柄正好落在页边距与面板边框中间。
                        .width(16.dp)
                        .height(240.dp)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onDismissRequest,
                        )
                        .semantics { contentDescription = "关闭侧边页" }
                        .testTag(
                            if (activeSide == MyTokenEdgeDrawerSide.Start) {
                                "edge_drawer_handle_start"
                            } else {
                                "edge_drawer_handle_end"
                            }
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(48.dp)
                            .clip(CircleShape)
                            .background(MyTokenThemeTokens.colors.actionFill)
                            .testTag(
                                if (activeSide == MyTokenEdgeDrawerSide.Start) {
                                    "edge_drawer_handle_bar_start"
                                } else {
                                    "edge_drawer_handle_bar_end"
                                }
                            ),
                    )
                }
            }
        }

    }
}

private fun Modifier.drawerSwipeToDismiss(
    enabled: Boolean,
    side: MyTokenEdgeDrawerSide,
    drawerWidthPx: Float,
    onStart: () -> Unit,
    onDelta: (Float) -> Unit,
    onCancel: () -> Unit,
    onEnd: (MyTokenEdgeDrawerSide, Float) -> Unit,
): Modifier {
    if (!enabled) return this
    return pointerInput(side, drawerWidthPx) {
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

            var finalized = false
            try {
                while (true) {
                    val event = awaitPointerEvent()
                    val change = event.changes.firstOrNull { it.id == down.id }
                    if (change == null) {
                        canceled = true
                        break
                    }
                    // 子级手势（滑块、卡片内拖动等）已经消费事件时，抽屉不再争抢，
                    // 否则横向拖动设置里的滑块会误触「滑动关闭设置」。
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
                        if (abs(totalX) > touchSlop && abs(totalX) > abs(totalY)) {
                            val outward =
                                if (side == MyTokenEdgeDrawerSide.Start) totalX < 0 else totalX > 0
                            if (outward) {
                                started = true
                                onStart()
                                val direction =
                                    if (side == MyTokenEdgeDrawerSide.Start) 1f else -1f
                                onDelta(totalX * direction / drawerWidthPx)
                                change.consume()
                            } else {
                                aborted = true
                            }
                        } else if (abs(totalY) > touchSlop && abs(totalY) > abs(totalX)) {
                            aborted = true
                        }
                    } else if (started) {
                        val direction = if (side == MyTokenEdgeDrawerSide.Start) 1f else -1f
                        onDelta(dx * direction / drawerWidthPx)
                        change.consume()
                    }
                    if (!change.pressed) break
                }
                if (started) {
                    if (canceled) {
                        onCancel()
                    } else {
                        onEnd(side, tracker.calculateVelocity().x)
                    }
                }
                finalized = true
            } finally {
                if (started && !finalized) {
                    onCancel()
                }
            }
        }
    }
}
