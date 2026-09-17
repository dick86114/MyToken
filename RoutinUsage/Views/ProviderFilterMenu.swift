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
            HStack(spacing: 5) {
                Text(selectedTitle)
                    .font(.caption.weight(.medium))
                    .lineLimit(1)
                Image(systemName: "chevron.up.chevron.down")
                    .font(.caption2.weight(.semibold))
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background {
                Capsule()
                    .fill(Color.primary.opacity(0.05))
            }
            .overlay {
                Capsule()
                    .stroke(Color.secondary.opacity(0.16), lineWidth: 1)
            }
            .foregroundStyle(.primary)
        }
        .menuStyle(.borderlessButton)
        .menuIndicator(.hidden)
        .fixedSize()
        .accessibilityLabel("供应商筛选，当前：\(selectedTitle)")
        .accessibilityHint("选择供应商筛选账户用量")
    }
}
