import SwiftUI

@MainActor
struct MenuBarLabelView: View {
    let store: UsageStore
    let settings: AppSettings

    var body: some View {
        Text(
            UsageFormatter.menuBarText(
                state: selectedState,
                dimension: settings.displayDimension
            )
        )
        .help(helpText)
    }
}

private extension MenuBarLabelView {
    var selectedState: KeyUsageState? {
        store.selectedKeyID.flatMap(store.state(for:))
    }

    var helpText: String {
        guard let state = selectedState else {
            return "尚未配置 Key"
        }
        var parts = [state.configuration.displayName]
        if let lastSuccessAt = state.lastSuccessAt {
            parts.append("最后更新 \(lastSuccessAt.formatted(date: .omitted, time: .shortened))")
        } else {
            parts.append("尚未更新")
        }
        if state.isStale {
            parts.append("缓存已过期")
        }
        return parts.joined(separator: " · ")
    }
}
