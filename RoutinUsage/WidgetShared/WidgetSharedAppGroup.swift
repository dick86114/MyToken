import Foundation

/// 主 App 与 Widget 必须使用同一个 App Group 标识；entitlements 也必须包含该值。
enum WidgetSharedAppGroup {
    static let identifier = "group.cc.idickies.mytoken.widgets"
}
