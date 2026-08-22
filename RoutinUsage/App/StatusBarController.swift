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
    private var appearanceObservation: NSKeyValueObservation?
    private var popoverWindowResignObserver: NSObjectProtocol?

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
        observeStatusBarAppearance()
        observeEnvironment()

        // 先完成常驻应用的启动流程，再显示更新完成提示，避免同步模态弹窗阻塞首次更新检查。
        Task { @MainActor [weak self] in
            guard let self else { return }
            await self.environment.start()
            self.environment.presentUpdateCompletionNoticeIfNeeded()
        }
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

    private func observeStatusBarAppearance() {
        appearanceObservation = NSApp.observe(\NSApplication.effectiveAppearance, options: [.new]) {
            [weak self] _, _ in
            Task { @MainActor [weak self] in
                self?.updateStatusButton()
            }
        }
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
            _ = environment.routinCheckIn.state
            _ = environment.codexGroupDetection.states
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
            button.title = text.isEmpty ? "" : text + " · "
            button.image = switch environment.settings.menuBarStyle {
            case .aliasLogoProgress, .logoProgress:
                MenuBarLogoUsageIcon.image(
                    percent: metric.percent,
                    appearance: button.effectiveAppearance
                )
            case .aliasVerticalBar:
                MenuBarVerticalUsageIcon.image(percent: metric.percent)
            case .percent, .aliasPercent:
                nil
            }
            button.imagePosition = .imageRight
            let usedPercent = UsageFormatter.percentText(metric) ?? ""
            let accessibilityText = text.isEmpty
                ? "已使用 \(usedPercent)"
                : "\(text)，已使用 \(usedPercent)"
            button.setAccessibilityLabel(accessibilityText)
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
        if state.isRefreshing || state.isStale || state.error != nil {
            parts.append(UsageFormatter.statusText(state: state))
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
            NSApp.activate(ignoringOtherApps: true)
            popover.show(relativeTo: button.bounds, of: button, preferredEdge: .minY)
            configurePopoverWindow()
        }
    }

    private func configurePopoverWindow() {
        guard let window = popover.contentViewController?.view.window else {
            return
        }
        window.level = .statusBar
        window.makeKeyAndOrderFront(nil)

        if let popoverWindowResignObserver {
            NotificationCenter.default.removeObserver(popoverWindowResignObserver)
        }
        popoverWindowResignObserver = NotificationCenter.default.addObserver(
            forName: NSWindow.didResignKeyNotification,
            object: window,
            queue: .main
        ) { [weak self] _ in
            self?.popover.performClose(nil)
        }
    }

    private func showContextMenu(from button: NSStatusBarButton) {
        popover.performClose(nil)
        let menu = NSMenu()
        menu.autoenablesItems = false
        menu.presentationStyle = .regular

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
            && !isDownloadingUpdate
        menu.addItem(checkForUpdatesItem)
        menu.addItem(.separator())

        let issueItem = NSMenuItem(
            title: "提交问题",
            action: #selector(submitIssueReport),
            keyEquivalent: ""
        )
        issueItem.target = self
        menu.addItem(issueItem)
        menu.addItem(.separator())

        let quitItem = NSMenuItem(
            title: "退出 MyRoutin",
            action: #selector(quitApplication),
            keyEquivalent: "q"
        )
        quitItem.target = self
        menu.addItem(quitItem)

        statusItem.menu = menu
        defer { statusItem.menu = nil }
        button.performClick(nil)
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

    @objc private func submitIssueReport() {
        Task { await environment.openIssueReport() }
    }

    private var isDownloadingUpdate: Bool {
        if case .downloading = environment.updateStatus {
            return true
        }
        return false
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
            codexGroupDetection: environment.codexGroupDetection,
            updateStatus: environment.updateStatus,
            installAvailableUpdate: environment.installAvailableUpdate,
            checkInState: environment.routinCheckIn.state,
            startRoutinCheckIn: environment.startRoutinCheckIn,
            startCodexGroupDetection: environment.startCodexGroupDetection(for:)
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
