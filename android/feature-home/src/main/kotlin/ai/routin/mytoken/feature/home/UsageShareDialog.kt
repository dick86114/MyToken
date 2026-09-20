package ai.routin.mytoken.feature.home

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UsageShareDialog(
    displayName: String,
    providerName: String,
    planName: String,
    snapshot: ai.routin.mytoken.domain.model.UsageSnapshot?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val graphicsLayer = rememberGraphicsLayer()
    val scope = rememberCoroutineScope()
    var isSharing by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                "分享用量",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            BoxWithConstraints(
                Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                val cardMax = maxWidth - 24.dp
                Box(
                    Modifier.drawWithContent {
                        graphicsLayer.record(density, layoutDirection, androidx.compose.ui.unit.IntSize(size.width.toInt(), size.height.toInt())) {
                            this@drawWithContent.drawContent()
                        }
                        drawContent()
                    }
                ) {
                    UsageShareCard(
                        displayName = displayName,
                        providerName = providerName,
                        planName = planName,
                        snapshot = snapshot,
                        maxWidth = cardMax,
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            isSharing = true
                            shareBitmap(context, graphicsLayer, displayName, targetPackage = "com.tencent.mm")
                            isSharing = false
                        }
                    },
                    enabled = !isSharing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("微信")
                }
                Button(
                    onClick = {
                        scope.launch {
                            isSharing = true
                            shareBitmap(context, graphicsLayer, displayName)
                            isSharing = false
                        }
                    },
                    enabled = !isSharing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("更多")
                }
            }
        }
    }
}

private suspend fun shareBitmap(
    context: Context,
    layer: GraphicsLayer,
    name: String,
    targetPackage: String? = null,
) {
    try {
        val bitmap = layer.toImageBitmap().asAndroidBitmap()
        val dir = File(context.cacheDir, "share")
        dir.mkdirs()
        val file = File(dir, "mytoken_share_${name.hashCode()}.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.update.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            targetPackage?.let { setPackage(it) }
        }
        if (targetPackage != null) {
            runCatching { context.startActivity(intent) }.onFailure {
                // 微信未安装时回退到系统分享
                val fallback = Intent.createChooser(intent.removeExtra(Intent.EXTRA_STREAM).let { intent }, "分享用量")
                context.startActivity(fallback)
            }
        } else {
            context.startActivity(Intent.createChooser(intent, "分享用量"))
        }
    } catch (_: Exception) {
        // 分享失败不崩溃
    }
}
