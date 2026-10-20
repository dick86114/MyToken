import AppKit
import SwiftUI

enum UpdateNoticePresentation: Equatable, Sendable {
    case updateCompleted
    case currentVersion

    func heading(version: String) -> String {
        switch self {
        case .updateCompleted:
            return "已更新到 v\(version)"
        case .currentVersion:
            return "当前版本 v\(version)"
        }
    }

    var windowTitle: String {
        switch self {
        case .updateCompleted:
            return "更新完成"
        case .currentVersion:
            return "版本更新内容"
        }
    }
}

/// 更新完成后的版本说明弹窗：展示本次版本、上一版本和可直接阅读的变更摘要。
struct UpdateCompletionNoticeView: View {
    @Environment(\.colorScheme) private var colorScheme

    let notice: UpdateCompletionNotice
    let presentation: UpdateNoticePresentation
    let onOpenFullNotes: () -> Void
    let onDismiss: () -> Void

    init(
        notice: UpdateCompletionNotice,
        presentation: UpdateNoticePresentation = .updateCompleted,
        onOpenFullNotes: @escaping () -> Void,
        onDismiss: @escaping () -> Void
    ) {
        self.notice = notice
        self.presentation = presentation
        self.onOpenFullNotes = onOpenFullNotes
        self.onDismiss = onDismiss
    }

    static func preferredHeight(for notice: UpdateCompletionNotice) -> CGFloat {
        let rowCount = UpdateReleaseSummaryBuilder.items(from: notice.notes).count
        let visibleRows = min(max(rowCount, 1), 5)
        return min(500, max(430, 330 + CGFloat(visibleRows) * 30))
    }

    private var items: [UpdateReleaseSummaryItem] {
        UpdateReleaseSummaryBuilder.items(from: notice.notes)
    }

    private var accent: Color {
        CompactPopoverPalette.brand(colorScheme)
    }

    private var mutedText: Color {
        CompactPopoverPalette.subtitle(colorScheme)
    }

