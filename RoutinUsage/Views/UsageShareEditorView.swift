import AppKit
import SwiftUI

struct UsageShareEditorView: View {
    let content: UsageShareContent
    var onClose: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @State private var draft: UsageShareDraft
    @State private var copyStatus: String?

    init(content: UsageShareContent, onClose: @escaping () -> Void) {
        self.content = content
        self.onClose = onClose
        _draft = State(initialValue: UsageShareDraft.make(from: content))
    }

    private var rendered: UsageShareRenderedCard {
        UsageShareContentBuilder.render(content: content, draft: draft)
    }

    private var chrome: UsageShareChrome { .make(colorScheme) }

    var body: some View {
        HStack(spacing: 0) {
            previewPane
            editorPane
        }
        .frame(minWidth: 860, minHeight: 640)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }

    private var previewPane: some View {
        VStack(alignment: .leading, spacing: 12) {
            HStack {
                HStack(spacing: 8) {
                    Text("实时导出预览")
                        .font(.system(size: 11, weight: .medium))
                        .foregroundStyle(chrome.textSecondary)
                    Text(draft.template.previewTag)
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(chrome.amber)
                        .padding(.horizontal, 8)
                        .padding(.vertical, 3)
                        .background(chrome.amber.opacity(0.12), in: RoundedRectangle(cornerRadius: 4, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 4, style: .continuous)
                                .strokeBorder(chrome.amber.opacity(0.25), lineWidth: 1)
                        }
                }
                Spacer()
                Text("自适应高度 · 100% 完整视图")
                    .font(.system(size: 11, design: .monospaced))
                    .foregroundStyle(chrome.textTertiary)
            }
            ScrollView(.vertical, showsIndicators: false) {
                UsageShareCardView(card: rendered)
                    .frame(maxWidth: .infinity)
                    .accessibilityLabel("用量分享图预览")
            }
            HStack {
                Label("数字完全来自本地快照，真实用量不可篡改", systemImage: "info.circle")
                    .font(.system(size: 11))
                    .foregroundStyle(chrome.textSecondary)
                Spacer()
                if let copyStatus {
                    Text(copyStatus)
                        .font(.system(size: 11))
                        .foregroundStyle(chrome.textTertiary)
                }
            }
        }
        .padding(20)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(chrome.previewPane)
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
        .background(chrome.editorPane)
        .overlay(alignment: .leading) { Rectangle().fill(chrome.divider).frame(width: 1) }
    }

