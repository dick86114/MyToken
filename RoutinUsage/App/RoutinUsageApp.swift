import AppKit
import SwiftUI

final class RoutinUsageAppDelegate: NSObject, NSApplicationDelegate {
    static var didFinishLaunchingHandler: (() -> Void)?
    static let statusItemPositionCacheKey = "NSStatusItem Preferred Position Item-0"

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
        Self.closeLegacySuppressedLaunchWindows()
        // 旧版本曾保存状态项位置；移除旧缓存，避免升级后状态项落到不可见位置。
        UserDefaults.standard.removeObject(forKey: Self.statusItemPositionCacheKey)
        UserDefaults.standard.removeObject(forKey: "NSStatusItem VisibleCC MyRoutinStatusBar")
        UserDefaults.standard.removeObject(forKey: "NSStatusItem VisibleCC ai.routin.myroutin")
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            guard !RoutinUsageApp.isRunningUnitTests else {
                return
            }
            Self.didFinishLaunchingHandler?()
            SettingsWindowActivationPolicy.refresh()
        }
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return false
    }

    @MainActor
    static func closeLegacySuppressedLaunchWindows() {
        // SwiftUI 会把唯一窗口场景当作启动场景；登录窗口只能由用户明确重试触发。
        NSApp.windows
            .filter { $0.isVisible && ["Routin 签到", "登录小米 MiMo"].contains($0.title) }
            .forEach { $0.close() }
    }
}

@main
@MainActor
struct RoutinUsageApp: App {
    @MainActor private static var retainedStatusBarController: StatusBarController?
    nonisolated static let applicationName = "MyToken"
    nonisolated static let websiteURL = URL(string: "https://mytoken.idickies.cc")!
    nonisolated static let githubURL = URL(string: "https://github.com/dick86114/MyToken")!
    nonisolated static let releasesURL = URL(string: "https://github.com/dick86114/MyToken/releases")!

    nonisolated static var currentVersion: String {
        Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
    }

    @NSApplicationDelegateAdaptor(RoutinUsageAppDelegate.self) private var appDelegate
    @State private var environment: AppEnvironment
    @State private var statusBarController: StatusBarController?

    static var isRunningUnitTests: Bool {
        ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil
    }

    init() {
        let environment = AppEnvironment.live()
        _environment = State(initialValue: environment)
        _statusBarController = State(initialValue: nil)

        guard !Self.isRunningUnitTests else {
            return
        }
        RoutinUsageAppDelegate.didFinishLaunchingHandler = { [environment] in
            DispatchQueue.main.async {
                Self.installStatusBarController(environment: environment)
            }
        }
    }

    @MainActor
    private static func installStatusBarController(environment: AppEnvironment) {
        let controller = StatusBarController(environment: environment)
        retainedStatusBarController = controller
        controller.start()
    }

    var body: some Scene {
        Window("Routin 签到", id: "routin-check-in") {
            if let session = environment.routinWebSession {
                RoutinCheckInWindow(service: environment.routinCheckIn, session: session)
            } else {
                ContentUnavailableView("Routin 签到暂不可用", systemImage: "wifi.exclamationmark")
            }
        }
        .defaultSize(width: 720, height: 760)
        .windowResizability(.contentMinSize)
    }
}
