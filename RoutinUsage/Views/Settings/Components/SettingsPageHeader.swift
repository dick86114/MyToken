import SwiftUI

struct SettingsPageHeader: View {
    let title: String
    let subtitle: String
    var trailing: AnyView? = nil

    var body: some View {
        HStack(alignment: .center, spacing: 18) {
            VStack(alignment: .leading, spacing: 5) {
                Text(title)
                    .font(SettingsTypography.pageTitle)
                    .tracking(-0.35)
                    .accessibilityAddTraits(.isHeader)

                Text(subtitle)
                    .font(SettingsTypography.body)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 12)

            trailing
        }
        .padding(.bottom, 6)
    }
}
