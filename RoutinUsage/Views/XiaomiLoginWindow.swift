import SwiftUI
import WebKit

@MainActor
struct XiaomiLoginWindow: View {
    let session: XiaomiWebSession
    var resetSession = false
    let onCaptured: (String) async -> Void
    let onClose: () -> Void

    @State private var isCapturing = false
    @State private var errorMessage: String?

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Image(systemName: "person.crop.circle.badge.checkmark")
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text("登录小米 MiMo")
                        .font(.headline)
                    Text("登录完成后点击“使用当前登录状态”，会自动读取所需 Cookie。")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    onClose()
                } label: {
                    Image(systemName: "xmark")
                }
                .liquidGlassButton()
                .help("关闭登录窗口")
                .accessibilityLabel("关闭登录窗口")
            }
            .padding(14)

            Divider()

            XiaomiWebView(webView: session.webView)

            Divider()

            HStack(spacing: 12) {
                if let errorMessage {
                    Label(errorMessage, systemImage: "exclamationmark.triangle.fill")
                        .font(.callout)
                        .foregroundStyle(.red)
                }
                Spacer()
                Button("使用当前登录状态") {
                    Task {
                        isCapturing = true
                        errorMessage = nil
                        if let cookie = await session.captureCookieHeader() {
                            isCapturing = false
                            await onCaptured(cookie)
                        } else {
                            isCapturing = false
                            errorMessage = "未读取到有效登录态，请确认已登录控制台"
                        }
                    }
                }
                .liquidGlassButton(prominent: true)
                .disabled(isCapturing)
            }
            .padding(14)
        }
        .frame(minWidth: 620, minHeight: 720)
        .liquidGlassWindowBackground()
        .onAppear {
            Task {
                await session.prepareLogin(resetSession: resetSession)
            }
        }
    }
}

@MainActor
struct XiaomiRetryLoginSheet: View {
    let request: XiaomiLoginRequest
    let session: XiaomiWebSession?
    let onCaptured: @MainActor (String) async -> Void
    let onClose: @MainActor () -> Void

    var body: some View {
        Group {
            if let session {
                XiaomiLoginWindow(
                    session: session,
                    resetSession: request.resetsWebSession,
                    onCaptured: { cookie in
                        await onCaptured(cookie)
                        onClose()
                    },
                    onClose: onClose
                )
            } else {
                ContentUnavailableView("小米 MiMo 登录暂不可用", systemImage: "wifi.exclamationmark")
                    .frame(minWidth: 420, minHeight: 260)
                    .overlay(alignment: .topTrailing) {
                        Button(action: onClose) {
                            Image(systemName: "xmark")
                        }
                        .liquidGlassButton()
                        .padding(14)
                        .help("关闭")
                        .accessibilityLabel("关闭")
                    }
            }
        }
    }
}

/// 小米登录必须是独立标题栏窗口。WKWebView 嵌在 sheet/popover 中时
/// 容易丢掉 key window 状态，导致窗口无法拖动、输入框无法响应键盘。
@MainActor
final class XiaomiLoginPanelController: NSObject, NSWindowDelegate {
    static let shared = XiaomiLoginPanelController()

    private var window: NSWindow?
    private var onClose: (() -> Void)?

    func present(
        request: XiaomiLoginRequest,
        session: XiaomiWebSession?,
        onCaptured: @escaping @MainActor (String) async -> Void,
        onClose: @escaping @MainActor () -> Void
    ) {
        let sheet = XiaomiRetryLoginSheet(
            request: request,
            session: session,
            onCaptured: onCaptured,
            onClose: { [weak self] in
                self?.close()
                onClose()
            }
        )
        present(content: sheet, onWindowClose: onClose)
    }

    func present(content: some View, onWindowClose: @escaping () -> Void = {}) {
        let hostingController = NSHostingController(rootView: content)
        hostingController.sizingOptions = []
        onClose = onWindowClose

        if let window {
            window.contentViewController = hostingController
            makeKey(window)
            return
        }

        let window = XiaomiLoginPanelWindow(
            contentRect: NSRect(origin: .zero, size: CGSize(width: 620, height: 720)),
            styleMask: [.titled, .closable, .resizable],
            backing: .buffered,
            defer: false
        )
        window.title = "登录小米 MiMo"
        window.isReleasedWhenClosed = false
        window.isRestorable = false
        window.tabbingMode = .disallowed
        window.minSize = CGSize(width: 620, height: 720)
        window.styleMask.remove(.fullSizeContentView)
        window.delegate = self
        window.contentViewController = hostingController
        window.center()
        self.window = window
        makeKey(window)
    }

    func close() {
        window?.performClose(nil)
    }

    func windowWillClose(_ notification: Notification) {
        let closeAction = onClose
        onClose = nil
        if let window = notification.object as? NSWindow {
            SettingsWindowActivationPolicy.unregister(window)
        }
        closeAction?()
    }

    private func makeKey(_ window: NSWindow) {
        SettingsWindowActivationPolicy.register(window)
        window.makeKeyAndOrderFront(nil)
        window.makeFirstResponder(window.contentView)
        NSApp.activate(ignoringOtherApps: true)
    }
}

@MainActor
private final class XiaomiLoginPanelWindow: NSWindow {
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { true }
}

@MainActor
private struct XiaomiWebView: NSViewRepresentable {
    let webView: WKWebView

    func makeNSView(context: Context) -> WKWebView {
        webView
    }

    func updateNSView(_: WKWebView, context _: Context) {}
}
