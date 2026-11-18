import AppKit
import SwiftUI

enum MenuBarColorRulesEditorLayout {
    case wide
    case compact
}

struct MenuBarColorRulesEditor: View {
    @Binding var rules: MenuBarColorRules
    var layout: MenuBarColorRulesEditorLayout = .wide
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var workingThresholds: Thresholds?
    @State private var activeBoundary: MenuBarColorRulesEditorBoundary?
    @State private var showsResetConfirmation = false

    private var displayedThresholds: Thresholds {
        workingThresholds ?? Thresholds(
            warning: rules.warningThreshold,
            critical: rules.criticalThreshold
        )
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("颜色阈值")
                    .font(SettingsTypography.secondary.weight(.medium))
                    .foregroundStyle(.secondary)

                Spacer()

                Button("恢复默认") {
                    showsResetConfirmation = true
                }
                .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.98))
                .font(SettingsTypography.secondary.weight(.medium))
                .foregroundStyle(.secondary)
                .disabled(rules == .standard)
                .accessibilityLabel("恢复默认进度颜色")
            }

            MenuBarThresholdDragGesture(
                normalColor: rules.normalColor,
                warningColor: rules.warningColor,
                criticalColor: rules.criticalColor,
                thresholds: displayedThresholds,
                activeBoundary: $activeBoundary,
                update: updateWorkingThresholds,
                commit: commitThresholds
            )

            colorLevelControls
        }
        .frame(
            maxWidth: layout == .wide ? 520 : .infinity,
            alignment: .leading
        )
        .onChange(of: rules) { _, _ in
            workingThresholds = nil
            activeBoundary = nil
        }
        .confirmationDialog(
            "恢复默认进度颜色？",
            isPresented: $showsResetConfirmation,
            titleVisibility: .visible
        ) {
            Button("恢复默认", role: .destructive, action: resetToDefaults)
            Button("取消", role: .cancel) {}
        } message: {
            Text("将同时重置两个阈值和正常、提醒、紧急三种颜色。")
        }
    }

    @ViewBuilder
    private var colorLevelControls: some View {
        if layout == .compact {
            HStack(spacing: 0) {
                compactColorLevelControl(
                    title: "正常",
                    range: "0–\(displayedThresholds.warning)%",
                    components: binding(for: \.normalColor)
                )
                compactColorDivider
                compactColorLevelControl(
                    title: "提醒",
                    range: "\(displayedThresholds.warning)–\(displayedThresholds.critical)%",
                    components: binding(for: \.warningColor)
                )
                compactColorDivider
                compactColorLevelControl(
                    title: "紧急",
                    range: "\(displayedThresholds.critical)–100%",
                    components: binding(for: \.criticalColor)
                )
            }
        } else {
            HStack(spacing: 8) {
                normalColorControl
                warningColorControl
                criticalColorControl
            }
        }
    }

    private var compactColorDivider: some View {
        Rectangle()
            .fill(SettingsDesign.dividerColor(colorScheme))
            .frame(width: 0.7, height: 28)
            .padding(.horizontal, 7)
            .accessibilityHidden(true)
    }

    private func compactColorLevelControl(
        title: String,
        range: String,
        components: Binding<MenuBarColorComponents>
    ) -> some View {
        HStack(spacing: 6) {
            ColorWellButton(title: title, components: components)
                .frame(width: 20, height: 20)
                .accessibilityLabel("\(title)颜色")

            VStack(alignment: .leading, spacing: 1) {
                Text(title)
                    .font(.system(size: 10.5, weight: .semibold))
                Text(range)
                    .font(.system(size: 9, design: .monospaced))
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .minimumScaleFactor(0.82)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .contentShape(Rectangle())
        .accessibilityElement(children: .contain)
    }

    private var normalColorControl: some View {
        colorLevelControl(
            title: "正常",
            range: "0–\(displayedThresholds.warning)%",
            components: binding(for: \.normalColor)
        )
    }

    private var warningColorControl: some View {
        colorLevelControl(
            title: "提醒",
            range: "\(displayedThresholds.warning)–\(displayedThresholds.critical)%",
            components: binding(for: \.warningColor)
        )
    }

    private var criticalColorControl: some View {
        colorLevelControl(
            title: "紧急",
            range: "\(displayedThresholds.critical)–100%",
            components: binding(for: \.criticalColor)
        )
    }

    private func colorLevelControl(
        title: String,
        range: String,
        components: Binding<MenuBarColorComponents>
    ) -> some View {
        let shape = RoundedRectangle(cornerRadius: 10, style: .continuous)

        return HStack(spacing: 8) {
            ColorWellButton(title: title, components: components)
                .frame(
                    width: layout == .compact ? 24 : 26,
                    height: layout == .compact ? 24 : 26
                )
                .accessibilityLabel("\(title)颜色")

            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(.system(size: 12, weight: .semibold))
                Text(range)
                    .font(.system(size: 10, design: .monospaced))
                    .monospacedDigit()
                    .foregroundStyle(.secondary)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 9)
        .padding(.vertical, layout == .compact ? 7 : 8)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            shape.fill(SettingsDesign.fieldFill(colorScheme))
        }
        .overlay {
            shape.strokeBorder(
                SettingsDesign.fieldBorder(colorScheme),
                lineWidth: 0.8
            )
        }
        .accessibilityElement(children: .contain)
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

    private func resetToDefaults() {
        withAnimation(
            reduceMotion
                ? nil
                : .spring(response: 0.3, dampingFraction: 0.9)
        ) {
            rules = .standard
        }
        workingThresholds = nil
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

private struct ColorWellButton: View {
    let title: String
    @Binding var components: MenuBarColorComponents
    @State private var panelLauncher = MenuBarColorPanelLauncher()

    var body: some View {
        Button {
            panelLauncher.onChange = { color in
                components = color
            }
            panelLauncher.present(components)
        } label: {
            Circle()
                .fill(Color(nsColor: components.nsColor))
                .overlay {
                    Circle()
                        .strokeBorder(.white.opacity(0.48), lineWidth: 1)
                }
                .shadow(color: .black.opacity(0.14), radius: 2, y: 1)
                .contentShape(Circle())
        }
        .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.92))
        .help("选择\(title)颜色")
        .accessibilityLabel("选择\(title)颜色")
    }
}

private final class MenuBarColorPanelLauncher: NSObject {
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

private struct MenuBarThresholdDragGesture: View {
    let normalColor: MenuBarColorComponents
    let warningColor: MenuBarColorComponents
    let criticalColor: MenuBarColorComponents
    let thresholds: MenuBarColorRulesEditor.Thresholds
    @Binding var activeBoundary: MenuBarColorRulesEditorBoundary?
    let update: (Double, MenuBarColorRulesEditorBoundary) -> Void
    let commit: () -> Void
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

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
                .frame(height: 20)
                .clipShape(Capsule())
                .overlay {
                    Capsule()
                        .strokeBorder(Color.primary.opacity(0.10), lineWidth: 0.8)
                }
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
            .frame(height: 30)

            GeometryReader { geometry in
                let labelWidth = CGFloat(geometry.size.width)
                let labelHeight = CGFloat(geometry.size.height)
                let warningX = labelWidth * CGFloat(thresholds.warning) / 100
                let criticalX = labelWidth * CGFloat(thresholds.critical) / 100
                ZStack {
                    thresholdLabel("0%", value: 0)
                        .position(x: 16, y: labelHeight / 2)
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
                        .position(x: labelWidth - 16, y: labelHeight / 2)
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
        Circle()
            .fill(.white)
            .overlay {
                Circle()
                    .strokeBorder(
                        isActive
                            ? Color.accentColor
                            : Color.black.opacity(0.12),
                        lineWidth: isActive ? 2 : 1
                    )
            }
            .frame(width: 18, height: 18)
            .shadow(
                color: .black.opacity(isActive ? 0.22 : 0.14),
                radius: isActive ? 6 : 3,
                y: 1
            )
            .scaleEffect(isActive ? 1.16 : 1)
            .animation(
                reduceMotion ? nil : .easeOut(duration: 0.12),
                value: isActive
            )
            .offset(x: x - 9)
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
