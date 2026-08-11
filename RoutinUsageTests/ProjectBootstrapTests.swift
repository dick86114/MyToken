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
        XCTAssertTrue(menuBarLabel.contains("Image(nsImage:"))
        XCTAssertTrue(menuBarLabel.contains("MenuBarVerticalUsageIcon.image"))
        XCTAssertTrue(menuBarLabel.contains("Text(\"·\")"))
        XCTAssertTrue(menuBarLabel.contains("HStack(spacing: 5)"))
        XCTAssertTrue(menuBarLabel.contains(".frame(width: 7, height: 18)"))
    }

    func test弹窗倒计时每分钟刷新并将分组倍率合并为一行() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(usageRowView.contains("TimelineView(.periodic(from: .now, by: 60))"))
        XCTAssertTrue(usageRowView.contains("now: timeline.date"))
        XCTAssertTrue(usageRowView.contains("UsageFormatter.groupMultiplierText"))
        XCTAssertFalse(usageRowView.contains("ForEach(Array(groupMultipliers.enumerated())"))
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

    func test弹窗设置入口直接打开独立设置窗口() throws {
        let usagePopoverView = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertTrue(usagePopoverView.contains("@Environment(\\.openWindow)"))
        XCTAssertTrue(usagePopoverView.contains("openWindow(id: \"settings\")"))
        XCTAssertTrue(usagePopoverView.contains("设置"))
        XCTAssertFalse(usagePopoverView.contains("SettingsLink"))
    }

    private func sourceText(at relativePath: String) throws -> String {
        let resource: (name: String, extension: String?)
        switch relativePath {
        case "project.yml":
            resource = ("project", "yml")
        case "RoutinUsage/App/RoutinUsageApp.swift":
            resource = ("RoutinUsageApp.swift", "txt")
        case "RoutinUsage/Views/SettingsView.swift":
            resource = ("SettingsView.swift", "txt")
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
        default:
            throw CocoaError(.fileNoSuchFile)
        }
        guard let sourceURL = Bundle(for: ProjectBootstrapTests.self)
            .url(forResource: resource.name, withExtension: resource.extension)
        else {
            throw CocoaError(.fileNoSuchFile)
        }
        return try String(contentsOf: sourceURL, encoding: .utf8)
    }
}
