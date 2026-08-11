import AppKit
import Observation
import SwiftUI

extension Notification.Name {
    static let showSettingsWindow = Notification.Name("showSettingsWindow")
}

@MainActor
final class StatusBarController: NSObject {
    private let environment: AppEnvironment
    private let statusItem = NSStatusBar.system.statusItem(withLength: NSStatusItem.variableLength)
    private let popover = NSPopover()
    private var refreshMinutes: Int
    private var notificationsEnabled: Bool
    private var thresholds: AlertThresholds
    private var selectedKeyID: UUID?

    init(environment: AppEnvironment) {
        self.environment = environment
        refreshMinutes = environment.settings.refreshMinutes
        notificationsEnabled = environment.settings.notificationsEnabled
        thresholds = environment.settings.thresholds
        selectedKeyID = environment.store.selectedKeyID
        super.init()

        configurePopover()
        configureStatusButton()
        updateStatusButton()
        observeEnvironment()

        Task { await environment.start() }
    }

    private func configurePopover() {
        popover.behavior = .transient
        popover.contentViewController = NSHostingController(
            rootView: StatusPopoverContent(environment: environment)
        )
    }

    private func configureStatusButton() {
        guard let button = statusItem.button else {
            return
        }
        button.target = self
        button.action = #selector(handleStatusButtonClick(_:))
        button.sendAction(on: [.leftMouseUp, .rightMouseUp])
        button.imagePosition = .imageRight
    }

    private func observeEnvironment() {
        withObservationTracking {
            _ = environment.settings.displayDimension
            _ = environment.settings.menuBarStyle
            _ = environment.settings.refreshMinutes
            _ = environment.settings.notificationsEnabled
            _ = environment.settings.thresholds
            _ = environment.store.selectedKeyID
            _ = environment.store.states
            _ = environment.updateStatus
        } onChange: { [weak self] in
            Task { @MainActor [weak self] in
                self?.synchronizeEnvironmentChanges()
                self?.observeEnvironment()
            }
        }
    }

    private func synchronizeEnvironmentChanges() {
        let settings = environment.settings
        if refreshMinutes != settings.refreshMinutes {
            refreshMinutes = settings.refreshMinutes
            environment.refreshIntervalDidChange(to: refreshMinutes)
        }
        if notificationsEnabled != settings.notificationsEnabled {
            notificationsEnabled = settings.notificationsEnabled
            Task { await environment.notificationsDidChange(enabled: notificationsEnabled) }
        }
        if thresholds != settings.thresholds {
            thresholds = settings.thresholds
            environment.thresholdsDidChange(to: thresholds)
        }
        if selectedKeyID != environment.store.selectedKeyID {
            selectedKeyID = environment.store.selectedKeyID
            Task { await environment.selectedKeyDidChange(to: selectedKeyID) }
        }
        updateStatusButton()
    }

    private func updateStatusButton() {
        guard let button = statusItem.button else {
            return
        }
        let state = environment.store.selectedKeyID.flatMap(environment.store.state(for:))
        let text = UsageFormatter.menuBarText(
            state: state,
            dimension: environment.settings.displayDimension,
            style: environment.settings.menuBarStyle
        )
        let metric = MenuBarVerticalUsage.metric(
            state: state,
            dimension: environment.settings.displayDimension,
            style: environment.settings.menuBarStyle
        )

        if let metric {
            button.title = text + " · "
            button.image = MenuBarVerticalUsageIcon.image(percent: metric.percent)
            button.imagePosition = .imageRight
            button.setAccessibilityLabel("\(text)，已使用 \(UsageFormatter.percentText(metric) ?? "")")
        } else {
            button.title = text
            button.image = nil
            button.setAccessibilityLabel(text)
        }
        button.toolTip = helpText(for: state)
    }

    private func helpText(for state: KeyUsageState?) -> String {
        guard let state else {
            return "尚未配置 Key"
        }
        var parts = [state.configuration.displayName]
        if let lastSuccessAt = state.lastSuccessAt {
            parts.append("最后更新 \(lastSuccessAt.formatted(date: .omitted, time: .shortened))")
        } else {
            parts.append("尚未更新")
        }
        if state.isStale {
            parts.append("缓存已过期")
        }
        return parts.joined(separator: " · ")
    }

