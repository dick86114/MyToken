import SwiftUI

/// 在各个窗口中提供一致的明亮玻璃表面，并兼容 macOS 14 至 25。
private struct LiquidGlassSurfaceModifier: ViewModifier {
    let cornerRadius: CGFloat

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        if #available(macOS 26.0, *) {
            content
                .glassEffect(.regular.tint(.white.opacity(0.16)), in: shape)
        } else {
            content
                .background(.regularMaterial, in: shape)
                .overlay {
                    shape.stroke(.white.opacity(0.34), lineWidth: 0.8)
                }
                .shadow(color: .black.opacity(0.08), radius: 10, y: 4)
        }
    }
}

/// 为整个窗口提供透亮底层，窗口内的卡片继续使用独立玻璃表面。
private struct LiquidGlassWindowBackgroundModifier: ViewModifier {
    @ViewBuilder
    func body(content: Content) -> some View {
        if #available(macOS 26.0, *) {
            content.background {
                Rectangle()
                    .fill(.clear)
                    .glassEffect(.regular.tint(.white.opacity(0.10)), in: Rectangle())
            }
        } else {
            content.background(.regularMaterial)
        }
    }
}

extension View {
    func liquidGlassSurface(cornerRadius: CGFloat = 16) -> some View {
        modifier(LiquidGlassSurfaceModifier(cornerRadius: cornerRadius))
    }

    func liquidGlassWindowBackground() -> some View {
        modifier(LiquidGlassWindowBackgroundModifier())
    }

    func liquidGlassControlSurface() -> some View {
        padding(.vertical, 5)
            .padding(.horizontal, 8)
            .liquidGlassSurface(cornerRadius: 10)
    }

    func liquidGlassProgressSurface() -> some View {
        padding(3)
            .liquidGlassSurface(cornerRadius: 7)
    }

    @ViewBuilder
    func liquidGlassButton(prominent: Bool = false) -> some View {
        if #available(macOS 26.0, *) {
            if prominent {
                self.buttonStyle(.glassProminent)
            } else {
                self.buttonStyle(.glass)
            }
        } else if prominent {
            self.buttonStyle(.borderedProminent)
        } else {
            self.buttonStyle(.bordered)
        }
    }
}
