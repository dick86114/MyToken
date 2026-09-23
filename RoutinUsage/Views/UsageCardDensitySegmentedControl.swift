import SwiftUI

struct UsageCardDensitySegmentedControl: View {
    @Binding var selection: UsageCardDensity
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 0) {
            segment(.compact)
            segment(.full)
        }
        .padding(2)
        .background {
            RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
                .fill(Color.primary.opacity(0.06))
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("卡片显示")
    }

    private func segment(_ density: UsageCardDensity) -> some View {
        let isSelected = selection == density

        return Button {
            withAnimation(.spring(response: 0.35, dampingFraction: 0.8)) {
                selection = density
            }
        } label: {
            Text(density.title)
                .font(.system(size: 11, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(isSelected ? Color.primary : Color.secondary)
                .padding(.horizontal, 10)
                .frame(height: 22)
                .background {
                    if isSelected {
                        RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner), style: .continuous)
                            .fill(CompactPopoverPalette.selectedSegmentFill(colorScheme))
                            .overlay {
                                RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner), style: .continuous)
                                    .strokeBorder(CompactPopoverPalette.selectedSegmentStroke(colorScheme), lineWidth: 1)
                            }
                    }
                }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(density.title)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
