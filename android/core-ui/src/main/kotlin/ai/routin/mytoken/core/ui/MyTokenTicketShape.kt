package ai.routin.mytoken.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** 报告/铭牌主体：四角圆角，去掉原先的右上、左下切角。 */
val MyTokenReportBodyShape: Shape = RoundedCornerShape(20.dp)

/** 报告/铭牌黄色票面：顶部两角圆角，底部与主体接缝保持直角。 */
val MyTokenReportTicketShape: Shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
