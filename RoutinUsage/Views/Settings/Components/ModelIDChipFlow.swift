import AppKit
import SwiftUI

struct ModelIDChipFlow: View {
    let models: [String]
    let copiedModelID: String?
    var accentColor: Color = .accentColor
    let onCopied: (String) -> Void

    var body: some View {
        WrappingModelChips(spacing: 7) {
            ForEach(models, id: \.self) { modelID in
                ModelIDChip(
                    modelID: modelID,
                    isCopied: copiedModelID == modelID,
                    accentColor: accentColor,
                    onCopied: onCopied
                )
            }
        }
    }
}

private struct ModelIDChip: View {
    let modelID: String
    let isCopied: Bool
    let accentColor: Color
    let onCopied: (String) -> Void

    @State private var isHovered = false
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        Button {
            NSPasteboard.general.clearContents()
            NSPasteboard.general.setString(modelID, forType: .string)
            onCopied(modelID)
        } label: {
            HStack(spacing: 4) {
                Circle()
                    .fill(accentColor)
                    .frame(width: 5, height: 5)

                Text(modelID)
                    .lineLimit(1)
                    .truncationMode(.middle)
                    .foregroundStyle(.primary)

                Image(systemName: isCopied ? "checkmark" : "doc.on.doc")
                    .font(.caption2.weight(.semibold))
                    .foregroundStyle(
                        isCopied
                            ? CompactPopoverPalette.positive(colorScheme)
                            : accentColor
                    )
                    .opacity(isCopied ? 1 : 0.54)
            }
            .font(.caption.weight(.medium).monospaced())
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background {
                Capsule()
                    .fill(
                        Color.primary.opacity(
                            isHovered ? 0.075 : 0.045
                        )
                    )
            }
            .overlay {
                Capsule()
                    .strokeBorder(
                        isCopied
                            ? CompactPopoverPalette.positive(colorScheme).opacity(0.58)
                            : accentColor.opacity(isHovered ? 0.34 : 0.22),
                        lineWidth: 0.8
                    )
            }
            .contentShape(Capsule())
        }
        .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.96))
        .onHover { isHovered = $0 }
        .animation(
            reduceMotion
                ? nil
                : .spring(response: 0.26, dampingFraction: 0.88),
            value: isCopied
        )
        .animation(.easeOut(duration: 0.12), value: isHovered)
        .help(isCopied ? "已复制模型 ID" : "复制模型 ID")
        .accessibilityLabel(isCopied ? "已复制模型 ID \(modelID)" : "复制模型 ID \(modelID)")
        .accessibilityAddTraits(.isButton)
    }
}

private struct WrappingModelChips: Layout {
    var spacing: CGFloat = 7

    func sizeThatFits(
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) -> CGSize {
        let maxWidth = max(proposal.width ?? 360, 1)
        let rows = calculateRows(subviews: subviews, maxWidth: maxWidth)
        let height = rows.reduce(0) { partial, row in
            partial + (row.map { $0.sizeThatFits(.unspecified).height }.max() ?? 0) + spacing
        }

        return CGSize(width: maxWidth, height: max(height - spacing, 0))
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        let rows = calculateRows(subviews: subviews, maxWidth: bounds.width)
        var y = bounds.minY

        for row in rows {
            let rowHeight = row.map { $0.sizeThatFits(.unspecified).height }.max() ?? 0
            var x = bounds.minX

            for view in row {
                let size = view.sizeThatFits(.unspecified)
                view.place(
                    at: CGPoint(x: x, y: y + (rowHeight - size.height) / 2),
                    anchor: .topLeading,
                    proposal: .unspecified
                )
                x += size.width + spacing
            }

            y += rowHeight + spacing
        }
    }

    private func calculateRows(
        subviews: Subviews,
        maxWidth: CGFloat
    ) -> [[Subviews.Element]] {
        var rows: [[Subviews.Element]] = [[]]
        var x: CGFloat = 0

        for view in subviews {
            let size = view.sizeThatFits(.unspecified)
            if x > 0, x + size.width > maxWidth {
                rows.append([])
                x = 0
            }

            rows[rows.count - 1].append(view)
            x += size.width + spacing
        }

        return rows
    }
}
