import AppKit
import SwiftUI

struct MenuBarColorRulesEditor: View {
    @Binding var rules: MenuBarColorRules
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var workingThresholds: Thresholds?
    @State private var activeBoundary: MenuBarColorRulesEditorBoundary?

    private var displayedThresholds: Thresholds {
        workingThresholds ?? Thresholds(
            warning: rules.warningThreshold,
            critical: rules.criticalThreshold
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 12) {
            MenuBarThresholdDragGesture(
                normalColor: rules.normalColor,
                warningColor: rules.warningColor,
                criticalColor: rules.criticalColor,
                thresholds: displayedThresholds,
                activeBoundary: $activeBoundary,
                update: updateWorkingThresholds,
                commit: commitThresholds
            )

            HStack(spacing: 10) {
                ColorSwatchButton(
                    title: "正常",
                    range: "0-\(displayedThresholds.warning)%",
                    components: binding(for: \.normalColor)
                )
                ColorSwatchButton(
                    title: "提醒",
                    range: "\(displayedThresholds.warning)-\(displayedThresholds.critical)%",
                    components: binding(for: \.warningColor)
                )
                ColorSwatchButton(
                    title: "紧急",
                    range: "\(displayedThresholds.critical)-100%",
                    components: binding(for: \.criticalColor)
                )
            }
        }
        .frame(maxWidth: 520, alignment: .leading)
        .onChange(of: rules) { _, _ in
            workingThresholds = nil
            activeBoundary = nil
        }
    }

    static func activeBoundary(
        horizontalRatio: Double,
        warning: Int,
        critical: Int
    ) -> MenuBarColorRulesEditorBoundary {
        let location = max(0, min(100, horizontalRatio * 100))
        let warningDistance = abs(location - Double(warning))
        let criticalDistance = abs(location - Double(critical))
        return warningDistance <= criticalDistance ? .warning : .critical
    }

    static func updatedValue(
        horizontalRatio: Double,
        active: MenuBarColorRulesEditorBoundary,
        warning: Int,
        critical: Int
    ) -> Int {
        let value = Int((max(0, min(100, horizontalRatio * 100))).rounded())
        switch active {
        case .warning:
            return max(1, min(value, critical - 1))
        case .critical:
            return max(warning + 1, min(value, 99))
        }
    }

    private func binding(
        for keyPath: WritableKeyPath<MenuBarColorRules, MenuBarColorComponents>
    ) -> Binding<MenuBarColorComponents> {
        Binding(
            get: { rules[keyPath: keyPath] },
            set: { value in
                var updated = rules
                updated[keyPath: keyPath] = value
                rules = updated
            }
        )
    }

    private func updateWorkingThresholds(
        ratio: Double,
        boundary: MenuBarColorRulesEditorBoundary
    ) {
        let current = workingThresholds ?? Thresholds(
            warning: rules.warningThreshold,
            critical: rules.criticalThreshold
        )
        let value = Self.updatedValue(
            horizontalRatio: ratio,
            active: boundary,
            warning: current.warning,
            critical: current.critical
        )

        var updated = current
        switch boundary {
        case .warning:
            updated.warning = value
        case .critical:
            updated.critical = value
        }

        guard updated.warning < updated.critical else { return }
        var transaction = Transaction()
        transaction.disablesAnimations = true
        withTransaction(transaction) {
            workingThresholds = updated
        }
    }

    private func commitThresholds() {
        if let workingThresholds,
           workingThresholds.warning != rules.warningThreshold
               || workingThresholds.critical != rules.criticalThreshold {
            var updated = rules
            updated.warningThreshold = workingThresholds.warning
            updated.criticalThreshold = workingThresholds.critical

            var transaction = Transaction()
            transaction.animation = reduceMotion
                ? nil
                : .interactiveSpring(response: 0.28, dampingFraction: 0.88)
            withTransaction(transaction) {
                rules = updated
            }
        }

        self.workingThresholds = nil
        activeBoundary = nil
    }

    struct Thresholds: Equatable {
        var warning: Int
        var critical: Int
    }
}

enum MenuBarColorRulesEditorBoundary {
    case warning
    case critical
}

