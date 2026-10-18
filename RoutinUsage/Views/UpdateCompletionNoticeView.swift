import AppKit
import SwiftUI

/// 更新完成后的版本说明弹窗：展示本次版本、上一版本和可直接阅读的变更摘要。
struct UpdateCompletionNoticeView: View {
    @Environment(\.colorScheme) private var colorScheme

    let notice: UpdateCompletionNotice
    let onOpenFullNotes: () -> Void
    let onDismiss: () -> Void

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
        VStack(alignment: .leading, spacing: 20) {
            header
            releaseSection
            Spacer(minLength: 0)
            actions
        }
        .padding(.horizontal, 30)
        .padding(.vertical, 26)
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
                    Text("已更新到 v\(notice.version)")
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
                .help("在 GitHub 查看完整发布说明")
                .accessibilityLabel("在 GitHub 查看 v\(notice.version) 的完整发布说明")
            }

            Divider()

            notesContent
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
        } else {
            ScrollView(.vertical, showsIndicators: true) {
                LazyVStack(alignment: .leading, spacing: 13) {
                    ForEach(items) { item in
                        summaryRow(item)
                    }
                }
                .padding(.vertical, 2)
            }
            .frame(maxHeight: .infinity)
        }
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
            Spacer(minLength: 0)

            Button("查看完整更新内容", action: onOpenFullNotes)
                .liquidGlassButton()
                .controlSize(.large)
                .accessibilityHint("在 GitHub 打开本次版本的完整发布说明")

            Button("知道了", action: onDismiss)
                .liquidGlassButton(prominent: true)
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
        override func mouseDown(with event: NSEvent) {
            window?.performDrag(with: event)
        }

        override func resetCursorRects() {
            addCursorRect(bounds, cursor: .openHand)
        }
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

    func present(_ notice: UpdateCompletionNotice) {
        if let window {
            window.orderFrontRegardless()
            window.makeKey()
            NSApp.activate(ignoringOtherApps: true)
            return
        }

        let view = UpdateCompletionNoticeView(
            notice: notice,
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
        window.title = "更新完成"
        window.level = .floating
        window.collectionBehavior = [.canJoinAllSpaces, .fullScreenAuxiliary]
        window.hidesOnDeactivate = false
        window.isMovableByWindowBackground = true
        window.isReleasedWhenClosed = false
        window.isRestorable = false
        window.tabbingMode = .disallowed
        window.backgroundColor = .clear
        window.isOpaque = false
        window.hasShadow = true
        window.delegate = self
        window.contentViewController = hostingController
        window.center()
        self.window = window

        SettingsWindowActivationPolicy.register(window)
        window.makeKeyAndOrderFront(nil)
        window.orderFrontRegardless()
        window.makeFirstResponder(window.contentView)
        NSApp.activate(ignoringOtherApps: true)
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
