import SwiftUI

struct UsageResetCardsBadge: View {
    enum Style {
        case compact
        case detail
    }

    let resetCards: [UsageResetCard]
    var style: Style = .compact
    var now: Date = .now

    @State private var isPresented = false
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(spacing: 3) {
            Image(systemName: "creditcard")
                .font(.system(size: 8, weight: .semibold))
                .foregroundStyle(CompactPopoverPalette.brand(colorScheme))
            if style == .detail {
                Text(UsageResetCardPresentation.detailText(cards: resetCards, now: now))
                    .font(.system(size: 10, weight: .medium))
                    .foregroundStyle(CompactPopoverPalette.brand(colorScheme))
                    .monospacedDigit()
            } else {
                Text(UsageResetCardPresentation.badgeText(cards: resetCards))
                    .font(.system(size: 10, weight: .semibold, design: .monospaced))
                    .foregroundStyle(CompactPopoverPalette.brand(colorScheme))
                    .monospacedDigit()
            }
        }
        .padding(.horizontal, style == .detail ? 6 : 5)
        .padding(.vertical, 2.5)
        .background {
            Capsule()
                .fill(
                    CompactPopoverPalette.brand(colorScheme)
                        .opacity(isPresented ? 0.15 : 0.09)
                )
        }
        .overlay {
            Capsule()
                .strokeBorder(
                    CompactPopoverPalette.brand(colorScheme)
                        .opacity(isPresented ? 0.24 : 0.13),
                    lineWidth: 1
                )
        }
        .fixedSize(horizontal: true, vertical: false)
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