    @objc private func handleStatusButtonClick(_ sender: NSStatusBarButton) {
        if NSApp.currentEvent?.type == .rightMouseUp {
            showContextMenu(from: sender)
        } else {
            togglePopover(from: sender)
        }
    }

    private func togglePopover(from button: NSStatusBarButton) {
        if popover.isShown {
            popover.performClose(nil)
        } else {
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
        }
    }

    private func showContextMenu(from button: NSStatusBarButton) {
        popover.performClose(nil)
        let menu = NSMenu()
        menu.autoenablesItems = false

        let accountsItem = NSMenuItem(title: "切换账号", action: nil, keyEquivalent: "")
        accountsItem.submenu = accountMenu()
        menu.addItem(accountsItem)

        let settingsItem = NSMenuItem(
            title: "设置",
            action: #selector(openSettingsWindow),
            keyEquivalent: ","
        )
        settingsItem.target = self
        menu.addItem(settingsItem)
        menu.addItem(.separator())

        let checkForUpdatesItem = NSMenuItem(
            title: "检查更新",
            action: #selector(checkForUpdates),
            keyEquivalent: ""
        )
        checkForUpdatesItem.target = self
        checkForUpdatesItem.isEnabled = environment.updateStatus != .checking
            && environment.updateStatus != .downloading
        menu.addItem(checkForUpdatesItem)
        menu.addItem(.separator())

        let quitItem = NSMenuItem(
            title: "退出 MyRoutin",
            action: #selector(quitApplication),
            keyEquivalent: "q"
        )
        quitItem.target = self
        menu.addItem(quitItem)

        menu.popUp(positioning: nil, at: NSPoint(x: 0, y: button.bounds.height), in: button)
    }

    private func accountMenu() -> NSMenu {
        let menu = NSMenu()
        let keyIDs = environment.store.orderedKeyIDs
        guard !keyIDs.isEmpty else {
            let emptyItem = NSMenuItem(title: "尚未配置账号", action: nil, keyEquivalent: "")
            emptyItem.isEnabled = false
            menu.addItem(emptyItem)
            return menu
        }

        for id in keyIDs {
            guard let state = environment.store.state(for: id) else {
                continue
            }
            let item = NSMenuItem(
                title: state.configuration.displayName,
                action: #selector(selectAccount(_:)),
                keyEquivalent: ""
            )
            item.target = self
            item.representedObject = id.uuidString
            item.state = environment.store.selectedKeyID == id ? .on : .off
            menu.addItem(item)
        }
        return menu
    }

    @objc private func selectAccount(_ sender: NSMenuItem) {
        guard let idText = sender.representedObject as? String,
              let id = UUID(uuidString: idText) else {
            return
        }
        environment.store.selectKey(id)
    }

    @objc private func checkForUpdates() {
        Task { await environment.checkForUpdates() }
    }

    @objc private func openSettingsWindow() {
        NSApp.activate(ignoringOtherApps: true)
        NotificationCenter.default.post(name: Notification.Name.showSettingsWindow, object: nil)
    }

    @objc private func quitApplication() {
        NSApplication.shared.terminate(nil)
    }
}

@MainActor
private struct StatusPopoverContent: View {
    @Bindable var environment: AppEnvironment

    @Environment(\.openWindow) private var openWindow

    var body: some View {
        UsagePopoverView(
            store: environment.store,
            settings: environment.settings,
            updateStatus: environment.updateStatus,
            installAvailableUpdate: environment.installAvailableUpdate
        )
        .sheet(isPresented: $environment.showsOnboarding) {
            OnboardingView(store: environment.store) {
                environment.dismissOnboarding()
            }
        }
        .onReceive(NotificationCenter.default.publisher(for: Notification.Name.showSettingsWindow)) { _ in
            openWindow(id: "settings")
        }
    }
}