private struct MenuBarThresholdDragGesture: View {
    let normalColor: MenuBarColorComponents
    let warningColor: MenuBarColorComponents
    let criticalColor: MenuBarColorComponents
    let thresholds: MenuBarColorRulesEditor.Thresholds
    @Binding var activeBoundary: MenuBarColorRulesEditorBoundary?
    let update: (Double, MenuBarColorRulesEditorBoundary) -> Void
    let commit: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            GeometryReader { geometry in
                let width = geometry.size.width
                let trackWidth = CGFloat(width)
                let warningRatio = CGFloat(thresholds.warning) / 100
                let criticalRatio = CGFloat(thresholds.critical) / 100
                let warningX = trackWidth * warningRatio
                let criticalX = trackWidth * criticalRatio
                let warningWidth = trackWidth * warningRatio
                let criticalWidth = trackWidth * (criticalRatio - warningRatio)
                let criticalSegmentWidth = max(0, width - criticalX)

                ZStack(alignment: .leading) {
                    HStack(spacing: 0) {
                        segment(
                            color: normalColor,
                            width: warningWidth
                        )
                        segment(
                            color: warningColor,
                            width: criticalWidth
                        )
                        segment(
                            color: criticalColor,
                            width: criticalSegmentWidth
                        )
                    }

                    LinearGradient(
                        colors: [.white.opacity(0.16), .clear],
                        startPoint: .top,
                        endPoint: .bottom
                    )
                    .allowsHitTesting(false)

                    boundaryHandle(
                        x: warningX,
                        isActive: activeBoundary == .warning
                    )
                    boundaryHandle(
                        x: criticalX,
                        isActive: activeBoundary == .critical
                    )
                }
                .frame(height: 26)
                .clipShape(Capsule())
                .overlay {
                    Capsule().strokeBorder(.white.opacity(0.22), lineWidth: 0.8)
                }
                .padding(.vertical, 3)
                .liquidGlassInteractiveControl(cornerRadius: 16)
                .contentShape(Rectangle())
                .gesture(
                    DragGesture(minimumDistance: 0)
                        .onChanged { value in
                            let locationX = Double(value.location.x)
                            let availableWidth = Double(max(8, width))
                            let ratio = locationX / availableWidth
                            let boundary: MenuBarColorRulesEditorBoundary
                            if let activeBoundary {
                                boundary = activeBoundary
                            } else {
                                boundary = MenuBarColorRulesEditor.activeBoundary(
                                    horizontalRatio: ratio,
                                    warning: thresholds.warning,
                                    critical: thresholds.critical
                                )
                            }
                            activeBoundary = boundary
                            update(ratio, boundary)
                        }
                        .onEnded { _ in
                            commit()
                        }
                )
            }
            .frame(height: 32)

            GeometryReader { geometry in
                let labelWidth = CGFloat(geometry.size.width)
                let labelHeight = CGFloat(geometry.size.height)
                let warningX = labelWidth * CGFloat(thresholds.warning) / 100
                let criticalX = labelWidth * CGFloat(thresholds.critical) / 100
                ZStack {
                    thresholdLabel("0%", value: 0)
                        .position(x: 0, y: labelHeight / 2)
                    thresholdLabel("\(thresholds.warning)%", value: thresholds.warning)
                        .position(
                            x: warningX,
                            y: labelHeight / 2
                        )
                    thresholdLabel("\(thresholds.critical)%", value: thresholds.critical)
                        .position(
                            x: criticalX,
                            y: labelHeight / 2
                        )
                    thresholdLabel("100%", value: 100)
                        .position(x: labelWidth, y: labelHeight / 2)
                }
            }
            .frame(height: 15)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("进度颜色阈值")
        .accessibilityValue("\(thresholds.warning)% 到 \(thresholds.critical)%")
        .accessibilityAction(named: "增加提醒阈值") {
            updateWorkingThresholds(ratio: Double(thresholds.warning + 1) / 100, boundary: .warning)
            commit()
        }
        .accessibilityAction(named: "减少提醒阈值") {
            updateWorkingThresholds(ratio: Double(thresholds.warning - 1) / 100, boundary: .warning)
            commit()
        }
        .accessibilityAction(named: "增加紧急阈值") {
            updateWorkingThresholds(ratio: Double(thresholds.critical + 1) / 100, boundary: .critical)
            commit()
        }
        .accessibilityAction(named: "减少紧急阈值") {
            updateWorkingThresholds(ratio: Double(thresholds.critical - 1) / 100, boundary: .critical)
            commit()
        }
    }

    private func segment(color: MenuBarColorComponents, width: CGFloat) -> some View {
        LinearGradient(
            colors: [
                Color(nsColor: color.nsColor).opacity(0.92),
                Color(nsColor: color.nsColor)
            ],
            startPoint: .top,
            endPoint: .bottom
        )
        .frame(width: max(0, width))
    }

    private func boundaryHandle(x: CGFloat, isActive: Bool) -> some View {
        Capsule()
            .fill(.white.opacity(isActive ? 0.24 : 0.14))
            .overlay {
                Capsule()
                    .strokeBorder(
                        isActive ? Color.accentColor : .white.opacity(0.48),
                        lineWidth: isActive ? 2 : 1
                    )
            }
            .frame(width: isActive ? 16 : 12, height: isActive ? 30 : 26)
            .shadow(color: .black.opacity(isActive ? 0.18 : 0.10), radius: isActive ? 6 : 3, y: 2)
            .liquidGlassInteractiveControl(cornerRadius: isActive ? 8 : 6)
            .scaleEffect(isActive ? 1.04 : 1)
            .offset(x: x - (isActive ? 8 : 6))
            .allowsHitTesting(false)
    }

    private func thresholdLabel(_ text: String, value: Int) -> some View {
        Text(text)
            .font(.caption2.monospacedDigit())
            .foregroundStyle(.secondary)
            .accessibilityHidden(value != thresholds.warning && value != thresholds.critical)
    }

    private func updateWorkingThresholds(ratio: Double, boundary: MenuBarColorRulesEditorBoundary) {
        let value = MenuBarColorRulesEditor.updatedValue(
            horizontalRatio: ratio,
            active: boundary,
            warning: thresholds.warning,
            critical: thresholds.critical
        )
        var updated = thresholds
        switch boundary {
        case .warning:
            updated.warning = value
        case .critical:
            updated.critical = value
        }

        guard updated.warning < updated.critical else { return }
        update(ratio, boundary)
    }
}

