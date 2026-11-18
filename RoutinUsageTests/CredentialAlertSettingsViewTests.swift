import XCTest
@testable import RoutinUsage

@MainActor
final class CredentialAlertSettingsViewTests: XCTestCase {
    func test凭证提醒设置默认开启并只展示可预警指标() {
        let usedMetric = NormalizedUsageMetric(
            id: "weekly",
            label: "周用量",
            used: 40,
            limit: 100,
            remaining: 60,
            unit: .token,
            presentation: .progress,
            semantic: .usedQuota
        )
        let valueMetric = NormalizedUsageMetric(
            id: "requests",
            label: "请求次数",
            value: 20,
            unit: .request,
            presentation: .value,
            semantic: .value
        )
        let capabilities = [
            UsageMetricCapability(
                metricID: "weekly",
                label: "周用量",
                presentation: .progress,
                semantic: .usedQuota,
                isMenuBarSelectable: true,
                menuBarPriority: 0,
                defaultAlertEnabled: true,
                defaultAbsoluteAlertThreshold: nil
            ),
            UsageMetricCapability(
                metricID: "requests",
                label: "请求次数",
                presentation: .value,
                semantic: .value,
                isMenuBarSelectable: false,
                menuBarPriority: nil,
                defaultAlertEnabled: false,
                defaultAbsoluteAlertThreshold: nil
            )
        ]
        var saved: CredentialUsagePreferences?
        let model = CredentialAlertSettingsModel(
            credentialID: UUID(),
            preferences: .defaultValue,
            metrics: [usedMetric, valueMetric],
            capabilities: capabilities
        ) { saved = $0 }

        XCTAssertTrue(model.notificationsEnabled)
        XCTAssertEqual(model.rules.map(\.metricID), [usedMetric.id])
        XCTAssertNil(saved)

        var rule = try! XCTUnwrap(model.rules.first)
        rule.isEnabled = false
        model.updateRule(rule)

        XCTAssertFalse(model.preferences.alertRules.first?.isEnabled ?? true)
        XCTAssertFalse(saved?.alertRules.first?.isEnabled ?? true)
    }

    func test凭证提醒设置窗口右上角提供关闭按钮() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("Settings")
                .appendingPathComponent("CredentialAlertSettingsView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("let onClose: () -> Void"))
        XCTAssertTrue(source.contains("SettingsModalTitle("))
        XCTAssertTrue(source.contains("SettingsCloseButton(action: onClose)"))
        XCTAssertTrue(source.contains("Spacer()"))
        XCTAssertTrue(source.contains("action: onClose"))
        XCTAssertFalse(source.contains("Button(\"关闭\")"))
        XCTAssertTrue(source.contains("LazyVGrid("))
        XCTAssertTrue(source.contains(".frame(width: 680)"))
        XCTAssertFalse(source.contains("Form {"))
        XCTAssertTrue(source.contains("ViewThatFits(in: .vertical)"))
        XCTAssertTrue(source.contains(".fixedSize(horizontal: false, vertical: true)"))
        XCTAssertTrue(source.contains("ScrollView(.vertical, showsIndicators: false)"))
    }

    func test提醒总开关关闭后隐藏规则并解释恢复方式() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "CredentialAlertSettingsView.swift"
        ])

        XCTAssertTrue(source.contains("if model.notificationsEnabled"))
        XCTAssertTrue(source.contains("提醒已关闭"))
        XCTAssertTrue(source.contains("暂无可提醒的指标"))
        XCTAssertTrue(source.contains("开启总开关后"))
        XCTAssertTrue(source.contains("Slider("))
        XCTAssertTrue(source.contains("percentSliderBinding"))
        XCTAssertFalse(source.contains("Stepper("))
    }
}
