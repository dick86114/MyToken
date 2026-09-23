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
                .font(.system(size: 10, weight: .semibold))
                .foregroundStyle(CompactPopoverPalette.chipSecondary(colorScheme))
            Text(selectedTitle)
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(CompactPopoverPalette.chipText(colorScheme))
                .lineLimit(1)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 6)
        .background {
            chipBackground
        }
        .overlay {
            RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
                .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme), lineWidth: 1)
                .allowsHitTesting(false)
        }
        .onHover { isHovered = $0 }
    }

    private var chipBackground: some View {
        RoundedRectangle(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button), style: .continuous)
            .fill(CompactPopoverPalette.chipFill(hovered: isHovered, colorScheme))
    }
}
