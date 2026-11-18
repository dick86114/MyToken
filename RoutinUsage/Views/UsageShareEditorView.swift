import AppKit
import SwiftUI

struct UsageShareEditorView: View {
    let content: UsageShareContent
    var onClose: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var draft: UsageShareDraft
    @State private var copyStatus: String?
    @State private var showsMoreFields = false

    init(
        content: UsageShareContent,
        onClose: @escaping () -> Void
    ) {
        self.content = content
        self.onClose = onClose
        _draft = State(initialValue: UsageShareDraft.make(from: content))
    }

    private var rendered: UsageShareRenderedCard {
        UsageShareContentBuilder.render(content: content, draft: draft)
    }

    var body: some View {
        VStack(spacing: 0) {
            header

            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)

            HStack(spacing: 0) {
                previewPane
                editorPane
            }
        }
        .liquidGlassWindowBackground()
        .frame(minWidth: 860, minHeight: 640)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .onExitCommand(perform: onClose)
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 12) {
            SettingsModalTitle(
                "分享用量",
                subtitle: "\(rendered.displayName) · \(draft.template.title)"
            )

            Spacer(minLength: 16)
        }
        .padding(.horizontal, 22)
        .padding(.top, 18)
        .padding(.bottom, 14)
        .background(CompactPopoverPalette.surface(.window, colorScheme))
    }

    private var previewPane: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 8) {
                    Text("实时预览")
                        .font(SettingsTypography.sectionTitle)
                    SettingsStatusPill(
                        text: draft.template.previewTag,
                        tone: .info
                    )
                }
                Spacer()
                Text("自适应高度 · 100% 完整视图")
                    .font(SettingsTypography.caption)
                    .foregroundStyle(.tertiary)
            }
            ScrollView(.vertical, showsIndicators: false) {
                UsageShareCardView(card: rendered)
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel("用量分享图预览")
            }
            HStack {
                Label("数字完全来自本地快照，真实用量不可篡改", systemImage: "info.circle")
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
                Spacer()
                if let copyStatus {
                    SettingsStatusPill(
                        text: copyStatus,
                        tone: copyStatus == "已复制到剪贴板" ? .success : .error
                    )
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(CompactPopoverPalette.surface(.metric, colorScheme))
    }

    private var editorPane: some View {
        VStack(alignment: .leading, spacing: 0) {
            ScrollView(.vertical, showsIndicators: false) {
                VStack(alignment: .leading, spacing: 20) {
                    templatePicker
                    customFields
                    fieldToggles
                }
                .padding(.bottom, 12)
            }
            actionBar
        }
        .padding(20)
        .frame(width: 420)
        .frame(maxHeight: .infinity)
        .background(CompactPopoverPalette.surface(.card, colorScheme))
        .overlay(alignment: .leading) {
            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(width: 0.8)
        }
    }

    private var templatePicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            SettingsSectionHeader(
                title: "模板风格",
                systemImage: "rectangle.3.group"
            )

            Picker("模板风格", selection: $draft.template) {
                ForEach(UsageShareTemplate.allCases) { template in
                    Label(template.title, systemImage: templateIcon(template))
                        .tag(template)
                }
            }
            .pickerStyle(.segmented)
            .labelsHidden()
            .accessibilityLabel("模板风格")
        }
    }

    private var customFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            SettingsSectionHeader(
                title: "文案与备注",
                systemImage: "textformat"
            )
            editorField(
                "显示名称",
                hint: "仅用于本次图片展示",
                text: $draft.displayName
            )
            editorField(
                "套餐文案",
                hint: "可补充团队说明",
                text: $draft.subtitle
            )
            editorField(
                "卡片附注",
                hint: "仅出现在分享图",
                text: $draft.note,
                hintAccent: true
            )
        }
        .padding(.top, 4)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)
        }
        .padding(.top, 16)
    }

    private var fieldToggles: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 8) {
                Image(systemName: "checklist")
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(Color.accentColor)

                VStack(alignment: .leading, spacing: 2) {
                    Text("显示内容")
                        .font(SettingsTypography.sectionTitle)
                    Text("关闭的字段不会出现在分享图中")
                        .font(SettingsTypography.caption)
                        .foregroundStyle(.secondary)
                }

                Spacer()
                Button("全部显示") {
                    draft.showAll(from: content)
                }
                .font(SettingsTypography.secondary.weight(.medium))
                .foregroundStyle(Color.accentColor)
                .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.98))
            }

            fieldSection(title: "基本信息", cells: basicFieldToggles)
            fieldSection(title: "用量指标", cells: metricFieldToggles)

            Button {
                withAnimation(
                    reduceMotion
                        ? nil
                        : .spring(response: 0.3, dampingFraction: 0.9)
                ) {
                    showsMoreFields.toggle()
                }
            } label: {
                HStack(spacing: 8) {
                    Text("更多字段")
                        .font(.system(size: 11.5, weight: .semibold))
                    Spacer()
                    Text("\(moreUsageFieldToggles.count + brandFieldToggles.count) 项")
                        .font(SettingsTypography.caption)
                        .foregroundStyle(.secondary)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 9, weight: .semibold))
                        .foregroundStyle(.secondary)
                        .rotationEffect(.degrees(showsMoreFields ? 180 : 0))
                }
                .padding(.horizontal, 10)
                .frame(minHeight: 34)
                .background(
                    SettingsDesign.fieldFill(colorScheme),
                    in: RoundedRectangle(cornerRadius: 10, style: .continuous)
                )
                .overlay {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 0.8)
                }
                .contentShape(Rectangle())
            }
            .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.99))
            .accessibilityLabel("更多分享字段")
            .accessibilityValue(showsMoreFields ? "已展开" : "已收起")

            if showsMoreFields {
                VStack(alignment: .leading, spacing: 14) {
                    fieldSection(title: "用量细节", cells: moreUsageFieldToggles)
                    fieldSection(title: "品牌与水印", cells: brandFieldToggles)
                }
                .transition(.opacity.combined(with: .move(edge: .top)))
            }
        }
        .padding(.top, 4)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)
        }
        .padding(.top, 16)
    }

    private var basicFieldToggles: [(title: String, binding: Binding<Bool>)] {
        var cells: [(title: String, binding: Binding<Bool>)] = []

        cells.append(("可用状态徽章", $draft.showsStatus))
        if !content.planName.isEmpty || !content.subtitle.isEmpty {
            cells.append(("套餐规格", $draft.showsSubtitle))
        }
        if content.cycleRemainingText != nil || content.subscriptionStartText != nil || content.subscriptionEndText != nil {
            cells.append(("订阅周期/到期", $draft.showsSubscriptionDates))
        }
        if content.expiryText != nil || content.cycleRemainingText != nil {
            cells.append(("到期与续费状态", $draft.showsExpiry))
        }
        cells.append(("附加备注框", $draft.showsNote))
        return cells
    }

    private var metricFieldToggles: [(title: String, binding: Binding<Bool>)] {
        var cells: [(title: String, binding: Binding<Bool>)] = []
        cells.append(("金额与数值明细", $draft.showsAmounts))

        let progressMetrics = content.metrics.filter { $0.percent != nil }
        let tileMetrics = content.metrics.filter { $0.percent == nil }
        for item in progressMetrics + tileMetrics {
            let id = item.id
            cells.append((item.title, metricVisibleBinding(id)))
        }
        return cells
    }

    private var moreUsageFieldToggles: [(title: String, binding: Binding<Bool>)] {
        var cells: [(title: String, binding: Binding<Bool>)] = []
        if content.metrics.contains(where: { $0.resetBadgeText != nil || !$0.timeDetails.isEmpty }) {
            cells.append(("重置时间与倒计时", $draft.showsResetTimes))
        }

        if content.tokenPercentText != nil {
            cells.append(("Token 占比与缓存", $draft.showsTokenPercent))
        }
        if content.groupMultiplierText != nil {
            cells.append(("Codex 路由分组", Binding(
                get: { draft.showsGroupMultiplier },
                set: {
                    draft.showsGroupMultiplier = $0
                }
            )))
        }
        return cells
    }

    private var brandFieldToggles: [(title: String, binding: Binding<Bool>)] {
        var cells: [(title: String, binding: Binding<Bool>)] = []
        cells.append(("快照水印与防伪", $draft.showsWatermark))
        return cells
    }

    private func fieldSection(
        title: String,
        cells: [(title: String, binding: Binding<Bool>)]
    ) -> some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(title)
                .font(.system(size: 11.5, weight: .semibold))
                .foregroundStyle(.secondary)

            LazyVStack(alignment: .leading, spacing: 8) {
                ForEach(Array(cells.enumerated()), id: \.offset) { _, cell in
                    toggleCell(cell.title, isOn: cell.binding)
                }
            }
        }
    }

    private var actionBar: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Button(action: copyImage) {
                    Label("复制图片到剪贴板", systemImage: "doc.on.doc")
                        .font(SettingsTypography.button)
                        .frame(maxWidth: .infinity, minHeight: 32)
                }
                .liquidGlassButton(prominent: true)
                .keyboardShortcut("c", modifiers: [.command, .shift])
                Button(action: saveImage) {
                    Label("保存 PNG 到本地", systemImage: "square.and.arrow.down")
                        .font(SettingsTypography.button)
                        .frame(maxWidth: .infinity, minHeight: 32)
                }
                .liquidGlassButton()
                .keyboardShortcut("s", modifiers: .command)
            }
            Text("快捷键: ⌘⇧C 复制 · ⌘S 保存")
                .font(SettingsTypography.caption)
                .foregroundStyle(.tertiary)
        }
        .padding(.top, 16)
        .overlay(alignment: .top) {
            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)
        }
    }

    private func editorField(_ title: String, hint: String, text: Binding<String>, hintAccent: Bool = false) -> some View {
        SettingsLabeledTextField(
            label: title,
            text: text,
            prompt: "",
            helpText: hint,
            accentColor: hintAccent
                ? CompactPopoverPalette.warningColor
                : Color.accentColor
        )
    }

    private func toggleCell(_ title: String, isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            Text(title)
                .font(SettingsTypography.secondary)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .toggleStyle(.switch)
        .controlSize(.small)
        .padding(.horizontal, 10)
        .padding(.vertical, 8)
        .background(
            SettingsDesign.fieldFill(colorScheme),
            in: RoundedRectangle(cornerRadius: 10, style: .continuous)
        )
        .overlay {
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .strokeBorder(SettingsDesign.fieldBorder(colorScheme), lineWidth: 0.8)
        }
    }

    private func templateIcon(_ template: UsageShareTemplate) -> String {
        switch template {
        case .ticket: return "ticket.fill"
        case .ticketLight: return "ticket"
        case .dark: return "square"
        case .light: return "sun.max"
        }
    }

    private func metricVisibleBinding(_ id: String) -> Binding<Bool> {
        Binding(
            get: { draft.isMetricVisible(id) },
            set: { draft.setMetricVisible(id, $0) }
        )
    }

    private func copyImage() {
        if UsageShareExport.copyPNG(for: rendered) {
            copyStatus = "已复制到剪贴板"
        } else {
            copyStatus = "复制失败"
        }
    }

    private func saveImage() {
        UsageShareExport.savePNG(
            for: rendered,
            suggestedName: UsageShareContentBuilder.fileName(
                displayName: rendered.displayName,
                date: content.capturedAt
            )
        )
    }
}

