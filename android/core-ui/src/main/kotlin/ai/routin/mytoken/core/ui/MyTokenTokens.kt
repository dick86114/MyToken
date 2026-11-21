package ai.routin.mytoken.core.ui

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * “安静的仪表”颜色单一来源（设计文档 §6.1）。
 *
 * 角色划分：`canvas/surface/subtle/line` 承载结构，`ink/muted` 承载文字，
 * `action*` 只做交互，`positive/warning/critical` 只做数据与异常，
 * `cat1/2/3` 是墨色深浅，用于构成条分类。
 */
@Immutable
data class MyTokenColors(
    val canvas: Color,
    val surface: Color,
    val subtle: Color,
    val line: Color,
    val ink: Color,
    val muted: Color,
    val actionFill: Color,
    val actionText: Color,
    val onAction: Color,
    val positiveFill: Color,
    val positiveText: Color,
    val warningFill: Color,
    val warningText: Color,
    val criticalFill: Color,
    val criticalText: Color,
    val cat1: Color,
    val cat2: Color,
    val cat3: Color,
    val track: Color,
    val statusDotNormal: Color,
    // 首页琥珀报告：正文区随主题反相，票据主区固定为琥珀纸色。
    val reportSurface: Color,
    val reportInk: Color,
    val reportMuted: Color,
    val reportLine: Color,
    val reportAccent: Color,
    val reportTicket: Color,
    val reportTicketInk: Color,
    val reportTicketMuted: Color,
    val reportDialTrack: Color,
)

internal val MyTokenLightColors = MyTokenColors(
    canvas = Color(0xFFEEF0F2),
    surface = Color(0xFFFFFFFF),
    subtle = Color(0xFFF6F7F9),
    line = Color(0xFFE5E7EB),
    ink = Color(0xFF17181A),
    muted = Color(0xFF6B7076),
    // 交互强调与报告票面共用同一块黄纸；深色墨字保证浅色表面上的对比度。
    actionFill = Color(0xFFF3CB73),
    actionText = Color(0xFF625126),
    onAction = Color(0xFFFFFFFF),
    positiveFill = Color(0xFF30C48D),
    positiveText = Color(0xFF14805A),
    warningFill = Color(0xFFF5A524),
    warningText = Color(0xFF9A5B00),
    criticalFill = Color(0xFFE5484D),
    criticalText = Color(0xFFC0272D),
    cat1 = Color(0xFF2B2D31),
    cat2 = Color(0xFF8A8F96),
    cat3 = Color(0xFFC9CDD3),
    track = Color(0xFFE9EBEE),
    statusDotNormal = Color(0xFFA3A9B0),
    reportSurface = Color(0xFF17181A),
    reportInk = Color(0xFFF5F6F7),
    reportMuted = Color(0xFFB6BCC4),
    reportLine = Color(0xFF4A4F56),
    reportAccent = Color(0xFFFFCE70),
    reportTicket = Color(0xFFF3CB73),
    reportTicketInk = Color(0xFF17181A),
    reportTicketMuted = Color(0xFF625126),
    reportDialTrack = Color(0xFFB4A071),
)

internal val MyTokenDarkColors = MyTokenColors(
    canvas = Color(0xFF0D0F12),
    surface = Color(0xFF16181C),
    subtle = Color(0xFF1D2025),
    line = Color(0xFF292D33),
    ink = Color(0xFFF5F6F7),
    muted = Color(0xFF9BA1A8),
    actionFill = Color(0xFFF3CB73),
    actionText = Color(0xFFF3CB73),
    onAction = Color(0xFF061326),
    positiveFill = Color(0xFF35D399),
    positiveText = Color(0xFF5BE0AC),
    warningFill = Color(0xFFFFB340),
    warningText = Color(0xFFFFC266),
    criticalFill = Color(0xFFFF6369),
    criticalText = Color(0xFFFF8A8E),
    cat1 = Color(0xFFE8EAED),
    cat2 = Color(0xFF9BA1A8),
    cat3 = Color(0xFF4A4F56),
    track = Color(0xFF2A2E34),
    statusDotNormal = Color(0xFF6B7076),
    reportSurface = Color(0xFFF5F6F7),
    reportInk = Color(0xFF17181A),
    reportMuted = Color(0xFF696D73),
    reportLine = Color(0xFFD2D6DC),
    reportAccent = Color(0xFF9A5B00),
    reportTicket = Color(0xFFF3CB73),
    reportTicketInk = Color(0xFF17181A),
    reportTicketMuted = Color(0xFF625126),
    reportDialTrack = Color(0xFFB4A071),
)

internal val LocalMyTokenColors: ProvidableCompositionLocal<MyTokenColors> =
    staticCompositionLocalOf { MyTokenLightColors }

/**
 * 设置页主题预览卡的固定场景色：无论当前外观如何，预览卡都要展示
 * 浅色、深色和跟随系统的真实对比，因此不能取自当前主题。
 */
object MyTokenThemePreviewColors {
    val lightScene = Color(0xFFFFFFFF)
    val darkScene = Color(0xFF16181C)
    val lightLine = Color(0xFFC9CDD3)
    val lightCard = Color(0xFFEEF0F2)
    val darkLine = Color(0xFF4A4F56)
    val darkCard = Color(0xFF292D33)
    val darkBorder = Color(0xFF292D33)
    val systemLine = Color(0xFF9BA1A8)
    val systemCard = Color(0xFF9BA1A8)
    val sceneBorder = Color(0xFFE5E7EB)
}

/** 业务代码读取颜色的唯一入口。 */
object MyTokenThemeTokens {
    val colors: MyTokenColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMyTokenColors.current
}
