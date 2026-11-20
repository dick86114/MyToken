package ai.routin.mytoken.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * MyToken 的语义字号层级。
 *
 * 页面标题、主数值、数据标签和辅助说明各只保留一个常用级别；大字号收紧字距与行高，
 * 小字号保持略宽松的行距。业务界面优先引用这里的 token，避免同一层级在不同页面
 * 出现 8sp、9sp、10sp 混用。
 */
object MyTokenType {
    val pageTitle = TextStyle(
        fontSize = 30.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.6).sp,
    )

    val sectionTitle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.1).sp,
    )

    val cardTitle = TextStyle(
        fontSize = 16.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    )

    val cardTitleLong = cardTitle.copy(fontSize = 14.sp, lineHeight = 18.sp)
    val cardTitleDense = cardTitle.copy(fontSize = 13.sp, lineHeight = 17.sp)

    val cardMeta = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Normal,
    )

    val heroNumber = TextStyle(
        fontSize = 31.sp,
        lineHeight = 33.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-1.1).sp,
    )

    val balanceNumber = heroNumber.copy(fontSize = 28.sp, lineHeight = 31.sp, letterSpacing = (-0.9).sp)

    val metricLabel = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.05.sp,
    )

    val metricLabelTight = metricLabel.copy(fontSize = 11.sp, lineHeight = 15.sp)

    val metricValue = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val metricValueStrong = metricValue.copy(fontSize = 14.sp, lineHeight = 18.sp)

    val metricCaption = TextStyle(
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Normal,
    )

    val factLabel = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Normal,
    )

    val factValue = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
    )

    val chip = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    )

    val badge = TextStyle(
        fontSize = 10.sp,
        lineHeight = 13.sp,
        fontWeight = FontWeight.SemiBold,
    )

    val sync = TextStyle(
        fontSize = 10.5.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.Medium,
    )
}

internal val MyTokenTypography = Typography(
    headlineLarge = TextStyle(
        fontSize = 28.sp,
        lineHeight = 34.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.5).sp,
    ),
    headlineMedium = TextStyle(
        fontSize = 24.sp,
        lineHeight = 30.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp,
    ),
    headlineSmall = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.3).sp,
    ),
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = (-0.2).sp,
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    bodyLarge = TextStyle(
        fontSize = 15.sp,
        lineHeight = 23.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
    ),
    bodySmall = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Normal,
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium,
    ),
    labelSmall = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.Medium,
    ),
)
