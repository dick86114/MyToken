import SwiftUI

/// 两段式密度切换：支持直接点击和横向拖拽，拖拽时指示器与指针保持 1:1。
struct UsageCardDensitySegmentedControl: View {
    @Binding var selection: UsageCardDensity

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var dragTranslation: CGFloat = 0
    @State private var isDragging = false
    @State private var keyboardFocusActive = false
    @FocusState private var isFocused: Bool

    private let segmentWidth: CGFloat = 27
    private let segmentHeight: CGFloat = 23
    private let trackInset: CGFloat = 2

    private var selectedIndex: Int {
        selection == .compact ? 0 : 1
    }

    /// 拖拽过程中提前切换图标前景，避免松手后才出现颜色跳变。
    private var displayedIndex: Int {
        let position = CGFloat(selectedIndex) + dragTranslation / segmentWidth
        return position >= 0.5 ? 1 : 0
    }

    private var animation: Animation {
        reduceMotion
            ? .easeOut(duration: 0.16)
            : .spring(response: 0.32, dampingFraction: 0.86)
    }

    var body: some View {
        ZStack(alignment: .leading) {
            selectionIndicator
                .offset(
                    x: CGFloat(selectedIndex) * segmentWidth
                        + rubberBandedTranslation(dragTranslation)
                )

            HStack(spacing: 0) {
                segmentButton(.compact)
                segmentButton(.full)
            }
        }
        .padding(trackInset)
        .frame(
            width: segmentWidth * 2 + trackInset * 2,
            height: segmentHeight + trackInset * 2
        )
        .background {
            trackBackground
        }
        .overlay {
            if isFocused, keyboardFocusActive {
                Capsule(style: .continuous)
                    .strokeBorder(Color.accentColor.opacity(0.72), lineWidth: 1)
                    .padding(-1.5)
            }
        }
        .contentShape(Capsule(style: .continuous))
        .simultaneousGesture(dragGesture)
        .focusable(true)
        .focused($isFocused)
        .focusEffectDisabled()
        .onKeyPress(.leftArrow) {
            keyboardFocusActive = true
            selectDensity(.compact)
            return .handled
        }
        .onKeyPress(.rightArrow) {
            keyboardFocusActive = true
            selectDensity(.full)
            return .handled
        }
        .onKeyPress(.space) {
            keyboardFocusActive = true
            selectDensity(selection == .compact ? .full : .compact)
            return .handled
        }
        .onChange(of: isFocused) { _, focused in
            if !focused {
                keyboardFocusActive = false
            }
        }
        .accessibilityElement(children: .contain)
        .accessibilityLabel("卡片显示")
        .accessibilityValue(selection.title)
        .accessibilityHint("使用左右方向键切换简洁或完整模式")
        .accessibilityAdjustableAction { direction in
            switch direction {
            case .increment:
                selectDensity(.full)
            case .decrement:
                selectDensity(.compact)
            @unknown default:
                break
            }
        }
    }

    private var selectionIndicator: some View {
        Capsule(style: .continuous)
            .fill(CompactPopoverPalette.selectedSegmentFill(colorScheme))
            .frame(width: segmentWidth, height: segmentHeight)
            .overlay {
                Capsule(style: .continuous)
                    .strokeBorder(
                        CompactPopoverPalette.selectedSegmentStroke(colorScheme),
                        lineWidth: 1
                    )
            }
            .shadow(
                color: .black.opacity(colorScheme == .dark ? 0.24 : 0.08),
                radius: isDragging ? 4 : 2,
                y: isDragging ? 2 : 1
            )
            .animation(.easeOut(duration: 0.14), value: isDragging)
    }

    @ViewBuilder
    private var trackBackground: some View {
        let shape = Capsule(style: .continuous)

        if #available(macOS 26.0, *) {
            shape
                .fill(Color.primary.opacity(colorScheme == .dark ? 0.045 : 0.025))
                .glassEffect(
                    .regular.tint(Color.primary.opacity(colorScheme == .dark ? 0.08 : 0.03)),
                    in: shape
                )
                .overlay {
                    shape.strokeBorder(
                        CompactPopoverPalette.cardStroke(colorScheme).opacity(0.8),
                        lineWidth: 0.8
                    )
                }
        } else {
            shape
                .fill(.ultraThinMaterial)
                .overlay {
                    shape.strokeBorder(
                        CompactPopoverPalette.cardStroke(colorScheme).opacity(0.8),
                        lineWidth: 0.8
                    )
                }
        }
    }

    private func segmentButton(_ density: UsageCardDensity) -> some View {
        let isHighlighted = displayedIndex == (density == .compact ? 0 : 1)

        return Button {
            isFocused = true
            keyboardFocusActive = false
            selectDensity(density)
        } label: {
            Image(systemName: iconName(for: density))
                .font(.system(size: 12, weight: .medium))
                .foregroundStyle(
                    isHighlighted
                        ? Color.primary
                        : Color.secondary.opacity(0.72)
                )
                .frame(width: segmentWidth, height: segmentHeight)
                .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .focusable(false)
        .accessibilityLabel(density.title)
        .help(density.title)
        .accessibilityAddTraits(selection == density ? [.isSelected] : [])
    }

    private func selectDensity(_ density: UsageCardDensity) {
        withAnimation(animation) {
            selection = density
            dragTranslation = 0
        }
    }

    private var dragGesture: some Gesture {
        DragGesture(minimumDistance: 6)
            .onChanged { value in
                guard abs(value.translation.width) >= abs(value.translation.height) else {
                    return
                }
                keyboardFocusActive = false
                isDragging = true
                dragTranslation = value.translation.width
            }
            .onEnded { value in
                guard isDragging else { return }

                let projectedPosition = CGFloat(selectedIndex)
                    + value.predictedEndTranslation.width / segmentWidth
                let target = projectedPosition >= 0.5 ? UsageCardDensity.full : .compact

                withAnimation(animation) {
                    selection = target
                    dragTranslation = 0
                }
                isDragging = false
            }
    }

    private func iconName(for density: UsageCardDensity) -> String {
        switch density {
        case .compact:
            return "rectangle.stack"
        case .full:
            return "list.bullet.rectangle"
        }
    }

    /// 越界后逐渐降低跟随幅度，给出可感知但不会脱离轨道的边界反馈。
    private func rubberBandedTranslation(_ translation: CGFloat) -> CGFloat {
        let lowerBound = -CGFloat(selectedIndex) * segmentWidth
        let upperBound = CGFloat(1 - selectedIndex) * segmentWidth

        if translation < lowerBound {
            return lowerBound + (translation - lowerBound) * 0.22
        }
        if translation > upperBound {
            return upperBound + (translation - upperBound) * 0.22
        }
        return translation
    }
}
