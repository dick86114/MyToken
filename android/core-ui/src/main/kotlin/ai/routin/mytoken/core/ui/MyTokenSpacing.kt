package ai.routin.mytoken.core.ui

import androidx.compose.ui.unit.dp

/** 间距 scale 与页面节奏（设计文档 §6.4）。 */
object MyTokenSpacing {
    val xxs = 2.dp
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp

    val page = 16.dp
    val cardCompact = 14.dp
    val cardFull = 16.dp
    val gridCompact = 8.dp
    val gridFull = 12.dp
    val section = 24.dp
    val settingsSection = 18.dp
    val settingsSectionCompact = 14.dp
}

/** 圆角：卡片 20、内嵌/按钮 12、列表行 18、sheet 顶部 24。 */
object MyTokenRadius {
    val card = 20.dp
    val inner = 12.dp
    val row = 18.dp
    val sheetTop = 24.dp
    val pill = 999.dp
}

/** 关键尺寸与触控区（设计文档 §6.4 / §6.7）。 */
object MyTokenSize {
    val touch = 48.dp
    val header = 44.dp
    val listRow = 64.dp
    val ringCompact = 52.dp
    val ringFull = 64.dp
    val ringHero = 88.dp
    val healthBar = 4.dp
    val switchWidth = 46.dp
    val switchHeight = 28.dp
    val settingsRowMin = 44.dp
    /** 浮起卡片描边（设计稿 §6：0.5dp）。 */
    val hairline = 0.5.dp
    /** 阈值双滑块（设计稿 §7：22dp 手柄、4dp 轨道）。 */
    val sliderHandle = 22.dp
    val sliderTrack = 4.dp
}

/** 轻阴影：只给首页浮起白卡与范围滑块手柄，保持很轻的一层。 */
object MyTokenElevation {
    val floatingCard = 4.dp
    val sliderHandle = 1.5.dp
}
