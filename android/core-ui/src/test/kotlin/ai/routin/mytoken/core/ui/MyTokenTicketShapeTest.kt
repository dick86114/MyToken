package ai.routin.mytoken.core.ui

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

/** 锁定票据只使用圆角，不再保留切角。 */
class MyTokenTicketShapeTest {

    @Test
    fun `报告主体四角均为圆角`() {
        assertEquals(RoundedCornerShape(20.dp), MyTokenReportBodyShape)
    }

    @Test
    fun `报告票面顶部两角为圆角`() {
        assertEquals(
            RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            MyTokenReportTicketShape,
        )
    }
}
