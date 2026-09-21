import AppKit
import SwiftUI

struct MenuBarIndicatorPreview: View, Equatable {
    let state: KeyUsageState
    let descriptor: ProviderDescriptor
    let metric: NormalizedUsageMetric?
    let colorRules: MenuBarColorRules

    static func == (lhs: Self, rhs: Self) -> Bool {
        lhs.state == rhs.state
            && lhs.descriptor == rhs.descriptor
            && lhs.metric == rhs.metric
            && lhs.colorRules == rhs.colorRules
    }

    var body: some View {
        let indicator = MenuBarIndicatorModel.make(
            state: state,
            descriptor: descriptor,
            metric: metric
        )
        let image = MenuBarMultiUsageIcon.image(
            indicators: [indicator],
            colorRules: colorRules
        )

        Image(nsImage: image)
            .interpolation(.high)
            .scaledToFit()
            .frame(height: 26)
    }
}
