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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.dp

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
                UsageShareCard(
                    displayName = displayName,
                    providerName = providerName,
                    planName = planName,
                    snapshot = snapshot,
                    maxWidth = cardMax,
                )
            }

            Spacer(Modifier.height(16.dp))

            Row(
                Modifier.fillMaxWidth().padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        isSharing = true
                        shareUsageImage(context, displayName, providerName, planName, snapshot, targetPackage = "com.tencent.mm")
                        isSharing = false
                    },
                    enabled = !isSharing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text("微信")
                }
                Button(
                    onClick = {
                        isSharing = true
                        shareUsageImage(context, displayName, providerName, planName, snapshot)
                        isSharing = false
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
