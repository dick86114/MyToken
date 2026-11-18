import SwiftUI

/// 设置页统一使用原生分段控件，保留系统强调色、焦点环和方向键导航。
struct SettingsSegmentedControl<Option: Hashable>: View {
    let options: [Option]
    @Binding var selection: Option
    let title: (Option) -> String
    let accessibilityLabel: String

    var body: some View {
        Picker(accessibilityLabel, selection: $selection) {
            ForEach(options, id: \.self) { option in
                Text(title(option))
                    .tag(option)
            }
        }
        .pickerStyle(.segmented)
        .labelsHidden()
        .accessibilityLabel(accessibilityLabel)
    }
}

struct AppAppearanceSegmentedControl: View {
    @Binding var selection: AppAppearance

    var body: some View {
        SettingsSegmentedControl(
            options: AppAppearance.allCases,
            selection: $selection,
            title: \.title,
            accessibilityLabel: "外观"
        )
    }
}
