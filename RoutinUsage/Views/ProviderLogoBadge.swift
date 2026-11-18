import AppKit
import SwiftUI

/// 统一展示供应商品牌 Logo，避免各界面重复维护图片加载和容器样式。
struct ProviderLogoBadge: View {
    let providerID: ProviderID
    let size: CGFloat
    var cornerRadius: CGFloat = 8
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Image(nsImage: NSImage(named: providerID.logoAssetName) ?? NSApp.applicationIconImage)
            .resizable()
            .interpolation(.high)
            .scaledToFit()
            .padding(size * 0.12)
            .frame(width: size, height: size)
            .background(
                colorScheme == .dark
                    ? Color.white.opacity(0.92)
                    : Color.white
            )
            .clipShape(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(
                        colorScheme == .dark
                            ? Color.white.opacity(0.24)
                            : Color.black.opacity(0.10),
                        lineWidth: 0.5
                    )
            }
            .accessibilityHidden(true)
    }
}

struct ProviderLogoMark: View {
    let providerID: ProviderID
    let size: CGFloat
    var cornerRadius: CGFloat = 10

    var body: some View {
        Image(nsImage: NSImage(named: providerID.logoAssetName) ?? NSApp.applicationIconImage)
            .resizable()
            .interpolation(.high)
            .scaledToFit()
            .padding(size * 0.12)
            .frame(width: size, height: size)
            .background(Color.white)
            .clipShape(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(Color.black.opacity(0.10), lineWidth: 0.5)
            }
            .accessibilityHidden(true)
    }
}
