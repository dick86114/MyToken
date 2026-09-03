import AppKit
import SwiftUI

final class RoutinUsageAppDelegate: NSObject, NSApplicationDelegate {
    static var didFinishLaunchingHandler: (() -> Void)?

    func applicationDidFinishLaunching(_ notification: Notification) {
        NSApp.setActivationPolicy(.accessory)
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.3) {
            guard !RoutinUsageApp.isRunningUnitTests else {
                return
            }
            SettingsWindowActivationPolicy.refresh()
            Self.didFinishLaunchingHandler?()
        }
    }

    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return false
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
            Self.installStatusBarController(environment: environment)
        }
    }

    @MainActor
    private static func installStatusBarController(environment: AppEnvironment) {
        retainedStatusBarController = StatusBarController(environment: environment)
    }

    var body: some Scene {
        Window("设置", id: "settings") {
            SettingsView(
                store: environment.store,
                settings: environment.settings,
                loginItemManager: environment.loginItemManager,
                updateValidatedKey: { id, name, secret in
                    try await environment.updateValidatedKey(
                        id: id,
                        name: name,
                        secret: secret
                    )
                },
                addValidatedCredential: environment.addValidatedCredential,
                updateValidatedCredential: environment.updateValidatedCredential,
                setKeyEnabled: environment.setKeyEnabled(_:enabled:),
                updateStatus: environment.updateStatus,
                checkForUpdates: environment.checkForUpdates,
                installAvailableUpdate: environment.installAvailableUpdate,
                submitIssueReport: environment.openIssueReport,
                readKey: environment.readKey(id:),
                routinCheckInState: environment.routinCheckIn.state,
                startRoutinCheckIn: environment.startRoutinCheckIn,
                beginRoutinLogin: environment.beginRoutinLogin,
                signOutRoutin: environment.signOutRoutin,
                deleteKey: environment.deleteKey(_:),
                codexGroupDetectionRecord: environment.codexGroupDetection.record(for:),
                clearCodexGroupDetection: environment.clearCodexGroupDetection(for:)
            )
        }
        .defaultSize(
            width: WindowFramePersistence.defaultSize.width,
            height: WindowFramePersistence.defaultSize.height
        )
        .windowResizability(.contentMinSize)

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
