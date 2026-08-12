import XCTest
@testable import RoutinUsage

final class ProjectBootstrapTests: XCTestCase {
    func test应用标识稳定() {
        XCTAssertEqual(RoutinUsageApp.applicationName, "MyRoutin")
    }

    func test菜单栏场景使用可承载总览面板的Window样式() throws {
        let source = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertTrue(source.contains(".menuBarExtraStyle(.window)"))
    }

    func test设置使用独立可缩放窗口场景而不是系统固定设置场景() throws {
        let source = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertTrue(source.contains("Window(\"设置\", id: \"settings\")"))
        XCTAssertFalse(source.contains("Settings {"))
        XCTAssertTrue(source.contains(".windowResizability(.contentMinSize)"))
    }

    func test设置与引导统一使用五小时产品文案() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")
        let onboarding = try sourceText(at: "RoutinUsage/Views/OnboardingView.swift")

        XCTAssertFalse(settings.contains("五小时"))
        XCTAssertFalse(onboarding.contains("五小时"))
        XCTAssertTrue(settings.contains("5 小时"))
        XCTAssertTrue(onboarding.contains("5 小时"))
    }

    func test设置页包含临时查看Key的眼睛按钮与窗口代理() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")
        let keyEditor = try sourceText(at: "RoutinUsage/Views/KeyEditorView.swift")

        XCTAssertTrue(settings.contains("WindowFramePersistence"))
        XCTAssertFalse(settings.contains("@State private var revealedKeyIDs"))
        XCTAssertTrue(keyEditor.contains("@State private var isSecretVisible"))
        XCTAssertTrue(keyEditor.contains("TextField(\"plan-…\""))
        XCTAssertTrue(keyEditor.contains("Image(systemName: isSecretVisible ? \"eye\" : \"eye.slash\")"))
    }

    func test设置页查看状态不从UserDefaults读取() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")

        XCTAssertFalse(settings.contains("UserDefaults"))
    }

    func test设置页与菜单栏视图真实接入三种显示样式() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")
        let menuBarLabel = try sourceText(at: "RoutinUsage/Views/MenuBarLabelView.swift")

        XCTAssertTrue(settings.contains("Picker(\"显示样式\", selection: $settings.menuBarStyle)"))
        XCTAssertTrue(settings.contains("MenuBarStyle.allCases"))
        XCTAssertTrue(menuBarLabel.contains("style: settings.menuBarStyle"))
        XCTAssertTrue(menuBarLabel.contains("MenuBarAliasVerticalUsageIcon.image(alias: alias, percent: metric.percent)"))
        XCTAssertTrue(menuBarLabel.contains("DynamicMenuBarAliasVerticalUsageImageRep"))
        XCTAssertTrue(menuBarLabel.contains("override func draw("))
        XCTAssertFalse(menuBarLabel.contains("HStack(spacing: 0)"))
    }

    func test统一玻璃辅助层使用系统玻璃并保留旧系统材质回退() throws {
        guard let surface = try optionalSourceText(at: "RoutinUsage/Views/LiquidGlassSurface.swift") else {
            XCTFail("缺少统一玻璃辅助层")
            return
        }
        let projectSpec = try sourceText(at: "project.yml")

        XCTAssertTrue(surface.contains("if #available(macOS 26.0, *)"))
        XCTAssertTrue(surface.contains(".glassEffect(.regular.tint(.white.opacity("))
        XCTAssertTrue(surface.contains(".buttonStyle(.glass)"))
        XCTAssertTrue(surface.contains(".buttonStyle(.glassProminent)"))
        XCTAssertTrue(surface.contains(".regularMaterial"))
        XCTAssertTrue(surface.contains("shape.stroke(.white.opacity("))
        XCTAssertTrue(surface.contains(".shadow("))
        XCTAssertTrue(surface.contains("func liquidGlassSurface(cornerRadius: CGFloat = 16)"))
        XCTAssertTrue(surface.contains("func liquidGlassWindowBackground()"))
        XCTAssertTrue(projectSpec.contains("macOS: \"14.0\""))
    }

    func test设置页和用量弹窗使用统一玻璃窗口背景() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertTrue(settings.contains(".liquidGlassWindowBackground()"))
        XCTAssertTrue(popover.contains(".liquidGlassWindowBackground()"))
    }

    func test设置页核心容器和控件全部接入玻璃外观() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")

        XCTAssertTrue(settings.contains(".liquidGlassSurface(cornerRadius: 18)"))
        XCTAssertTrue(settings.contains(".liquidGlassSurface(cornerRadius: 14)"))
        XCTAssertTrue(settings.contains(".liquidGlassControlSurface()"))
        XCTAssertTrue(settings.contains(".liquidGlassButton()"))
        XCTAssertTrue(settings.contains(".liquidGlassProgressSurface()"))
        XCTAssertTrue(settings.contains("TabView {"))
        XCTAssertTrue(settings.contains("List {"))
        XCTAssertGreaterThanOrEqual(
            settings.components(separatedBy: ".liquidGlassControlSurface()").count - 1,
            8
        )
        XCTAssertTrue(settings.contains("Button(role: .destructive)"))
        XCTAssertGreaterThanOrEqual(
            settings.components(separatedBy: "glassSection {").count - 1,
            5
        )
    }

    func test弹窗行工具栏按钮进度和引导卡片接入玻璃外观() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let row = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")
        let onboarding = try sourceText(at: "RoutinUsage/Views/OnboardingView.swift")

        XCTAssertTrue(popover.contains(".liquidGlassSurface(cornerRadius: 14)"))
        XCTAssertTrue(popover.contains(".liquidGlassControlSurface()"))
        XCTAssertTrue(popover.contains(".liquidGlassButton()"))
        XCTAssertTrue(popover.contains(".liquidGlassProgressSurface()"))
        XCTAssertTrue(row.contains(".liquidGlassSurface(cornerRadius: 12)"))
        XCTAssertTrue(row.contains(".liquidGlassProgressSurface()"))
        XCTAssertTrue(onboarding.contains(".liquidGlassSurface(cornerRadius: 24)"))
        XCTAssertTrue(onboarding.contains(".liquidGlassButton(prominent: true)"))
        XCTAssertTrue(onboarding.contains(".liquidGlassWindowBackground()"))
    }

    func test菜单栏别名竖条作为单一辅助功能元素朗读() throws {
        let menuBarLabel = try sourceText(at: "RoutinUsage/Views/MenuBarLabelView.swift")

        XCTAssertTrue(menuBarLabel.contains("MenuBarAliasVerticalUsageIcon.image(alias: alias, percent: metric.percent)"))
        XCTAssertTrue(menuBarLabel.contains("DynamicMenuBarAliasVerticalUsageImageRep"))
        XCTAssertFalse(menuBarLabel.contains("NSAttributedString.Key"))
        XCTAssertFalse(menuBarLabel.contains("CGWindowListCreateImage"))
        XCTAssertFalse(menuBarLabel.contains("desktopImageURL"))
        XCTAssertTrue(menuBarLabel.contains(".accessibilityElement(children: .ignore)"))
        XCTAssertTrue(menuBarLabel.contains(".accessibilityLabel(verticalBarAccessibilityLabel(metric: metric))"))
    }

    func test弹窗倒计时每分钟刷新并将分组倍率合并为一行() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(usageRowView.contains("TimelineView(.periodic(from: .now, by: 60))"))
        XCTAssertTrue(usageRowView.contains("now: timeline.date"))
        XCTAssertTrue(usageRowView.contains("UsageFormatter.groupMultiplierText"))
        XCTAssertFalse(usageRowView.contains("ForEach(Array(groupMultipliers.enumerated())"))
    }

    func test弹窗详情将倒计时并排并在进度条下方右对齐分组倍率() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        let countdownStart = try XCTUnwrap(
            usageRowView.range(of: "HStack(alignment: .firstTextBaseline, spacing: 8)")
        )
        let multiplierStart = try XCTUnwrap(
            usageRowView.range(of: "Text(UsageFormatter.groupMultiplierText(groupMultipliers))")
        )
        let countdowns = usageRowView[countdownStart.lowerBound..<multiplierStart.lowerBound]

        XCTAssertTrue(countdowns.contains("detailLine("))
        XCTAssertTrue(countdowns.contains("\"5 小时剩余\""))
        XCTAssertTrue(countdowns.contains("\"周剩余\""))
        XCTAssertTrue(countdowns.contains("Spacer(minLength: 8)"))
        XCTAssertTrue(usageRowView.contains("HStack {\n                    Spacer()"))
    }

    func test本地Key相关文案不再声称使用系统钥匙串() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")
        let onboarding = try sourceText(at: "RoutinUsage/Views/OnboardingView.swift")

        XCTAssertFalse(settings.contains("将同时删除系统钥匙串中的 Key"))
        XCTAssertFalse(onboarding.contains("Key 仅保存在这台 Mac 的系统钥匙串中"))
    }

    func test工程规格锁定为Xcode15兼容格式() throws {
        let projectSpec = try sourceText(at: "project.yml")

        XCTAssertTrue(projectSpec.contains("projectFormat: xcode15_3"))
    }

    func test工程锁定Xcode15兼容的Swift版本() throws {
        let projectSpec = try sourceText(at: "project.yml")

        XCTAssertTrue(projectSpec.contains("SWIFT_VERSION: \"5.0\""))
    }

    func test发布工作流使用MyRoutin作为版本展示名称() throws {
        let releaseWorkflow = try sourceText(at: ".github/workflows/release.yml")

        XCTAssertTrue(releaseWorkflow.contains("name: MyRoutin v${{ inputs.version }}"))
        XCTAssertFalse(releaseWorkflow.contains("Routin Usage"))
    }

    func test发布工作流要求手动Markdown更新日志() throws {
        let workflow = try sourceText(at: ".github/workflows/release.yml")
        let releaseNotesInput = """
              release_notes:
                description: '发布说明（Markdown）'
                required: true
                type: string
        """

        XCTAssertTrue(workflow.contains(releaseNotesInput))
        XCTAssertTrue(workflow.contains("release_notes:"))
        XCTAssertTrue(workflow.contains("body: ${{ inputs.release_notes }}"))
        XCTAssertTrue(workflow.contains("generate_release_notes: false"))
    }

    func test发布工作流显式选择并校验Xcode26() throws {
        let workflow = try sourceText(at: ".github/workflows/release.yml")

        XCTAssertTrue(
            workflow.contains("DEVELOPER_DIR: /Applications/Xcode_26.3.app/Contents/Developer")
        )
        XCTAssertTrue(workflow.contains("scripts/verify-xcode-26.sh"))
    }

    func test弹窗详情显示剩余时长与配对分组倍率且不显示允许模型() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(usageRowView.contains("\"5 小时剩余\""))
        XCTAssertTrue(usageRowView.contains("\"周剩余\""))
        XCTAssertTrue(usageRowView.contains("UsageFormatter.remainingDurationText"))
        XCTAssertTrue(usageRowView.contains("groupMultipliers"))
        XCTAssertFalse(usageRowView.contains("allowedModels"))
        XCTAssertFalse(usageRowView.contains("允许模型"))
    }

    func test设置详情显示全部按Key配对的分组倍率() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")

        XCTAssertTrue(settings.contains("UsageFormatter.groupMultiplierText(snapshot.groupMultipliers)"))
    }

    func test设置页显示当前版本与完整更新日志() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")

        XCTAssertTrue(settings.contains("当前版本"))
        XCTAssertTrue(settings.contains("UpdateNotesView(notes: update.notes)"))
        XCTAssertFalse(settings.contains(".lineLimit(3)"))
    }

    func test更新日志视图使用Markdown并为无日志版本提供提示() throws {
        let updateNotes = try sourceText(at: "RoutinUsage/Views/UpdateNotesView.swift")

        XCTAssertTrue(updateNotes.contains("AttributedString(markdown:"))
        XCTAssertTrue(updateNotes.contains("此版本未提供更新日志"))
    }

    func test弹窗设置入口直接打开独立设置窗口() throws {
        let usagePopoverView = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertTrue(usagePopoverView.contains("@Environment(\\.openWindow)"))
        XCTAssertTrue(usagePopoverView.contains("openWindow(id: \"settings\")"))
        XCTAssertTrue(usagePopoverView.contains("设置"))
        XCTAssertFalse(usagePopoverView.contains("SettingsLink"))
    }

    private func sourceText(at relativePath: String) throws -> String {
        guard let source = try optionalSourceText(at: relativePath) else {
            throw CocoaError(.fileNoSuchFile)
        }
        return source
    }

    private func optionalSourceText(at relativePath: String) throws -> String? {
        let resource: (name: String, extension: String?)
        switch relativePath {
        case "project.yml":
            resource = ("project", "yml")
        case ".github/workflows/release.yml":
            resource = ("release", "yml")
        case "RoutinUsage/App/RoutinUsageApp.swift":
            resource = ("RoutinUsageApp.swift", "txt")
        case "RoutinUsage/Views/SettingsView.swift":
            resource = ("SettingsView.swift", "txt")
        case "RoutinUsage/Views/UpdateNotesView.swift":
            resource = ("UpdateNotesView.swift", "txt")
        case "RoutinUsage/Views/KeyEditorView.swift":
            resource = ("KeyEditorView.swift", "txt")
        case "RoutinUsage/Views/OnboardingView.swift":
            resource = ("OnboardingView.swift", "txt")
        case "RoutinUsage/Views/UsageRowView.swift":
            resource = ("UsageRowView.swift", "txt")
        case "RoutinUsage/Views/MenuBarLabelView.swift":
            resource = ("MenuBarLabelView.swift", "txt")
        case "RoutinUsage/Views/UsagePopoverView.swift":
            resource = ("UsagePopoverView.swift", "txt")
        case "RoutinUsage/Views/LiquidGlassSurface.swift":
            resource = ("LiquidGlassSurface.swift", "txt")
        default:
            throw CocoaError(.fileNoSuchFile)
        }
        guard let sourceURL = Bundle(for: ProjectBootstrapTests.self)
            .url(forResource: resource.name, withExtension: resource.extension) else { return nil }
        return try String(contentsOf: sourceURL, encoding: .utf8)
    }
}
