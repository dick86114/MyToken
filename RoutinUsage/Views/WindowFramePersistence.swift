import AppKit
import SwiftUI

/// 负责设置窗口尺寸的恢复、保存和最小尺寸约束。
@MainActor
struct WindowFramePersistence: NSViewRepresentable {
    nonisolated static let defaultSize = CGSize(width: 560, height: 500)
    nonisolated static let minimumSize = CGSize(width: 520, height: 420)

    private nonisolated static let widthKey = "settingsWindowWidth"
    private nonisolated static let heightKey = "settingsWindowHeight"

    let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func makeCoordinator() -> Coordinator {
        Coordinator(defaults: defaults)
    }

    func makeNSView(context: Context) -> WindowFrameHostingView {
        let view = WindowFrameHostingView()
        view.onWindowChange = { window in
            context.coordinator.attach(to: window)
        }
        return view
    }

    func updateNSView(_ nsView: WindowFrameHostingView, context: Context) {
        context.coordinator.attach(to: nsView.window)
    }

    nonisolated static func loadSize(defaults: UserDefaults = .standard) -> CGSize {
        let width = defaults.double(forKey: widthKey)
        let height = defaults.double(forKey: heightKey)
        guard width.isFinite, height.isFinite, width > 0, height > 0 else {
            return defaultSize
        }
        return CGSize(
            width: max(width, minimumSize.width),
            height: max(height, minimumSize.height)
        )
    }

    nonisolated static func saveSize(_ size: CGSize, defaults: UserDefaults = .standard) {
        let clamped = CGSize(
            width: max(size.width, minimumSize.width),
            height: max(size.height, minimumSize.height)
        )
        defaults.set(clamped.width, forKey: widthKey)
        defaults.set(clamped.height, forKey: heightKey)
    }

    @MainActor
    final class Coordinator {
        private let defaults: UserDefaults
        private weak var window: NSWindow?
        private var resizeObserver: NSObjectProtocol?

        init(defaults: UserDefaults) {
            self.defaults = defaults
        }

        func attach(to window: NSWindow?) {
            guard let window else {
                detach()
                return
            }
            guard self.window !== window else {
                return
            }
            detach()
            self.window = window
            window.minSize = WindowFramePersistence.minimumSize
            var frame = window.frame
            frame.size = WindowFramePersistence.loadSize(defaults: defaults)
            window.setFrame(frame, display: false)
            resizeObserver = NotificationCenter.default.addObserver(
                forName: NSWindow.didResizeNotification,
                object: window,
                queue: .main
            ) { [weak self] notification in
                guard
                    let self,
                    let resizedWindow = notification.object as? NSWindow
                else {
                    return
                }
                WindowFramePersistence.saveSize(
                    resizedWindow.frame.size,
                    defaults: self.defaults
                )
            }
        }

        private func detach() {
            if let resizeObserver {
                NotificationCenter.default.removeObserver(resizeObserver)
                self.resizeObserver = nil
            }
            window = nil
        }

    }
}

@MainActor
final class WindowFrameHostingView: NSView {
    var onWindowChange: ((NSWindow?) -> Void)?

    override func viewDidMoveToWindow() {
        super.viewDidMoveToWindow()
        onWindowChange?(window)
    }
}
