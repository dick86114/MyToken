import AppKit

@MainActor
enum SettingsWindowActivationPolicy {
    private static let trackedWindows = NSHashTable<NSWindow>.weakObjects()
    private static var closeObserver: NSObjectProtocol?

    static var hasSettingsWindow: Bool {
        trackedWindows.allObjects.contains { window in
            window.isVisible && window.contentView != nil
        }
    }

    static func register(_ window: NSWindow) {
        installCloseObserverIfNeeded()
        trackedWindows.add(window)
        refresh()
    }

    static func unregister(_ window: NSWindow) {
        trackedWindows.remove(window)
        refresh()
    }

    static func refresh() {
        let target: NSApplication.ActivationPolicy = hasSettingsWindow ? .regular : .accessory
        guard NSApp.activationPolicy() != target else {
            return
        }
        apply(hasSettingsWindow: target == .regular)
    }

    /// 只在窗口真正关闭时撤回 Dock 图标；SwiftUI 重建视图不应反复切换策略。
    private static func installCloseObserverIfNeeded() {
        guard closeObserver == nil else {
            return
        }
        closeObserver = NotificationCenter.default.addObserver(
            forName: NSWindow.willCloseNotification,
            object: nil,
            queue: .main
        ) { notification in
            guard let window = notification.object as? NSWindow else {
                return
            }
            // willClose 仍在窗口关闭流程内；立刻切换策略可能被当前事件吞掉，
            // 等本轮事件结束后确认窗口不可见再恢复菜单栏形态。
            DispatchQueue.main.async { [weak window] in
                MainActor.assumeIsolated {
                    guard let window, !window.isVisible else {
                        return
                    }
                    guard trackedWindows.allObjects.contains(window) else {
                        return
                    }
                    unregister(window)
                }
            }
        }
    }

    static func apply(
        hasSettingsWindow: Bool,
        setActivationPolicy: @MainActor (NSApplication.ActivationPolicy) -> Bool = { NSApp.setActivationPolicy($0) },
        activate: @MainActor () -> Void = { NSApp.activate(ignoringOtherApps: true) }
    ) {
        let policy: NSApplication.ActivationPolicy = hasSettingsWindow ? .regular : .accessory
        if setActivationPolicy(policy), hasSettingsWindow {
            activate()
        }
    }
}
