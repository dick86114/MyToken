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
        VStack(spacing: 20) {
            Image(systemName: "chart.bar.xaxis")
                .font(.system(size: 44))
                .foregroundStyle(.tint)
                .accessibilityHidden(true)

            VStack(spacing: 8) {
                Text("欢迎使用 MyRoutin")
                    .font(.title2.weight(.semibold))
                Text("添加第一个 plan Key，即可在菜单栏查看 5 小时、周或 Token 资源包用量。")
                    .multilineTextAlignment(.center)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
            .accessibilityElement(children: .combine)

            Label("Key 仅保存在这台 Mac 的本地偏好设置中", systemImage: "lock.shield")
                .font(.callout)
                .foregroundStyle(.secondary)

            Button("添加第一个 Key") {
                showsKeyEditor = true
            }
            .buttonStyle(.borderedProminent)
            .keyboardShortcut(.defaultAction)
            .accessibilityHint("打开 Key 编辑表单")
        }
        .padding(36)
        .frame(width: 460)
        .frame(minHeight: 330)
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
