package ai.routin.mytoken.core.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** 统一菜单容器：浮层 overlay 底色 + hairline，只有浮层投影，不带 Material 默认大圆角。 */
@Composable
fun MyTokenDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MyTokenThemeTokens.colors
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        modifier = modifier,
        shape = RoundedCornerShape(MyTokenRadius.inner),
        containerColor = colors.overlay,
        tonalElevation = 0.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, colors.line),
        content = content,
    )
}

/** 菜单项：正文 14sp、48dp 行高；选中项加粗并显示勾，危险操作使用错误色。 */
@Composable
fun MyTokenDropdownMenuItem(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    destructive: Boolean = false,
    leadingIcon: ImageVector? = null,
    count: String? = null,
) {
    val colors = MyTokenThemeTokens.colors
    val contentColor = if (destructive) colors.criticalText else colors.ink
    DropdownMenuItem(
        text = {
            Text(
                text = if (count != null) "$text $count" else text,
                style = MyTokenType.body,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                color = contentColor,
                maxLines = 1,
            )
        },
        onClick = onClick,
        modifier = modifier.semantics { this.selected = selected },
        leadingIcon = leadingIcon?.let { icon ->
            {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (destructive) colors.criticalText else colors.muted,
                    modifier = Modifier.size(18.dp),
                )
            }
        },
        trailingIcon = if (selected) {
            {
                Icon(
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = colors.ink,
                    modifier = Modifier.size(18.dp),
                )
            }
        } else {
            null
        },
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    )
}
