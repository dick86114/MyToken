import AppKit
import XCTest
@testable import RoutinUsage

final class SettingsWindowShellTests: XCTestCase {
    func test新设置窗口包含四个现代导航分类() throws {
        let shell = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "SettingsWindowView.swift"
        ])
        let credentialPage = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "CredentialManagementView.swift"
        ])

        XCTAssertTrue(shell.contains("enum SettingsSection"))
        XCTAssertTrue(shell.contains("case credentials"))
        XCTAssertTrue(shell.contains("case menuBar"))
        XCTAssertTrue(shell.contains("case general"))
        XCTAssertTrue(shell.contains("case help"))
        XCTAssertTrue(shell.contains("liquidGlassWindowBackground()"))
        XCTAssertTrue(credentialPage.contains("SettingsPageHeader"))
        XCTAssertFalse(shell.contains("Routin 签到"))
    }

    func test设置窗口使用品牌侧栏和统一的分组设置组件() throws {
        let shell = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "SettingsWindowView.swift"
        ])
        let sidebar = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "SettingsSidebar.swift"
        ])
        let design = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "SettingsDesignSystem.swift"
        ])

        XCTAssertTrue(shell.contains("SettingsSidebar(selection:"))
        XCTAssertTrue(shell.contains("SettingsDetailBackdrop()"))
        XCTAssertTrue(
            shell.contains(
                ".frame(minWidth: 1120, idealWidth: 1180, minHeight: 640, idealHeight: 720)"
            )
        )
        XCTAssertTrue(sidebar.contains("PopoverColorBrandLogo"))
        XCTAssertTrue(sidebar.contains("lock.shield"))
        XCTAssertTrue(sidebar.contains("accessibilityReduceMotion"))
        XCTAssertTrue(sidebar.contains(".accessibilityAddTraits"))
        XCTAssertTrue(sidebar.contains("ForEach(SettingsSection.allCases)"))
        XCTAssertFalse(sidebar.contains("navigationGroups"))
        XCTAssertFalse(sidebar.contains("RoutinUsageApp.currentVersion"))
        XCTAssertTrue(design.contains("SettingsPageScroll"))
        XCTAssertTrue(design.contains("SettingsGroup"))
        XCTAssertTrue(design.contains("SettingsRow"))
        XCTAssertTrue(design.contains("SettingsDetailBackdrop"))
        XCTAssertTrue(design.contains("CompactPopoverPalette.opaqueWindow(colorScheme)"))
        XCTAssertTrue(design.contains("liquidGlassSurface(cornerRadius:"))
        XCTAssertFalse(design.contains("cardShadowColor"))
    }

    @MainActor
    func test显示设置窗口时切换前台形态并激活应用() {
        var appliedPolicies: [NSApplication.ActivationPolicy] = []
        var activationCount = 0

        SettingsWindowActivationPolicy.apply(
            hasSettingsWindow: true,
            setActivationPolicy: { policy in
                appliedPolicies.append(policy)
                return true
            },
            activate: { activationCount += 1 }
        )

        XCTAssertEqual(appliedPolicies, [.regular])
        XCTAssertEqual(activationCount, 1)
    }

    @MainActor
    func test关闭设置窗口时恢复菜单栏形态且不强制激活() {
        var appliedPolicies: [NSApplication.ActivationPolicy] = []
        var activationCount = 0

        SettingsWindowActivationPolicy.apply(
            hasSettingsWindow: false,
            setActivationPolicy: { policy in
                appliedPolicies.append(policy)
                return true
            },
            activate: { activationCount += 1 }
        )

        XCTAssertEqual(appliedPolicies, [.accessory])
        XCTAssertEqual(activationCount, 0)
    }
}
