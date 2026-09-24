import Foundation

/// 菜单栏中使用的显示样式。
///
/// 历史设置项：菜单栏现在统一由多指标图标渲染，这个样式只用于
/// 读取和迁移旧配置，不再决定实际显示效果。
enum MenuBarStyle: String, CaseIterable, Codable, Equatable, Sendable {
    case percent
    case aliasLogoProgress
    case logoProgress
    case aliasVerticalBar
    case aliasPercent

    /// 设置界面中显示的中文名称。
    var title: String {
        switch self {
        case .percent:
            "仅百分比"
        case .aliasLogoProgress:
            "别名 + Logo 进度"
        case .logoProgress:
            "仅 Logo 进度"
        case .aliasVerticalBar:
            "别名 + 竖形进度条"
        case .aliasPercent:
            "别名 + 百分比"
        }
    }
}
