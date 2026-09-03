import AppKit

@MainActor
enum SettingsWindowActivationPolicy {
    private static let trackedWindows = NSHashTable<NSWindow>.weakObjects()

    static var hasSettingsWindow: Bool {
        trackedWindows.allObjects.contains { window in
            window.isVisible && window.contentView != nil
        }
    }

    static func register(_ window: NSWindow) {
        trackedWindows.add(window)
        refresh()
    }

    static func unregister(_ window: NSWindow) {
        trackedWindows.remove(window)
        refresh()
    }

    static func refresh() {
        NSApp.setActivationPolicy(hasSettingsWindow ? .regular : .accessory)
    }
}
