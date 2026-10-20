import AppKit
import SwiftUI

private struct PointingHandCursorModifier: ViewModifier {
    @State private var isHovering = false

    func body(content: Content) -> some View {
        content
            .onHover { hovering in
                guard hovering != isHovering else { return }
                isHovering = hovering
                if hovering {
                    NSCursor.pointingHand.push()
                } else {
                    NSCursor.pop()
                }
            }
            .onDisappear {
                if isHovering {
                    NSCursor.pop()
                    isHovering = false
                }
            }
    }
}

extension View {
    /// macOS 的普通按钮默认仍显示箭头光标；可点击控件统一使用手指光标。
    func pointingHandCursorOnHover() -> some View {
        modifier(PointingHandCursorModifier())
    }
}