    private var templatePicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("模板风格选择 (实时切换)")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(chrome.textSecondary)
            HStack(spacing: 6) {
                ForEach(UsageShareTemplate.allCases) { template in
                    let selected = draft.template == template
                    Button {
                        draft.template = template
                    } label: {
                        HStack(spacing: 5) {
                            Image(systemName: templateIcon(template))
                                .font(.system(size: 11, weight: .semibold))
                            Text(template.title)
                                .lineLimit(1)
                        }
                        .font(.system(size: 11, weight: selected ? .semibold : .medium))
                        .foregroundStyle(selected ? chrome.amber : chrome.textSecondary)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 8)
                        .background {
                            if selected {
                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    .fill(Color(red: 0.96, green: 0.62, blue: 0.04).opacity(colorScheme == .dark ? 0.20 : 0.12))
                            }
                        }
                        .overlay {
                            if selected {
                                RoundedRectangle(cornerRadius: 8, style: .continuous)
                                    .strokeBorder(Color(red: 0.96, green: 0.62, blue: 0.04).opacity(0.35), lineWidth: 1)
                            }
                        }
                        .contentShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .contentShape(RoundedRectangle(cornerRadius: 8, style: .continuous))
                }
            }
            .padding(4)
            .background(chrome.inputFill, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 12, style: .continuous)
                    .strokeBorder(chrome.inputBorder, lineWidth: 1)
            }
        }
    }

    private var customFields: some View {
        VStack(alignment: .leading, spacing: 12) {
            Text("自定义文案与备注 (不影响云端账户)")
                .font(.system(size: 11, weight: .bold))
                .foregroundStyle(chrome.textSecondary)
            editorField("显示名称", hint: "仅用于本次图片展示", text: $draft.displayName)
            editorField("套餐文案", hint: "可补充团队说明", text: $draft.subtitle)
            editorField("卡片附注说明 (可选)", hint: "仅出现在分享图", text: $draft.note, hintAccent: true)
        }
        .padding(.top, 4)
        .overlay(alignment: .top) { Rectangle().fill(chrome.divider).frame(height: 1) }
        .padding(.top, 16)
    }

    private var fieldToggles: some View {
        let cells = visibleFieldToggles
        return VStack(alignment: .leading, spacing: 10) {
            HStack {
            Text("展示字段开关 (按票面顺序，隐藏即不导出)")
                    .font(.system(size: 11, weight: .bold))
                    .foregroundStyle(chrome.textSecondary)
                Spacer()
                Button("全部显示") {
                    draft.showAll(from: content)
                }
                .font(.system(size: 11, weight: .medium))
                .foregroundStyle(chrome.blue)
                .buttonStyle(.plain)
            }
            LazyVStack(alignment: .leading, spacing: 8) {
                ForEach(Array(cells.enumerated()), id: \.offset) { _, cell in
                    toggleCell(cell.title, isOn: cell.binding)
                }
            }
        }
        .padding(.top, 4)
        .overlay(alignment: .top) { Rectangle().fill(chrome.divider).frame(height: 1) }
        .padding(.top, 16)
    }

    private var visibleFieldToggles: [(title: String, binding: Binding<Bool>)] {
        var cells: [(title: String, binding: Binding<Bool>)] = []

        cells.append(("可用状态徽章", $draft.showsStatus))
        if !content.planName.isEmpty || !content.subtitle.isEmpty {
            cells.append(("套餐规格", $draft.showsSubtitle))
        }
        if content.cycleRemainingText != nil || content.subscriptionStartText != nil || content.subscriptionEndText != nil {
            cells.append(("订阅周期/到期", $draft.showsSubscriptionDates))
        }
        cells.append(("附加备注框", $draft.showsNote))
        if content.metrics.contains(where: { $0.resetBadgeText != nil || !$0.timeDetails.isEmpty }) {
            cells.append(("重置时间与倒计时", $draft.showsResetTimes))
        }

        let progressMetrics = content.metrics.filter { $0.percent != nil }
        let tileMetrics = content.metrics.filter { $0.percent == nil }
        for item in progressMetrics + tileMetrics {
            let id = item.id
            cells.append((item.title, metricVisibleBinding(id)))
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

        cells.append(("快照水印与防伪", $draft.showsWatermark))
        return cells
    }

    private var actionBar: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(spacing: 10) {
                Button(action: copyImage) {
                    Label("复制图片到剪贴板", systemImage: "doc.on.doc")
                        .font(.system(size: 12, weight: .semibold))
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .background(chrome.blue, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .foregroundStyle(.white)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                Button(action: saveImage) {
                    Label("保存 PNG 到本地", systemImage: "square.and.arrow.down")
                        .font(.system(size: 12, weight: .semibold))
                        .frame(maxWidth: .infinity, minHeight: 46)
                        .background(chrome.fieldFill, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 12, style: .continuous)
                                .strokeBorder(chrome.inputBorder, lineWidth: 1)
                        }
                        .foregroundStyle(chrome.textPrimary)
                        .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
            }
            Text("快捷键: ⌘C 复制 · ⌘S 保存")
                .font(.system(size: 10, design: .monospaced))
                .foregroundStyle(chrome.textTertiary)
        }
        .padding(.top, 16)
        .overlay(alignment: .top) { Rectangle().fill(chrome.divider).frame(height: 1) }
    }

    private func editorField(_ title: String, hint: String, text: Binding<String>, hintAccent: Bool = false) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack {
                Text(title)
                    .font(.system(size: 12))
                    .foregroundStyle(chrome.textPrimary)
                Spacer()
                Text(hint)
                    .font(.system(size: 10))
                    .foregroundStyle(hintAccent ? chrome.amber : chrome.textTertiary)
            }
            TextField("", text: text)
                .textFieldStyle(.plain)
                .font(.system(size: 12))
                .foregroundStyle(chrome.textPrimary)
                .padding(.horizontal, 10)
                .padding(.vertical, 8)
                .background(chrome.inputFill, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 8, style: .continuous)
                        .strokeBorder(chrome.inputBorder, lineWidth: 1)
                }
        }
    }

    private func toggleCell(_ title: String, isOn: Binding<Bool>) -> some View {
        Button {
            isOn.wrappedValue.toggle()
        } label: {
            HStack(spacing: 9) {
                Image(systemName: isOn.wrappedValue ? "checkmark.square.fill" : "square")
                    .font(.system(size: 14, weight: .medium))
                    .foregroundStyle(isOn.wrappedValue ? chrome.blue : chrome.textTertiary)
                Text(title)
                    .font(.system(size: 11))
                    .foregroundStyle(chrome.textPrimary)
                    .lineLimit(2)
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
            .padding(.horizontal, 10)
            .padding(.vertical, 12)
            .background(chrome.fieldFill, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 8, style: .continuous)
                    .strokeBorder(chrome.inputBorder.opacity(0.7), lineWidth: 1)
            }
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(title)
        .accessibilityValue(isOn.wrappedValue ? "已显示" : "已隐藏")
        .accessibilityAddTraits(.isButton)
    }

    private func legacyToggleCell(_ title: String, isOn: Binding<Bool>) -> some View {
        Toggle(isOn: isOn) {
            Text(title)
                .font(.system(size: 11))
                .foregroundStyle(chrome.textPrimary)
                .lineLimit(2)
                .frame(maxWidth: .infinity, alignment: .leading)
        }
        .toggleStyle(.checkbox)
        .padding(8)
        .background(chrome.fieldFill, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 8, style: .continuous)
                .strokeBorder(chrome.inputBorder.opacity(0.7), lineWidth: 1)
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
            SettingsWindowActivationPolicy.unregister(window)
        }
    }

    private func applyChrome(_ window: NSWindow) {
        window.title = "分享用量"
        window.hasShadow = true
        window.isMovableByWindowBackground = true
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
