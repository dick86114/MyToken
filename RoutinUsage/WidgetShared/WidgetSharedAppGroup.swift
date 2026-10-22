import Foundation
import Security

/// 主 App 与 Widget 必须使用同一个 App Group 标识；entitlements 也必须包含该值。
enum WidgetSharedAppGroup {
    static let identifier = "group.cc.idickies.mytoken.widgets"

    /// 仅当当前进程的代码签名确实包含该 App Group 时才查询共享容器。
    /// 未签名包不满足系统容器成员条件，直接查询会触发“访问其他 App 的数据”授权弹窗。
    static func containerURL(
        entitlementGroups: [String]? = currentEntitlementGroups,
        lookup: () -> URL? = {
            FileManager.default.containerURL(
                forSecurityApplicationGroupIdentifier: WidgetSharedAppGroup.identifier
            )
        }
    ) -> URL? {
        guard let entitlementGroups,
              entitlementGroups.contains(identifier)
        else {
            return nil
        }
        return lookup()
    }

    private static var currentEntitlementGroups: [String]? {
        guard let task = SecTaskCreateFromSelf(nil),
              let value = SecTaskCopyValueForEntitlement(
                  task,
                  "com.apple.security.application-groups" as CFString,
                  nil
              )
        else {
            return nil
        }
        return value as? [String]
    }
}