enum UsageShareWindowFrame {
    static let widthKey = "usageShareWindowWidth"
    static let heightKey = "usageShareWindowHeight"
    static let xKey = "usageShareWindowX"
    static let yKey = "usageShareWindowY"
    static let defaultSize = NSSize(width: 1020, height: 680)
    static let minimumSize = NSSize(width: 860, height: 640)

    static func load(defaults: UserDefaults = .standard) -> NSRect? {
        let width = defaults.double(forKey: widthKey)
        let height = defaults.double(forKey: heightKey)
        guard width >= minimumSize.width, height >= minimumSize.height else {
            return nil
        }
        let x = defaults.object(forKey: xKey) as? Double
        let y = defaults.object(forKey: yKey) as? Double
        guard let x, let y else {
            return NSRect(origin: .zero, size: NSSize(width: width, height: height))
        }
        return NSRect(x: x, y: y, width: width, height: height)
    }

    static func save(_ frame: NSRect, defaults: UserDefaults = .standard) {
        guard frame.width >= minimumSize.width, frame.height >= minimumSize.height else {
            return
        }
        defaults.set(frame.width, forKey: widthKey)
        defaults.set(frame.height, forKey: heightKey)
        defaults.set(frame.origin.x, forKey: xKey)
        defaults.set(frame.origin.y, forKey: yKey)
    }

