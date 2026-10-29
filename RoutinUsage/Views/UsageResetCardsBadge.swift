import SwiftUI

struct UsageResetCardsBadge: View {
    enum Style {
        case compact
        case detail
    }

    let resetCards: [UsageResetCard]
    var style: Style = .compact

    @State private var isPresented = false
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "creditcard")
                .font(.system(size: 8, weight: .semibold))
                .foregroundStyle(CompactPopoverPalette.brand(colorScheme))
            if style == .detail {
                Text(UsageResetCardPresentation.detailText(cards: resetCards))
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(Color.secondary)
                    .monospacedDigit()
            } else {
                Text(UsageResetCardPresentation.badgeText(cards: resetCards))
                    .font(.system(size: 10, weight: .semibold, design: .monospaced))
                    .foregroundStyle(Color.primary.opacity(0.78))
                    .monospacedDigit()
            }
        }
        .padding(.horizontal, style == .detail ? 5 : 4)
        .padding(.vertical, 2)
        .background {
            Capsule()
                .fill(CompactPopoverPalette.chipFill(hovered: isPresented, colorScheme))
        }
        .overlay {
            Capsule()
                .strokeBorder(Color.primary.opacity(isPresented ? 0.12 : 0.06), lineWidth: 1)
        }
        .contentShape(Capsule())
        .onHover { isPresented = $0 }
        .popover(isPresented: $isPresented, arrowEdge: .top) {
            VStack(alignment: .leading, spacing: 8) {
                Text("重置卡到期时间")
                    .font(.system(size: 11, weight: .semibold))
                    .foregroundStyle(.secondary)
                Divider()
                ForEach(Array(resetCards.enumerated()), id: \.offset) { index, card in
                    HStack {
                        Text("\(index + 1)")
                            .font(.system(size: 10, weight: .semibold, design: .monospaced))
                            .foregroundStyle(.tertiary)
                            .frame(width: 14, alignment: .trailing)
                        Text(UsageFormatter.fullDateTime(card.expiresAt))
                            .font(.system(size: 11, design: .monospaced))
                            .monospacedDigit()
                    }
                }
            }
            .padding(12)
            .frame(width: 210, alignment: .leading)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(UsageResetCardPresentation.accessibilityText(cards: resetCards))
    }
}
