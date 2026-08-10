import Foundation

/// 菜单栏中使用的显示样式。
enum MenuBarStyle: String, CaseIterable, Codable, Equatable, Sendable {
    case percent
    case aliasVerticalBar
    case aliasPercent

    /// 设置界面中显示的中文名称。
    var title: String {
        switch self {
        case .percent:
            "仅百分比"
        case .aliasVerticalBar:
            "别名 + 竖形进度条"
        case .aliasPercent:
            "别名 + 百分比"
        }
    }
}
