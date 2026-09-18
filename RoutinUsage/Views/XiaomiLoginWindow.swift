import SwiftUI
import WebKit

@MainActor
struct XiaomiLoginWindow: View {
    let session: XiaomiWebSession
    let onCaptured: (String) -> Void
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
                            onCaptured(cookie)
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
                await session.prepareLogin()
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
                    onCaptured: { cookie in
                        Task { await onCaptured(cookie) }
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
        .onDisappear(perform: onClose)
    }
}

@MainActor
private struct XiaomiWebView: NSViewRepresentable {
    let webView: WKWebView

    func makeNSView(context: Context) -> WKWebView {
        webView
    }

    func updateNSView(_: WKWebView, context _: Context) {}
}
