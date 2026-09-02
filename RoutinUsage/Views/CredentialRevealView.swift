import AppKit
import SwiftUI

@MainActor
struct CredentialRevealView: View {
    let title: String
    let secret: String

    @Environment(\.dismiss) private var dismiss
    @State private var isVisible = false
    @State private var didCopy = false

    var body: some View {
        VStack(alignment: .leading, spacing: 18) {
            Text(title)
                .font(.title3.weight(.semibold))

            HStack(spacing: 8) {
                Group {
                    if isVisible {
                        TextField("凭证值", text: .constant(secret))
                    } else {
                        SecureField("凭证值", text: .constant(secret))
                    }
                }
                .textSelection(.enabled)
                .textFieldStyle(.roundedBorder)

                Button {
                    isVisible.toggle()
                } label: {
                    Image(systemName: isVisible ? "eye.slash" : "eye")
                }
                .buttonStyle(.borderless)
                .help(isVisible ? "隐藏凭证" : "显示凭证")
                .accessibilityLabel(isVisible ? "隐藏凭证" : "显示凭证")

                Button {
                    NSPasteboard.general.clearContents()
                    NSPasteboard.general.setString(secret, forType: .string)
                    didCopy = true
                    Task { @MainActor in
                        try? await Task.sleep(for: .milliseconds(1200))
                        didCopy = false
                    }
                } label: {
                    Image(systemName: didCopy ? "checkmark" : "doc.on.doc")
                }
                .buttonStyle(.borderless)
                .help("复制凭证")
                .accessibilityLabel(didCopy ? "已复制凭证" : "复制凭证")
            }

            Text("凭证仅在当前窗口中显示，不会写入日志。")
                .font(.caption)
                .foregroundStyle(.secondary)

            HStack {
                Spacer()
                Button("关闭") { dismiss() }
                    .keyboardShortcut(.cancelAction)
                    .liquidGlassButton(prominent: true)
            }
        }
        .padding(24)
        .frame(width: 500)
    }
}