    static func clamp(_ frame: NSRect, to screen: NSScreen?) -> NSRect {
        guard let visible = screen?.visibleFrame else { return frame }
        var result = frame
        result.size.width = max(result.width, minimumSize.width)
        result.size.height = max(result.height, minimumSize.height)
        result.size.width = min(result.size.width, visible.width)
        result.size.height = min(result.size.height, visible.height * 0.92)
        if result.maxX > visible.maxX {
            result.origin.x = visible.maxX - result.width
        }
        if result.maxY > visible.maxY {
            result.origin.y = visible.maxY - result.height
        }
        if result.minX < visible.minX {
            result.origin.x = visible.minX
        }
        if result.minY < visible.minY {
            result.origin.y = visible.minY
        }
        return result
    }

    static func estimatedCardHeight(for card: UsageShareRenderedCard) -> CGFloat {
        let isTicket = card.template == .ticket || card.template == .ticketLight
        let header: CGFloat = isTicket ? 176 : 148
        let stub: CGFloat = isTicket ? 128 : 32
        let note: CGFloat = card.note == nil ? 0 : 56
        let subscription: CGFloat = (card.subscriptionStartText != nil || card.subscriptionEndText != nil) ? 24 : 0
        var height: CGFloat = header + stub + note + subscription
        var index = 0
        let metrics = card.metrics
        while index < metrics.count {
            let item = metrics[index]
            if item.spansFullWidth {
                height += metricHeight(item) + 16
                index += 1
            } else if index + 1 < metrics.count, !metrics[index + 1].spansFullWidth {
                height += max(metricHeight(item), metricHeight(metrics[index + 1])) + 16
                index += 2
            } else {
                height += metricHeight(item) + 16
                index += 1
            }
        }
        return max(height, 520)
    }