    var body: some View {
        ZStack(alignment: .topLeading) {
            UpdateCompletionDragHandle()
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 20) {
                header
                releaseSection
                draggableSpacer
                actions
            }
            .padding(.horizontal, 30)
            .padding(.vertical, 26)
            .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topLeading)
        }
        .frame(width: 640, height: preferredHeight, alignment: .topLeading)
        .liquidGlassWindowBackground()
        .onExitCommand(perform: onDismiss)
        .accessibilityElement(children: .contain)
    }

    private var preferredHeight: CGFloat {
        Self.preferredHeight(for: notice)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(spacing: 14) {
                Image(systemName: "sparkles")
                    .font(.system(size: 27, weight: .semibold))
                    .foregroundStyle(accent)
                    .frame(width: 42, height: 42)
                    .background {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .fill(accent.opacity(colorScheme == .dark ? 0.18 : 0.12))
                    }
                    .overlay {
                        RoundedRectangle(cornerRadius: 12, style: .continuous)
                            .strokeBorder(accent.opacity(0.24), lineWidth: 1)
                    }
                    .accessibilityHidden(true)

                VStack(alignment: .leading, spacing: 5) {
                    Text(presentation.heading(version: notice.version))
                        .font(.system(size: 24, weight: .bold))
                        .foregroundStyle(.primary)
                        .monospacedDigit()

                    Text(previousVersionDescription)
                        .font(.system(size: 15))
                        .foregroundStyle(mutedText)
                }
            }
        }
        .accessibilityElement(children: .combine)
        .overlay {
            UpdateCompletionDragHandle()
                .accessibilityHidden(true)
        }
    }

    private var previousVersionDescription: String {
        guard let previousVersion = notice.previousVersion,
              !previousVersion.isEmpty else {
            return "查看本次版本的更新内容"
        }
        return "自 v\(previousVersion) 以来的更新"
    }

    private var releaseSection: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .firstTextBaseline) {
                Text("v\(notice.version)")
                    .font(.system(size: 18, weight: .bold, design: .monospaced))
                    .monospacedDigit()
                    .foregroundStyle(.primary)

                Spacer(minLength: 12)

                Button(action: onOpenFullNotes) {
                    HStack(spacing: 5) {
                        Text("详情")
                        Image(systemName: "arrow.up.right")
                            .font(.system(size: 11, weight: .semibold))
                    }
                    .font(.system(size: 13, weight: .medium))
                    .foregroundStyle(mutedText)
                    .contentShape(Rectangle())
                }
                .buttonStyle(.plain)
                .pointingHandCursorOnHover()
                .help("在 GitHub 查看完整发布说明")
                .accessibilityLabel("在 GitHub 查看 v\(notice.version) 的完整发布说明")
            }

            Divider()

            notesContent
        }
    }

    private var draggableSpacer: some View {
        Spacer(minLength: 0)
            .overlay {
                UpdateCompletionDragHandle()
                    .accessibilityHidden(true)
            }
    }

    @ViewBuilder
    private var notesContent: some View {
        if items.isEmpty {
            HStack(spacing: 10) {
                Image(systemName: "doc.text")
                    .foregroundStyle(mutedText)
                Text("此版本未提供更新日志")
                    .font(.system(size: 14))
                    .foregroundStyle(mutedText)
            }
            .frame(maxWidth: .infinity, minHeight: 110, alignment: .center)
            .background {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .fill(CompactPopoverPalette.surface(.metric, colorScheme))
            }
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .strokeBorder(CompactPopoverPalette.cardStroke(colorScheme), lineWidth: 1)
            }
            .overlay {
                UpdateCompletionDragHandle()
                    .accessibilityHidden(true)
            }
        } else if items.count <= 5 {
            draggableNotesContent
        } else {
            ScrollView(.vertical, showsIndicators: true) {
                notesList
            }
            .frame(maxHeight: .infinity)
        }
    }

    private var draggableNotesContent: some View {
        notesList
            .overlay {
                UpdateCompletionDragHandle()
                    .accessibilityHidden(true)
            }
    }

    private var notesList: some View {
        LazyVStack(alignment: .leading, spacing: 13) {
            ForEach(items) { item in
                summaryRow(item)
            }
        }
        .padding(.vertical, 2)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func summaryRow(_ item: UpdateReleaseSummaryItem) -> some View {
        let categoryColor = color(for: item.category)

        return HStack(alignment: .firstTextBaseline, spacing: 12) {
            Text(item.category.title)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(categoryColor)
                .padding(.horizontal, 9)
                .padding(.vertical, 4)
                .frame(minWidth: 44)
                .background {
                    Capsule()
                        .fill(categoryColor.opacity(colorScheme == .dark ? 0.16 : 0.10))
                }
                .overlay {
                    Capsule()
                        .strokeBorder(categoryColor.opacity(0.24), lineWidth: 1)
                }

            Text(item.text)
                .font(.system(size: 14))
                .foregroundStyle(.primary)
                .lineSpacing(3)
                .fixedSize(horizontal: false, vertical: true)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(item.category.title)，\(item.text)")
    }

    private func color(for category: UpdateReleaseChangeCategory) -> Color {
        switch category {
        case .added:
            return CompactPopoverPalette.positive(colorScheme)
        case .fixed:
            return CompactPopoverPalette.warningColor
        case .improved:
            return accent
        case .other:
            return mutedText
        }
    }

    private var actions: some View {
        HStack(spacing: 12) {
            draggableSpacer

            Button("查看完整更新内容", action: onOpenFullNotes)
                .liquidGlassButton()
                .pointingHandCursorOnHover()
                .controlSize(.large)
                .accessibilityHint("在 GitHub 打开本次版本的完整发布说明")

            Button("知道了", action: onDismiss)
                .liquidGlassButton(prominent: true)
                .pointingHandCursorOnHover()
                .tint(accent)
                .controlSize(.large)
                .keyboardShortcut(.defaultAction)
        }
    }
}

/// SwiftUI 的整块内容会拦截背景拖拽，这里用 AppKit 视图把标题区域明确变成拖动把手。
private struct UpdateCompletionDragHandle: NSViewRepresentable {
    func makeNSView(context: Context) -> DraggableView {
        DraggableView()
    }

    func updateNSView(_ nsView: DraggableView, context: Context) {}

    final class DraggableView: NSView {
        override func acceptsFirstMouse(for event: NSEvent?) -> Bool {
            true
        }

        override func mouseDown(with event: NSEvent) {
            window?.performDrag(with: event)
        }
    }
}

enum UpdateCompletionWindowPlacement {
    static func centeredOrigin(windowSize: NSSize, visibleFrame: NSRect) -> NSPoint {
        NSPoint(
            x: visibleFrame.midX - windowSize.width / 2,
            y: visibleFrame.midY - windowSize.height / 2
        )
    }
}

/// 无标题栏窗口默认不能成为 key window；更新提示需要键盘确认，因此显式允许。
private final class UpdateCompletionNoticeWindow: NSWindow {
    override var canBecomeKey: Bool { true }
    override var canBecomeMain: Bool { true }
}

/// 更新完成提示必须等新进程完全启动后再展示，因此由菜单栏控制器持有独立窗口。
@MainActor
final class UpdateCompletionNoticeWindowController: NSObject, NSWindowDelegate {
    static let shared = UpdateCompletionNoticeWindowController()

    private var window: NSWindow?

    func present(
        _ notice: UpdateCompletionNotice,
        presentation: UpdateNoticePresentation = .updateCompleted
    ) {
        if let window {
            window.close()
            self.window = nil
        }

        let view = UpdateCompletionNoticeView(
            notice: notice,
            presentation: presentation,
            onOpenFullNotes: {
                NSWorkspace.shared.open(notice.releaseURL)
            },
            onDismiss: { [weak self] in
                self?.close()
            }
        )
        let hostingController = NSHostingController(rootView: view)
        hostingController.sizingOptions = []
        let contentSize = NSSize(
            width: 640,
            height: UpdateCompletionNoticeView.preferredHeight(for: notice)
        )
        let window = UpdateCompletionNoticeWindow(
            contentRect: NSRect(origin: .zero, size: contentSize),
            styleMask: [.borderless],
            backing: .buffered,
            defer: false
        )
        window.title = presentation.windowTitle
        window.level = .floating
        window.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        window.hidesOnDeactivate = false
        window.isMovableByWindowBackground = true
        window.acceptsMouseMovedEvents = true
        window.isReleasedWhenClosed = false
        window.isRestorable = false
        window.tabbingMode = .disallowed
        window.backgroundColor = .clear
        window.isOpaque = false
        window.hasShadow = true
        window.delegate = self
        window.contentViewController = hostingController
        window.setContentSize(contentSize)
        centerWindowOnCurrentScreen(window)
        self.window = window

        SettingsWindowActivationPolicy.register(window)
        window.makeKeyAndOrderFront(nil)
        window.orderFrontRegardless()
        // SwiftUI 首次布局可能调整窗口 frame；下一轮主循环再按最终尺寸居中一次。
        DispatchQueue.main.async { [weak self, weak window] in
            guard let self, let window else { return }
            self.centerWindowOnCurrentScreen(window)
        }
        window.makeFirstResponder(window.contentView)
        NSApp.activate(ignoringOtherApps: true)
    }

    private func centerWindowOnCurrentScreen(_ window: NSWindow) {
        let screen = NSScreen.screens.first { $0.frame.contains(NSEvent.mouseLocation) }
            ?? NSScreen.main
        guard let screen else { return }
        window.setFrameOrigin(
            UpdateCompletionWindowPlacement.centeredOrigin(
                windowSize: window.frame.size,
                visibleFrame: screen.visibleFrame
            )
        )
    }

    func close() {
        window?.close()
    }

    func windowWillClose(_ notification: Notification) {
        if let window = notification.object as? NSWindow {
            SettingsWindowActivationPolicy.unregister(window)
        }
        window = nil
    }
}
