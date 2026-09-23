import SwiftUI

/// 统一承载窗口与模态玻璃；普通卡片使用独立实色表面，并兼容 macOS 14 至 25。
private struct LiquidGlassSurfaceModifier: ViewModifier {
    let role: PopoverVisualPolicy.SurfaceRole
    let cornerRadius: CGFloat

    @Environment(\.colorScheme) private var colorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        let material = PopoverVisualPolicy.material(for: role)

        switch material {
        case .solid:
            content
                .background(shape.fill(CompactPopoverPalette.surface(role, colorScheme)))
                .overlay {
                    shape
                        .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme), lineWidth: 1)
                        .allowsHitTesting(false)
                }
        case .windowGlass, .modalGlass:
            if #available(macOS 26.0, *) {
                content
                    .glassEffect(
                        .regular.tint(CompactPopoverPalette.surface(role, colorScheme)),
                        in: shape
                    )
                    .overlay {
                        shape.stroke(Color.white.opacity(0.34), lineWidth: 0.8)
                    }
                    .modifier(PopoverShadowModifier(role: role))
            } else {
                content
                    .background(.regularMaterial, in: shape)
                    .background(shape.fill(CompactPopoverPalette.surface(role, colorScheme)))
                    .overlay {
                        shape.stroke(Color.white.opacity(0.34), lineWidth: 0.8)
                    }
                    .modifier(PopoverShadowModifier(role: role))
            }
        }
    }
}

private struct PopoverShadowModifier: ViewModifier {
    let role: PopoverVisualPolicy.SurfaceRole

    @ViewBuilder
    func body(content: Content) -> some View {
        if PopoverVisualPolicy.allowsShadow(for: role) {
            content.shadow(color: .black.opacity(0.24), radius: 18, y: 8)
        } else {
            content
        }
    }
}

/// 为整个窗口提供透亮底层，窗口内卡片使用实色表面。
private struct LiquidGlassWindowBackgroundModifier: ViewModifier {
    @Environment(\.colorScheme) private var colorScheme

    @ViewBuilder
    func body(content: Content) -> some View {
        let tint = CompactPopoverPalette.surface(.window, colorScheme)
        if #available(macOS 26.0, *) {
            content.background {
                Rectangle()
                    .fill(.clear)
                    .glassEffect(.regular.tint(tint), in: Rectangle())
            }
        } else {
            content
                .background(.regularMaterial)
                .background {
                    Rectangle().fill(tint)
                }
        }
    }
}

private struct LiquidGlassInteractiveControlModifier: ViewModifier {
    let cornerRadius: CGFloat

    @Environment(\.colorScheme) private var colorScheme

    func body(content: Content) -> some View {
        let shape = RoundedRectangle(cornerRadius: cornerRadius, style: .continuous)
        content
            .background(shape.fill(CompactPopoverPalette.surface(.control, colorScheme)))
            .overlay {
                shape.strokeBorder(CompactPopoverPalette.cardStroke(colorScheme), lineWidth: 1)
            }
    }
}

/// 以居中弹层承载编辑、提醒等临时操作，并支持点击焦外背景关闭。
private struct LiquidGlassOverlay<OverlayContent: View>: View {
    let onDismiss: () -> Void
    @ViewBuilder let content: () -> OverlayContent

    var body: some View {
        ZStack {
            Color.black.opacity(0.24)
                .ignoresSafeArea()
                .contentShape(Rectangle())
                .onTapGesture(perform: onDismiss)

            content()
                .liquidGlassModalSurface()
                .padding(16)
                .contentShape(Rectangle())
                .onTapGesture {}
        }
    }
}

private struct LiquidGlassOverlayModifier<Item: Identifiable, OverlayContent: View>: ViewModifier {
    @Binding var item: Item?
    let content: (Item) -> OverlayContent

    func body(content: Content) -> some View {
        content.overlay {
            if let item {
                LiquidGlassOverlay(onDismiss: { self.item = nil }) {
                    self.content(item)
                }
            }
        }
    }
}

extension View {

    func liquidGlassSurface(
        cornerRadius: CGFloat = PopoverVisualPolicy.cornerRadius(for: .outer)
    ) -> some View {
        modifier(LiquidGlassSurfaceModifier(role: .card, cornerRadius: cornerRadius))
    }

    func liquidGlassModalSurface(
        cornerRadius: CGFloat = PopoverVisualPolicy.cornerRadius(for: .outer)
    ) -> some View {
        modifier(LiquidGlassSurfaceModifier(role: .modal, cornerRadius: cornerRadius))
    }

    func liquidGlassWindowBackground() -> some View {
        modifier(LiquidGlassWindowBackgroundModifier())
    }

    func liquidGlassInteractiveControl(cornerRadius: CGFloat) -> some View {
        modifier(LiquidGlassInteractiveControlModifier(cornerRadius: cornerRadius))
    }

    func liquidGlassOverlay<Item: Identifiable, OverlayContent: View>(
        item: Binding<Item?>,
        @ViewBuilder content: @escaping (Item) -> OverlayContent
    ) -> some View {
        modifier(LiquidGlassOverlayModifier(item: item, content: content))
    }

    func liquidGlassControlSurface() -> some View {
        padding(.vertical, 5)
            .padding(.horizontal, 8)
            .liquidGlassSurface(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .button))
    }

    func liquidGlassProgressSurface() -> some View {
        padding(3)
            .liquidGlassSurface(cornerRadius: PopoverVisualPolicy.cornerRadius(for: .inner))
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