    private static func metricHeight(_ item: UsageShareMetricItem) -> CGFloat {
        var height: CGFloat = item.percent == nil ? 58 : 88
        height += CGFloat(item.amountDetails.count + item.timeDetails.count) * 16
        return height
    }
}

final class UsageSharePanelWindow: NSWindow {
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { true }
}

@MainActor
final class UsageSharePanelController: NSObject, NSWindowDelegate {
    static let shared = UsageSharePanelController()

    private var window: NSWindow?
    private var canPersistFrame = false

    func present(content: UsageShareContent) {
        // 避开菜单栏弹窗当前这一轮 display cycle，防止约束更新嵌套闪退。
        DispatchQueue.main.async { [weak self] in
            self?.show(content: content)
        }
    }

    private func show(content: UsageShareContent) {
        let editor = UsageShareEditorView(
            content: content,
            onClose: { [weak self] in self?.window?.close() }
        )
        let hosting = NSHostingController(rootView: editor)
        hosting.sizingOptions = []
        hosting.view.translatesAutoresizingMaskIntoConstraints = true
        hosting.view.autoresizingMask = [.width, .height]

        let fitted = preferredFrame(for: content)
        if let window {
            window.contentViewController = hosting
            applyChrome(window)
            if window.frame.height < UsageShareWindowFrame.minimumSize.height {
                window.setFrame(fitted, display: true)
            }
            SettingsWindowActivationPolicy.register(window)
            window.makeKeyAndOrderFront(nil)
            window.makeFirstResponder(hosting.view)
            NSApp.activate(ignoringOtherApps: true)
            return
        }

        let window = UsageSharePanelWindow(
            contentRect: NSRect(origin: .zero, size: fitted.size),
            styleMask: [.titled, .closable, .miniaturizable, .resizable],
            backing: .buffered,
            defer: false
        )
        window.isReleasedWhenClosed = false
        window.isRestorable = false
        window.delegate = self
        window.contentViewController = hosting
        window.minSize = UsageShareWindowFrame.minimumSize
        applyChrome(window)
        window.setFrame(fitted, display: false)
        SettingsWindowActivationPolicy.register(window)
        window.makeKeyAndOrderFront(nil)
        window.makeFirstResponder(hosting.view)
        NSApp.activate(ignoringOtherApps: true)
        self.window = window
        DispatchQueue.main.async { [weak self] in
            guard let self, let window = self.window else { return }
            window.setFrame(fitted, display: true)
            self.canPersistFrame = true
        }
    }

