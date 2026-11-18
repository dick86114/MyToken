import AppKit
import SwiftUI

/// 设置窗口的固定侧栏：品牌身份在上，账户/应用分组居中，隐私说明固定在底部。
struct SettingsSidebar: View {
    @Binding var selection: SettingsSection

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @Environment(\.accessibilityReduceTransparency) private var reduceTransparency
    @State private var hoveredSection: SettingsSection?

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            brandHeader
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 20)

            VStack(alignment: .leading, spacing: 4) {
                ForEach(SettingsSection.allCases) { section in
                    navigationRow(section)
                }
            }
            .padding(.horizontal, 10)

            Spacer(minLength: 24)

            sidebarFooter
                .padding(.horizontal, 16)
                .padding(.bottom, 16)
        }
        .frame(width: 220)
        .frame(maxHeight: .infinity, alignment: .top)
        .background {
            if reduceTransparency {
                CompactPopoverPalette.opaqueWindow(colorScheme)
                    .ignoresSafeArea(edges: .top)
            } else {
                LinearGradient(
                    colors: [
                        Color.primary.opacity(colorScheme == .dark ? 0.048 : 0.024),
                        Color.primary.opacity(colorScheme == .dark ? 0.022 : 0.010)
                    ],
                    startPoint: .top,
                    endPoint: .bottom
                )
                .ignoresSafeArea(edges: .top)
            }
        }
        .overlay(alignment: .trailing) {
            Rectangle()
                .fill(Color.primary.opacity(colorScheme == .dark ? 0.09 : 0.065))
                .frame(width: 0.5)
                .ignoresSafeArea(edges: .top)
        }
    }

    private var brandHeader: some View {
        HStack(spacing: 10) {
            Image(nsImage: NSImage(named: "PopoverColorBrandLogo") ?? NSApp.applicationIconImage)
                .resizable()
                .interpolation(.high)
                .scaledToFit()
                .frame(width: 28, height: 28)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 1) {
                Text("MyToken")
                    .font(.system(size: 13.5, weight: .semibold))
                    .tracking(-0.1)

                Text("菜单栏用量")
                    .font(.system(size: 10.5))
                    .foregroundStyle(.secondary)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityLabel("MyToken 菜单栏用量设置")
    }

    private func navigationRow(_ section: SettingsSection) -> some View {
        let isSelected = selection == section

        return Button {
            select(section)
        } label: {
            HStack(spacing: 10) {
                Image(systemName: section.symbol)
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(
                        isSelected
                            ? Color.accentColor
                            : Color.secondary
                    )
                    .frame(width: 16)

                Text(section.title)
                    .font(.system(
                        size: 12.5,
                        weight: isSelected ? .semibold : .medium
                    ))
                    .foregroundStyle(
                        isSelected
                            ? Color.primary
                            : Color.primary.opacity(0.82)
                    )
                    .lineLimit(1)

                Spacer(minLength: 0)
            }
            .padding(.horizontal, 10)
            .frame(minHeight: 32)
            .background {
                RoundedRectangle(cornerRadius: 9, style: .continuous)
                    .fill(rowFill(for: section, isSelected: isSelected))
            }
            .overlay {
                if isSelected {
                    RoundedRectangle(cornerRadius: 9, style: .continuous)
                        .strokeBorder(
                            Color.accentColor.opacity(0.16),
                            lineWidth: 0.8
                        )
                }
            }
            .contentShape(RoundedRectangle(cornerRadius: 9, style: .continuous))
        }
        .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.985))
        .onHover { isInside in
            hoveredSection = isInside ? section : (hoveredSection == section ? nil : hoveredSection)
        }
        .accessibilityLabel(section.title)
        .accessibilityAddTraits(isSelected ? [.isSelected] : [])
    }

    private func rowFill(for section: SettingsSection, isSelected: Bool) -> Color {
        if isSelected {
            return Color.accentColor
                .opacity(colorScheme == .dark ? 0.20 : 0.12)
        }
        if hoveredSection == section {
            return Color.primary.opacity(colorScheme == .dark ? 0.055 : 0.035)
        }
        return .clear
    }

    private func select(_ section: SettingsSection) {
        guard selection != section else { return }
        if reduceMotion {
            selection = section
        } else {
            withAnimation(.spring(response: 0.3, dampingFraction: 0.88)) {
                selection = section
            }
        }
    }

    private var sidebarFooter: some View {
        HStack(spacing: 5) {
            Image(systemName: "lock.shield")
                .font(.system(size: 9.5, weight: .medium))

            Text("凭证仅保存在此 Mac")
                .lineLimit(1)
        }
        .font(.system(size: 10))
        .foregroundStyle(.secondary)
        .accessibilityElement(children: .combine)
        .padding(.horizontal, 10)
    }
}
