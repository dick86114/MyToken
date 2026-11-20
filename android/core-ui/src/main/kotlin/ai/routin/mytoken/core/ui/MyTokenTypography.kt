package ai.routin.mytoken.core.ui

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

/**
 * MyToken 的语义字号层级（设计文档 §6.3）。
 *
 * 七级主层级负责结构与正文；数字与详情专用变体保证业务文件不需要内联 fontSize。
 * 数字统一使用 tabular numerals（`tnum`），中文 token 字距为 0。
 */
object MyTokenType {

    /** 余额金额、详情 Hero 数字。 */
    val display = TextStyle(
        fontSize = 32.sp,
        lineHeight = 36.sp,
        fontWeight = FontWeight(750),
        letterSpacing = (-0.8).sp,
        fontFeatureSettings = "tnum",
    )

    /** 简洁模式余额金额。 */
    val displayCompact = TextStyle(
        fontSize = 24.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight(750),
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = "tnum",
    )

    /** 页面标题。 */
    val title = TextStyle(
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight(700),
        letterSpacing = 0.sp,
    )

    /** 区块标题、完整模式卡片名。 */
    val section = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight(650),
        letterSpacing = 0.sp,
    )

    /** 正文、设置行。 */
    val body = TextStyle(
        fontSize = 14.sp,
        lineHeight = 21.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )

    /** 指标标签、按钮。 */
    val label = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.sp,
    )

    /** 说明、图例、页脚。 */
    val caption = TextStyle(
        fontSize = 11.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.sp,
    )

    /** 拉丁单位、重置卡数字；不用于中文句子。 */
    val micro = TextStyle(
        fontSize = 11.sp,
        lineHeight = 15.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    )

    /** 详情页名称。 */
    val detailTitle = TextStyle(
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight(700),
        letterSpacing = 0.sp,
    )

    /** 列表行名称。 */
    val listTitle = TextStyle(
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight(600),
        letterSpacing = 0.sp,
    )

    /** 简洁弧线圆心数字。 */
    val ringCompact = TextStyle(
        fontSize = 12.5.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(700),
        letterSpacing = (-0.3).sp,
        fontFeatureSettings = "tnum",
    )

    /** 完整弧线圆心数字。 */
    val ringFull = TextStyle(
        fontSize = 15.sp,
        lineHeight = 19.sp,
        fontWeight = FontWeight(700),
        letterSpacing = (-0.5).sp,
        fontFeatureSettings = "tnum",
    )

    /** 详情 Hero 的 88dp 弧线圆心数字。 */
    val detailRing = TextStyle(
        fontSize = 20.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(750),
        letterSpacing = (-0.6).sp,
        fontFeatureSettings = "tnum",
    )

    /** 分节小标题（设计稿 section-label）：11sp 大写字距。 */
    val sectionLabel = TextStyle(
        fontSize = 11.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight(650),
        letterSpacing = 0.9.sp,
    )

    /** 百分号符号，仅用于数字旁。 */
    val percentMark = TextStyle(
        fontSize = 9.sp,
        lineHeight = 12.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.sp,
    )

    /** 详情 Hero 的剩余量。 */
    val detailRemaining = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(650),
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )

    /** 简洁值网格等中号数字。 */
    val numericValue = TextStyle(
        fontSize = 18.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight(650),
        letterSpacing = 0.sp,
        fontFeatureSettings = "tnum",
    )
}

internal val MyTokenTypography = Typography(
    headlineLarge = MyTokenType.title,
    headlineMedium = MyTokenType.title,
    headlineSmall = MyTokenType.section,
    titleLarge = MyTokenType.section,
    titleMedium = MyTokenType.section,
    titleSmall = MyTokenType.label,
    bodyLarge = MyTokenType.body,
    bodyMedium = MyTokenType.body,
    bodySmall = MyTokenType.caption,
    labelLarge = MyTokenType.label,
    labelMedium = MyTokenType.label,
    labelSmall = MyTokenType.micro,
)
