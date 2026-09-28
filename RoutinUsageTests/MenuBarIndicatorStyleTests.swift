import XCTest
@testable import RoutinUsage

final class MenuBarIndicatorStyleTests: XCTestCase {
    func test余额指标提供圆圈和上下余额样式() {
        let metric = NormalizedUsageMetric(
            id: "balance",
            label: "余额",
            value: 100,
            unit: .currency,
            presentation: .balance,
            semantic: .balance
        )

        XCTAssertEqual(
            MenuBarIndicatorStyle.availableStyles(for: metric),
            [.progressBar, .stacked]
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.progressBar.title(for: metric),
            "余额圆圈"
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.stacked.title(for: metric),
            "短码 + 余额"
        )
    }

    func test进度指标提供进度条和短码百分比两种样式() {
        let metric = NormalizedUsageMetric(
            id: "five-hour",
            label: "5 小时",
            used: 60,
            limit: 100,
            remaining: 40,
            unit: .token,
            presentation: .progress,
            semantic: .usedQuota
        )

        XCTAssertEqual(
            MenuBarIndicatorStyle.availableStyles(for: metric),
            [.progressBar, .stacked]
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.stacked.title(for: metric),
            "短码 + 百分比"
        )
    }

    func test状态和缺少指标时只提供状态样式() {
        let status = NormalizedUsageMetric(
            id: "availability",
            label: "账户状态",
            unit: .boolean,
            presentation: .status,
            semantic: .status
        )

        XCTAssertEqual(
            MenuBarIndicatorStyle.availableStyles(for: status),
            [.progressBar]
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.availableStyles(for: nil),
            [.progressBar]
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.progressBar.title(for: status),
            "状态文字"
        )
    }

    func test设置菜单图标跟随当前样式和指标类型() {
        let balance = NormalizedUsageMetric(
            id: "balance",
            label: "余额",
            value: 100,
            unit: .currency,
            presentation: .balance,
            semantic: .balance
        )
        let progress = NormalizedUsageMetric(
            id: "five-hour",
            label: "5 小时",
            used: 60,
            limit: 100,
            remaining: 40,
            unit: .token,
            presentation: .progress,
            semantic: .usedQuota
        )

        XCTAssertEqual(
            MenuBarIndicatorStyle.progressBar.systemImage(for: balance),
            "circle"
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.stacked.systemImage(for: balance),
            "banknote"
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.progressBar.systemImage(for: progress),
            "chart.bar.fill"
        )
        XCTAssertEqual(
            MenuBarIndicatorStyle.stacked.systemImage(for: progress),
            "textformat.123"
        )
    }
}
