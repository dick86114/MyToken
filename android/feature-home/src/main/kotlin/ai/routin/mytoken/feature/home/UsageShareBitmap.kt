package ai.routin.mytoken.feature.home

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import java.io.File

internal object UsageShareBitmapFactory {
    fun qrCode(size: Int): Bitmap {
        val matrix = QRCodeWriter().encode(
            "https://mytoken.idickies.cc/",
            BarcodeFormat.QR_CODE,
            size,
            size,
        )
        val white = Color.WHITE
        val black = Color.BLACK
        return Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565).also { bitmap ->
            for (x in 0 until size) {
                for (y in 0 until size) {
                    bitmap.setPixel(x, y, if (matrix[x, y]) black else white)
                }
            }
        }
    }
}

internal fun renderUsageShareBitmap(card: UsageShareRenderedCard, density: Float): Bitmap {
    val width = (UsageShareCardRenderer.CANVAS_WIDTH * density).toInt()
    val height = UsageShareCardRenderer.height(card, width)
    return Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also { bitmap ->
        UsageShareCardRenderer.draw(Canvas(bitmap), card, width)
    }
}

/** 生成并导出分享图；返回给编辑器展示的操作结果文案。 */
internal suspend fun exportUsageShareImage(
    context: Context,
    card: UsageShareRenderedCard,
    action: UsageShareAction,
    targetPackage: String? = null,
): String? = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
    val bitmap = renderUsageShareBitmap(card, context.resources.displayMetrics.density)
    try {
        when (action) {
            UsageShareAction.Copy -> {
                val uri = cacheShareUri(context, card, bitmap)
                val clipData = ClipData(
                    ClipDescription("MyToken 分享图", arrayOf("image/png")),
                    ClipData.Item(uri),
                )
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(clipData)
                "已复制到剪贴板"
            }

            UsageShareAction.Save -> {
                val values = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, UsageShareContentBuilder.fileName(card, java.time.Instant.now()))
                    put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, "Pictures/MyToken")
                }
                val collection = android.provider.MediaStore.Images.Media.getContentUri(
                    android.provider.MediaStore.VOLUME_EXTERNAL_PRIMARY,
                )
                val targetUri = context.contentResolver.insert(collection, values)
                    ?: return@withContext "保存失败"
                val saved = context.contentResolver.openOutputStream(targetUri)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                } ?: false
                if (saved) "已保存到 Pictures/MyToken" else "保存失败"
            }

            UsageShareAction.Share -> {
                val uri = cacheShareUri(context, card, bitmap)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "image/png"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_TITLE, "MyToken 用量分享图")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    targetPackage?.let(::setPackage)
                }
                if (targetPackage == null) {
                    context.startActivity(Intent.createChooser(intent, "分享用量"))
                } else {
                    runCatching { context.startActivity(intent) }.onFailure {
                        context.startActivity(Intent.createChooser(intent, "分享用量"))
                    }
                }
                null
            }
        }
    } catch (_: Exception) {
        when (action) {
            UsageShareAction.Copy -> "复制失败"
            UsageShareAction.Save -> "保存失败"
            UsageShareAction.Share -> null
        }
    } finally {
        bitmap.recycle()
    }
}

private fun cacheShareUri(context: Context, card: UsageShareRenderedCard, bitmap: Bitmap) : android.net.Uri {
    val directory = File(context.cacheDir, "share").apply { mkdirs() }
    val file = File(directory, UsageShareContentBuilder.fileName(card, java.time.Instant.now()))
    file.outputStream().use { output ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
    }
    return FileProvider.getUriForFile(
        context,
        "${context.packageName}.update.fileprovider",
        file,
    )
}
