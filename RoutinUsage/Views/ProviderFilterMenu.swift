import SwiftUI

struct ProviderFilterOption: Equatable, Identifiable {
    let providerID: ProviderID?
    let title: String
    let count: Int

    var id: ProviderID? { providerID }
    var menuTitle: String { "\(title)（\(count)）" }
}

enum ProviderFilterMenuModel {
    static func options(
        counts: [ProviderID: Int],
        allCount: Int,
        providerName: (ProviderID) -> String
    ) -> [ProviderFilterOption] {
        var options = [
            ProviderFilterOption(providerID: nil, title: "全部", count: allCount)
        ]
        for providerID in ProviderID.allCases where (counts[providerID] ?? 0) > 0 {
            options.append(
                ProviderFilterOption(
                    providerID: providerID,
                    title: providerName(providerID),
                    count: counts[providerID] ?? 0
                )
            )
        }
        return options
    }

    static func buttonTitle(
        selectedProviderID: ProviderID?,
        providerName: (ProviderID) -> String
    ) -> String {
        if let selectedProviderID {
            return "供应商：\(providerName(selectedProviderID))"
        }
        return "供应商：全部"
    }
}

struct ProviderFilterMenu: View {
    @Binding var selection: ProviderID?
    let options: [ProviderFilterOption]

    @State private var isHovered = false
    @Environment(\.colorScheme) private var colorScheme

    private var selectedTitle: String {
        if let selection {
            let title = options.first { $0.id == selection }?.title ?? selection.rawValue
            return "供应商：\(title)"
        }
        return "供应商：全部"
    }

    var body: some View {
        Menu {
            Picker(selection: $selection) {
                ForEach(options) { option in
                    Text(option.menuTitle)
                        .tag(option.id as ProviderID?)
                }
            } label: {
                EmptyView()
            }
            .labelsHidden()
            .pickerStyle(.inline)
        } label: {
            filterChip
        }
        .buttonStyle(.plain)
        .menuIndicator(.hidden)
        .fixedSize()
        .accessibilityLabel("供应商筛选，当前：\(selectedTitle)")
        .accessibilityHint("选择供应商筛选账户用量")
    }

    private var filterChip: some View {
        HStack(spacing: 6) {
            Image(systemName: "chevron.down")
                .font(.system(size: 9, weight: .semibold))
                .foregroundStyle(CompactPopoverPalette.chipSecondary(colorScheme))
            Text(selectedTitle)
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(CompactPopoverPalette.chipText(colorScheme))
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background {
            chipBackground
        }
        .overlay {
            RoundedRectangle(cornerRadius: 9, style: .continuous)
                .strokeBorder(
                    LinearGradient(
                        colors: [
                            Color.white.opacity(colorScheme == .dark ? 0.22 : 0.95),
                            Color.white.opacity(colorScheme == .dark ? 0.10 : 0.55)
                        ],
                        startPoint: .top,
                        endPoint: .bottom
                    ),
                    lineWidth: 1
                )
                .allowsHitTesting(false)
        }
        .shadow(color: Color.black.opacity(0.04), radius: 1, y: 1)
        .onHover { isHovered = $0 }
    }

    @ViewBuilder
    private var chipBackground: some View {
        let shape = RoundedRectangle(cornerRadius: 9, style: .continuous)
        let fill = CompactPopoverPalette.chipFill(hovered: isHovered, colorScheme)
        if #available(macOS 26.0, *) {
            shape
                .fill(fill)
                .glassEffect(.regular.tint(CompactPopoverPalette.glassTint(colorScheme)), in: shape)
        } else {
            shape.fill(fill)
        }
    }
}
