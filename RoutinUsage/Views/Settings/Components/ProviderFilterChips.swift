import SwiftUI

struct ProviderFilterChips: View {
    let providers: [ProviderDescriptor]
    let counts: [ProviderID: Int]
    let selectedProviderID: ProviderID?
    let select: (ProviderID?) -> Void

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 8) {
                chip(
                    title: "全部",
                    count: counts.values.reduce(0, +),
                    isSelected: selectedProviderID == nil
                ) {
                    select(nil)
                }

                ForEach(providers) { provider in
                    chip(
                        title: provider.displayName,
                        count: counts[provider.id, default: 0],
                        providerID: provider.id,
                        isSelected: selectedProviderID == provider.id
                    ) {
                        select(provider.id)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func chip(
        title: String,
        count: Int,
        providerID: ProviderID? = nil,
        isSelected: Bool,
        action: @escaping () -> Void
    ) -> some View {
        Button(action: action) {
            HStack(spacing: 6) {
                if let providerID {
                    ProviderLogoMark(
                        providerID: providerID,
                        size: 17,
                        cornerRadius: 4
                    )
                }

                Text(title)
                    .font(.system(size: 11.5, weight: .medium))
                    .lineLimit(1)

                Text("\(count)")
                    .font(.system(size: 10, weight: .medium, design: .monospaced))
                    .foregroundStyle(isSelected ? Color.accentColor : Color.secondary)
            }
            .padding(.horizontal, 9)
            .padding(.vertical, 5)
            .contentShape(Capsule())
            .background {
                Capsule()
                    .fill(
                        isSelected
                            ? Color.accentColor.opacity(0.14)
                            : Color.primary.opacity(0.04)
                    )
            }
            .overlay {
                Capsule()
                    .stroke(
                        isSelected
                            ? Color.accentColor.opacity(0.28)
                            : Color.secondary.opacity(0.14),
                        lineWidth: 1
                    )
            }
        }
        .buttonStyle(.plain)
        .help("\(title)，\(count) 个凭证")
        .accessibilityLabel("\(title)，\(count) 个凭证")
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }
}
