import AppKit
import SwiftUI

/// 统一展示供应商品牌 Logo，避免各界面重复维护图片加载和容器样式。
struct ProviderLogoBadge: View {
    let providerID: ProviderID
    let size: CGFloat
    var cornerRadius: CGFloat = 8

    var body: some View {
        Image(nsImage: NSImage(named: providerID.logoAssetName) ?? NSApp.applicationIconImage)
            .resizable()
            .interpolation(.high)
            .scaledToFill()
            .frame(width: size, height: size)
            .background(Color.white)
            .clipShape(
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
            )
            .overlay {
                RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
                    .strokeBorder(Color.black.opacity(0.12), lineWidth: 0.5)
            }
            .accessibilityHidden(true)
    }
}
