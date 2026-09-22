import SwiftUI

struct UsageCardDensitySegmentedControl: View {
    @Binding var selection: UsageCardDensity

    var body: some View {
        HStack(spacing: 0) {
            segment(.compact)
            segment(.full)
        }
        .padding(2)
        .background {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .fill(Color.primary.opacity(0.055))
        }
        .overlay {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .strokeBorder(Color.primary.opacity(0.12))
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("卡片显示")
    }

    private func segment(_ density: UsageCardDensity) -> some View {
        let isSelected = selection == density

        return Button {
            selection = density
        } label: {
            Text(density.title)
                .font(.system(size: 11, weight: .medium))
                .monospacedDigit()
                .foregroundStyle(isSelected ? Color.primary : Color.secondary)
                .padding(.horizontal, 10)
                .frame(height: 26)
                .background {
                    if isSelected {
                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                            .fill(Color(nsColor: .controlBackgroundColor))
                            .shadow(color: .primary.opacity(0.16), radius: 1, y: 0.5)
                    }
                }
                .contentShape(RoundedRectangle(cornerRadius: 6, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(density.title)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
