import SwiftUI

/// 设置页通用的横向分段控件，视觉与卡片显示控件保持一致。
struct SettingsSegmentedControl<Option: Hashable>: View {
    let options: [Option]
    @Binding var selection: Option
    let title: (Option) -> String
    let accessibilityLabel: String

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 0) {
            ForEach(options, id: \.self) { option in
                segment(option)
            }
        }
        .padding(2)
        .background {
            RoundedRectangle(
                cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button),
                style: .continuous
            )
            .fill(Color.primary.opacity(0.06))
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel(accessibilityLabel)
    }

    private func segment(_ option: Option) -> some View {
        let isSelected = selection == option

        return Button {
            selection = option
        } label: {
            Text(title(option))
                .font(.system(size: 11, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(isSelected ? Color.primary : Color.secondary)
                .padding(.horizontal, 10)
                .frame(height: 22)
                .background {
                    if isSelected {
                        RoundedRectangle(
                            cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner),
                            style: .continuous
                        )
                        .fill(CompactPopoverPalette.selectedSegmentFill(colorScheme))
                        .overlay {
                            RoundedRectangle(
                                cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner),
                                style: .continuous
                            )
                            .strokeBorder(
                                CompactPopoverPalette.selectedSegmentStroke(colorScheme),
                                lineWidth: 1
                            )
                        }
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title(option))
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
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
