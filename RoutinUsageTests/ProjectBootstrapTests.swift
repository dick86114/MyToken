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

        XCTAssertTrue(settings.contains("eye.slash"))
        XCTAssertTrue(settings.contains("WindowFramePersistence"))
        XCTAssertTrue(settings.contains("@State private var revealedKeyIDs"))
    }

    func test设置页查看状态不从UserDefaults读取() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/SettingsView.swift")

        XCTAssertFalse(settings.contains("revealedKeyIDs = State(initialValue:"))
        XCTAssertFalse(settings.contains("UserDefaults"))
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

        XCTAssertTrue(usageRowView.contains("detailLine(\"5 小时剩余\""))
        XCTAssertTrue(usageRowView.contains("detailLine(\"周剩余\""))
        XCTAssertTrue(usageRowView.contains("remainingDurationText(until:"))
        XCTAssertTrue(usageRowView.contains("groupMultipliers"))
        XCTAssertFalse(usageRowView.contains("allowedModels"))
        XCTAssertFalse(usageRowView.contains("允许模型"))
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
        case "RoutinUsage/Views/OnboardingView.swift":
            resource = ("OnboardingView.swift", "txt")
        case "RoutinUsage/Views/UsageRowView.swift":
            resource = ("UsageRowView.swift", "txt")
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
