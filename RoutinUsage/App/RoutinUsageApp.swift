import SwiftUI

@main
struct RoutinUsageApp: App {
    static let applicationName = "Routin Usage"

    var body: some Scene {
        MenuBarExtra("…") {
            Text("尚未配置 Key")
        }
        Settings {
            Text("设置")
                .frame(width: 520, height: 420)
        }
    }
}