    func windowDidMove(_ notification: Notification) {
        saveFrame()
    }

    func windowDidEndLiveResize(_ notification: Notification) {
        saveFrame()
    }

    func windowWillClose(_ notification: Notification) {
        saveFrame()
        canPersistFrame = false
        if let window {
            window.contentViewController = nil
            SettingsWindowActivationPolicy.unregister(window)
        }
    }

    private func applyChrome(_ window: NSWindow) {
        window.title = "分享用量"
        window.hasShadow = true
        window.isMovableByWindowBackground = true
        window.styleMask.insert(.fullSizeContentView)
        window.titleVisibility = .hidden
        window.titlebarAppearsTransparent = true
        window.titlebarSeparatorStyle = .none
        window.backgroundColor = .clear
    }

    private func resetLayout(for content: UsageShareContent) {
        let defaults = UserDefaults.standard
        defaults.removeObject(forKey: UsageShareWindowFrame.widthKey)
        defaults.removeObject(forKey: UsageShareWindowFrame.heightKey)
        defaults.removeObject(forKey: UsageShareWindowFrame.xKey)
        defaults.removeObject(forKey: UsageShareWindowFrame.yKey)
        let frame = preferredFrame(for: content)
        window?.setFrame(frame, display: true, animate: true)
        UsageShareWindowFrame.save(frame)
    }

    private func preferredFrame(for content: UsageShareContent) -> NSRect {
        if let saved = UsageShareWindowFrame.load() {
            return UsageShareWindowFrame.clamp(saved, to: NSScreen.main)
        }

        let draft = UsageShareDraft.make(from: content)
        let card = UsageShareContentBuilder.render(content: content, draft: draft)
        let cardHeight = UsageShareWindowFrame.estimatedCardHeight(for: card)
        let size = NSSize(width: UsageShareWindowFrame.defaultSize.width, height: max(UsageShareWindowFrame.defaultSize.height, 108 + cardHeight))
        var frame = NSRect(origin: .zero, size: size)
        frame = UsageShareWindowFrame.clamp(frame, to: NSScreen.main)
        if let visible = NSScreen.main?.visibleFrame {
            frame.origin.x = visible.midX - frame.width / 2
            frame.origin.y = visible.midY - frame.height / 2
        }
        return frame
    }

    private func saveFrame() {
        guard canPersistFrame, let window else { return }
        UsageShareWindowFrame.save(window.frame)
    }
}
