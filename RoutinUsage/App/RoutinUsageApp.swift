import SwiftUI

@main
@MainActor
struct RoutinUsageApp: App {
    nonisolated static let applicationName = "MyRoutin"

    @State private var environment: AppEnvironment

    init() {
        _environment = State(initialValue: AppEnvironment.live())
    }

    var body: some Scene {
        MenuBarExtra {
            UsagePopoverView(
                store: environment.store,
                settings: environment.settings,
                updateStatus: environment.updateStatus,
                installAvailableUpdate: environment.installAvailableUpdate
            )
        } label: {
            AppMenuBarLabel(environment: environment)
        }
        .menuBarExtraStyle(.window)
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

@MainActor
private struct AppMenuBarLabel: View {
    @Bindable var environment: AppEnvironment

    var body: some View {
        MenuBarLabelView(
            store: environment.store,
            settings: environment.settings,
            checkForUpdates: environment.checkForUpdates
        )
        .task {
            await environment.start()
        }
        .onChange(of: environment.settings.refreshMinutes) { _, minutes in
            environment.refreshIntervalDidChange(to: minutes)
        }
        .onChange(of: environment.settings.notificationsEnabled) { _, enabled in
            Task {
                await environment.notificationsDidChange(enabled: enabled)
            }
        }
        .onChange(of: environment.settings.thresholds) { _, thresholds in
            environment.thresholdsDidChange(to: thresholds)
        }
        .onChange(of: environment.store.selectedKeyID) { _, keyID in
            Task {
                await environment.selectedKeyDidChange(to: keyID)
            }
        }
        .sheet(isPresented: $environment.showsOnboarding) {
            OnboardingView(store: environment.store) {
                environment.dismissOnboarding()
            }
        }
    }
}
