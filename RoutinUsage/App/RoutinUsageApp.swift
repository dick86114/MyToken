import AppKit
import SwiftUI

final class RoutinUsageAppDelegate: NSObject, NSApplicationDelegate {
    func applicationShouldTerminateAfterLastWindowClosed(_ sender: NSApplication) -> Bool {
        return false
    }
}

@main
@MainActor
struct RoutinUsageApp: App {
    nonisolated static let applicationName = "MyRoutin"

    @NSApplicationDelegateAdaptor(RoutinUsageAppDelegate.self) private var appDelegate
    @State private var environment: AppEnvironment
    @State private var statusBarController: StatusBarController?

    private static var isRunningUnitTests: Bool {
        ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil
    }

    init() {
        let environment = AppEnvironment.live()
        _environment = State(initialValue: environment)
        _statusBarController = State(
            initialValue: Self.isRunningUnitTests ? nil : StatusBarController(environment: environment)
        )
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
                moveKey: environment.moveKey(fromOffsets:toOffset:),
                updateStatus: environment.updateStatus,
                checkForUpdates: environment.checkForUpdates,
                installAvailableUpdate: environment.installAvailableUpdate,
                submitIssueReport: environment.openIssueReport,
                readKey: environment.readKey(id:)
            )
        }
        .defaultSize(
            width: WindowFramePersistence.defaultSize.width,
            height: WindowFramePersistence.defaultSize.height
        )
        .windowResizability(.contentMinSize)
    }
}
