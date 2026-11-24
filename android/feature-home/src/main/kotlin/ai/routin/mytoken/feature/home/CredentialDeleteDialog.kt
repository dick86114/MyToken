package ai.routin.mytoken.feature.home

import ai.routin.mytoken.core.ui.MyTokenButton
import ai.routin.mytoken.core.ui.MyTokenButtonTone
import ai.routin.mytoken.core.ui.MyTokenDialog
import ai.routin.mytoken.core.ui.MyTokenDialogRow
import ai.routin.mytoken.core.ui.MyTokenDialogSection
import ai.routin.mytoken.core.ui.MyTokenDialogStatus
import ai.routin.mytoken.core.ui.MyTokenDialogTone
import ai.routin.mytoken.domain.model.Credential
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/** 删除凭证确认框：手机详情页与大屏右侧详情共用同一份文案与状态。 */
@Composable
fun CredentialDeleteDialog(
    target: Credential,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MyTokenDialog(
        title = "删除凭证",
        subtitle = "只影响本机数据",
        icon = Icons.Outlined.Delete,
        tone = MyTokenDialogTone.Danger,
        onDismissRequest = onDismiss,
        modifier = modifier,
        content = {
            MyTokenDialogSection {
                MyTokenDialogRow(
                    label = "「${target.name}」",
                    detail = "将从首页和凭证列表移除",
                )
            }
            MyTokenDialogStatus(
                message = "不会删除或修改供应商账户中的任何内容。",
                tone = MyTokenDialogTone.Info,
            )
        },
        actions = {
            MyTokenButton(
                text = "取消",
                onClick = onDismiss,
                modifier = Modifier.weight(1f),
                tone = MyTokenButtonTone.Neutral,
            )
            MyTokenButton(
                text = "删除",
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                tone = MyTokenButtonTone.Destructive,
            )
        },
    )
}
