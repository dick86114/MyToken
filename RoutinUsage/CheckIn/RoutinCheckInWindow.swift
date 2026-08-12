import SwiftUI
import WebKit

@MainActor
struct RoutinCheckInWindow: View {
    @Bindable var service: RoutinCheckInService
    let session: RoutinWebSession

    @Environment(\.dismiss) private var dismiss

    var body: some View {
        VStack(spacing: 0) {
            HStack(spacing: 10) {
                Image(systemName: "checkmark.circle")
                    .foregroundStyle(Color.accentColor)
                    .accessibilityHidden(true)
                VStack(alignment: .leading, spacing: 2) {
                    Text("Routin 签到")
                        .font(.headline)
                    Text(service.state.statusText)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                Spacer()
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                }
                .liquidGlassButton()
                .help("关闭签到窗口")
                .accessibilityLabel("关闭签到窗口")
            }
            .padding(14)

            Divider()

            RoutinWebView(webView: session.webView)
        }
        .frame(minWidth: 520, minHeight: 640)
        .liquidGlassWindowBackground()
        .onDisappear {
            service.cancelLogin()
        }
    }
}

@MainActor
private struct RoutinWebView: NSViewRepresentable {
    let webView: WKWebView

    func makeNSView(context: Context) -> WKWebView {
        webView
    }

    func updateNSView(_: WKWebView, context _: Context) {}
}
