import AppKit
import SwiftUI

@MainActor
struct OnboardingView: View {
    @Bindable var store: UsageStore
    let onCompleted: @MainActor () -> Void

    @Environment(\.dismiss) private var dismiss
    @State private var showsKeyEditor = false

    init(store: UsageStore, onCompleted: @escaping @MainActor () -> Void = {}) {
        self.store = store
        self.onCompleted = onCompleted
    }

    var body: some View {
        VStack(spacing: 22) {
            Image(nsImage: NSImage(named: "PopoverColorBrandLogo") ?? NSApp.applicationIconImage)
                .resizable()
                .interpolation(.high)
                .scaledToFit()
                .frame(width: 72, height: 72)
                .shadow(color: .black.opacity(0.16), radius: 4, y: 2)
                .accessibilityHidden(true)

            VStack(spacing: 8) {
                Text("欢迎使用 MyToken")
                    .font(.title.weight(.semibold))
                Text("添加第一个 plan Key，即可在菜单栏查看 5 小时、周或 Token 资源包用量。")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                    .frame(maxWidth: 330)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .combine)

            Label("Key 仅保存在这台 Mac 的本地偏好设置中", systemImage: "lock.shield")
                .font(.callout)
                .foregroundStyle(.secondary)

            Button("添加第一个 Key") {
                showsKeyEditor = true
            }
            .liquidGlassButton(prominent: true)
            .keyboardShortcut(.defaultAction)
            .accessibilityHint("打开 Key 编辑表单")
        }
        .padding(32)
        .liquidGlassSurface(cornerRadius: 24)
        .padding(36)
        .frame(width: 460)
        .frame(minHeight: 350)
        .liquidGlassWindowBackground()
        .sheet(isPresented: $showsKeyEditor) {
            KeyEditorView(save: addFirstKey) {
                completeOnboarding()
            }
        }
        .onAppear {
            if !store.orderedKeyIDs.isEmpty {
                completeOnboarding()
            }
        }
    }
}

private extension OnboardingView {
    func addFirstKey(name: String, secret: String) async throws -> KeyEditorSaveResult {
        try await store.addValidatedKey(name: name, secret: secret)
        guard let keyID = store.orderedKeyIDs.first else {
            return .saved
        }
        return store.state(for: keyID)?.error == .noSubscription
            ? .savedWithoutSubscription
            : .saved
    }

    func completeOnboarding() {
        onCompleted()
        dismiss()
    }
}
