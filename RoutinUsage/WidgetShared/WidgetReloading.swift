import Foundation
import WidgetKit

protocol WidgetReloading: Sendable {
    func reloadAllTimelines()
}

struct WidgetCenterReloader: WidgetReloading {
    func reloadAllTimelines() {
        WidgetCenter.shared.reloadAllTimelines()
    }
}