private struct ColorSwatchButton: View {
    let title: String
    let range: String
    @Binding var components: MenuBarColorComponents
    @State private var showsChoices = false

    var body: some View {
        Button {
            showsChoices = true
        } label: {
            HStack(spacing: 9) {
                Circle()
                    .fill(Color(nsColor: components.nsColor))
                    .frame(width: 15, height: 15)
                    .overlay {
                        Circle().strokeBorder(.white.opacity(0.36), lineWidth: 0.8)
                    }

                VStack(alignment: .leading, spacing: 1) {
                    Text(title)
                        .font(.callout.weight(.medium))
                        .foregroundStyle(.primary)
                    Text(range)
                        .font(.caption2.monospacedDigit())
                        .foregroundStyle(.secondary)
                }

                Spacer(minLength: 0)

                Image(systemName: "paintpalette")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
            .padding(.horizontal, 11)
            .padding(.vertical, 7)
            .background(.primary.opacity(0.055), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(.primary.opacity(0.07), lineWidth: 0.6)
            }
        }
        .buttonStyle(.plain)
        .accessibilityLabel("\(title)颜色")
        .accessibilityValue(range)
        .popover(
            isPresented: $showsChoices,
            attachmentAnchor: .point(.bottom),
            arrowEdge: .bottom
        ) {
            ColorChoicePopover(title: title, selection: $components)
        }
    }
}

private struct ColorChoicePopover: View {
    let title: String
    @Binding var selection: MenuBarColorComponents
    @Environment(\.dismiss) private var dismiss
    @State private var panelLauncher = ColorPanelLauncher()

    private var recommendations: [MenuBarColorComponents] {
        switch title {
        case "正常":
            return [
                .init(red: 0.20, green: 0.78, blue: 0.35),
                .init(red: 0.08, green: 0.72, blue: 0.58),
                .init(red: 0.12, green: 0.52, blue: 0.94),
                .init(red: 0.34, green: 0.36, blue: 0.88),
                .init(red: 0.56, green: 0.28, blue: 0.84)
            ]
        case "提醒":
            return [
                .init(red: 1.00, green: 0.78, blue: 0.12),
                .init(red: 1.00, green: 0.62, blue: 0.04),
                .init(red: 0.97, green: 0.47, blue: 0.16),
                .init(red: 0.95, green: 0.36, blue: 0.34),
                .init(red: 0.94, green: 0.33, blue: 0.58)
            ]
        default:
            return [
                .init(red: 1.00, green: 0.42, blue: 0.28),
                .init(red: 0.97, green: 0.33, blue: 0.28),
                .init(red: 0.90, green: 0.18, blue: 0.24),
                .init(red: 0.82, green: 0.10, blue: 0.32),
                .init(red: 0.58, green: 0.12, blue: 0.30)
            ]
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 13) {
            Text("\(title)颜色")
                .font(.headline)

            HStack(spacing: 8) {
                ForEach(Array(recommendations.enumerated()), id: \.offset) { index, color in
                    Button {
                        selection = color
                        dismiss()
                    } label: {
                        RoundedRectangle(cornerRadius: 8, style: .continuous)
                            .fill(Color(nsColor: color.nsColor))
                            .frame(width: 31, height: 28)
                            .overlay {
                                if color == selection {
                                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                                        .strokeBorder(.primary.opacity(0.62), lineWidth: 2)
                                }
                            }
                    }
                    .buttonStyle(.plain)
                    .accessibilityLabel("\(title)推荐颜色 \(index + 1)")
                }
            }

            Divider()

            Button {
                panelLauncher.onChange = { color in
                    selection = color
                }
                panelLauncher.present(selection)
            } label: {
                Label("自定义色盘", systemImage: "drop.halved")
            }
            .liquidGlassButton()
        }
        .padding(15)
        .frame(width: 238)
    }
}

private final class ColorPanelLauncher: NSObject {
    var onChange: ((MenuBarColorComponents) -> Void)?

    func present(_ color: MenuBarColorComponents) {
        let panel = NSColorPanel.shared
        panel.showsAlpha = false
        panel.setTarget(self)
        panel.setAction(#selector(colorDidChange(_:)))
        panel.color = color.nsColor
        panel.makeKeyAndOrderFront(nil)
    }

    @objc private func colorDidChange(_ sender: NSColorPanel) {
        guard let rgb = sender.color.usingColorSpace(.sRGB) else { return }
        onChange?(
            MenuBarColorComponents(
                red: Double(rgb.redComponent),
                green: Double(rgb.greenComponent),
                blue: Double(rgb.blueComponent)
            )
        )
    }
}
