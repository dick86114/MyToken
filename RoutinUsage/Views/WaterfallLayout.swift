import SwiftUI

/// 两列瀑布流：逐个测量子视图高度，放入当前较矮的一列，避免等高网格留白。
struct WaterfallLayout: Layout {
    var columns: Int = 2
    var spacing: CGFloat = 8

    func sizeThatFits(
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) -> CGSize {
        let width = proposal.width ?? 440
        let columnWidth = columnWidth(forTotalWidth: width)
        var heights = [CGFloat](repeating: 0, count: max(columns, 1))

        for subview in subviews {
            let size = subview.sizeThatFits(ProposedViewSize(width: columnWidth, height: nil))
            let index = shortestColumnIndex(in: heights)
            heights[index] += size.height + spacing
        }

        let totalHeight = max((heights.max() ?? 0) - spacing, 0)
        return CGSize(width: width, height: totalHeight)
    }

    func placeSubviews(
        in bounds: CGRect,
        proposal: ProposedViewSize,
        subviews: Subviews,
        cache: inout ()
    ) {
        let columnWidth = columnWidth(forTotalWidth: bounds.width)
        var heights = [CGFloat](repeating: 0, count: max(columns, 1))

        for subview in subviews {
            let size = subview.sizeThatFits(ProposedViewSize(width: columnWidth, height: nil))
            let index = shortestColumnIndex(in: heights)
            let origin = CGPoint(
                x: bounds.minX + CGFloat(index) * (columnWidth + spacing),
                y: bounds.minY + heights[index]
            )
            subview.place(
                at: origin,
                proposal: ProposedViewSize(width: columnWidth, height: size.height)
            )
            heights[index] += size.height + spacing
        }
    }

    func columnWidth(forTotalWidth totalWidth: CGFloat) -> CGFloat {
        max((totalWidth - spacing * CGFloat(columns - 1)) / CGFloat(max(columns, 1)), 0)
    }

    func shortestColumnIndex(in heights: [CGFloat]) -> Int {
        var index = 0
        for candidate in heights.indices where heights[candidate] < heights[index] {
            index = candidate
        }
        return index
    }
}
