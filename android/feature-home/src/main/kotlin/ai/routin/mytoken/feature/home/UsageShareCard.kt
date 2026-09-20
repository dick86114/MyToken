package ai.routin.mytoken.feature.home

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

/** Android View 包装器，保证分享预览与导出 PNG 走同一套 Canvas 绘制。 */
internal class UsageShareCardRenderView(context: Context) : View(context) {
    var card: UsageShareRenderedCard? = null
        private set

    fun update(newCard: UsageShareRenderedCard) {
        card = newCard
        requestLayout()
        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val measuredHeight = card?.let { UsageShareCardRenderer.height(it, width) } ?: 0
        setMeasuredDimension(width, measuredHeight)
    }

    override fun onDraw(canvas: android.graphics.Canvas) {
        val currentCard = card ?: return
        UsageShareCardRenderer.draw(canvas, currentCard, width)
    }
}

/** 窄屏下按 macOS 的 430pt 版心等比缩小，避免横向裁切。 */
@Composable
internal fun UsageShareCardPreview(
    card: UsageShareRenderedCard,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val canvasWidth = 430.dp
    val canvasWidthPx = with(density) { canvasWidth.toPx() }
    val canvasHeightPx = remember(card, canvasWidthPx) {
        UsageShareCardRenderer.height(card, canvasWidthPx.toInt())
    }
    val canvasHeight = with(density) { canvasHeightPx.toDp() }

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val displayedWidth = minOf(maxWidth, canvasWidth)
        val scale = displayedWidth / canvasWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(canvasHeight * scale),
            contentAlignment = Alignment.TopStart,
        ) {
            AndroidView(
                factory = { context ->
                    UsageShareCardRenderView(context).apply { update(card) }
                },
                update = { view -> view.update(card) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(canvasHeight * scale),
            )
        }
    }
}
