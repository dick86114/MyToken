package ai.routin.mytoken.core.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class HelpMarker {
    Positive,
    Warning,
    Critical,
}

data class HelpRow(
    val label: String,
    val detail: String? = null,
    val marker: HelpMarker? = null,
)

data class HelpSection(
    val title: String? = null,
    val rows: List<HelpRow>,
)

/** 轻量问号入口：只把真正需要解释的规则放进点击后的说明里。 */
@Composable
fun GlassHelpButton(
    title: String,
    sections: List<HelpSection>,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    icon: ImageVector = Icons.Outlined.Info,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(
        modifier = modifier
            .size(28.dp)
            .clip(CircleShape)
            .clickable { expanded = true }
            .semantics { contentDescription = "查看$title 说明" },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.size(17.dp),
        )
    }

    if (expanded) {
        MyTokenDialog(
            title = title,
            subtitle = subtitle,
            icon = icon,
            onDismissRequest = { expanded = false },
            content = {
                sections.forEach { section ->
                    MyTokenDialogSection(title = section.title) {
                        section.rows.forEach { row ->
                            MyTokenDialogRow(
                                label = row.label,
                                detail = row.detail,
                                markerColor = when (row.marker) {
                                    HelpMarker.Positive -> MaterialTheme.colorScheme.secondary
                                    HelpMarker.Warning -> MaterialTheme.colorScheme.tertiary
                                    HelpMarker.Critical -> MaterialTheme.colorScheme.error
                                    null -> null
                                },
                            )
                        }
                    }
                }
            },
            actions = {
                GlassButton(
                    text = "知道了",
                    onClick = { expanded = false },
                    modifier = Modifier.fillMaxWidth(),
                    tone = GlassButtonTone.Primary,
                )
            },
        )
    }
}
