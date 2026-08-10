import Foundation
import XCTest
@testable import RoutinUsage

final class UsageFormatterTests: XCTestCase {
    func test菜单栏把百分比四舍五入为整数() {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .fiveHour),
            "68%"
        )
    }

    func test首次加载显示省略号() {
        let state = makeState(snapshot: nil, isRefreshing: true)

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .fiveHour),
            "…"
        )
    }

    func test无订阅显示双短横线() {
        let state = makeState(snapshot: nil, error: .noSubscription)

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .fiveHour),
            "--"
        )
    }

    func test无缓存请求错误显示感叹号() {
        let state = makeState(snapshot: nil, error: .network)

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .fiveHour),
            "!"
        )
    }

    func test资源包在周维度仍显示Token百分比() {
        let state = makeState(snapshot: makeTokenSnapshot(percent: 92.4))

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .weekly),
            "92%"
        )
    }

    func test存在缓存且刷新失败继续显示缓存百分比() {
        let state = makeState(
            snapshot: makePeriodicSnapshot(fiveHourPercent: 81.2),
            isStale: true,
            error: .network
        )

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .fiveHour),
            "81%"
        )
    }

    func test美元金额固定显示两位小数() {
        let metric = makeMetric(used: 6.8, limit: 10, remaining: 3.2, unit: .usd)

        XCTAssertEqual(UsageFormatter.amount(metric), "$6.80 / $10.00")
        XCTAssertEqual(UsageFormatter.remaining(metric), "$3.20")
    }

    func testToken金额使用紧凑缩写且去除无意义小数() {
        let metric = makeMetric(
            used: 9_200_000,
            limit: 10_000_000,
            remaining: 800_000,
            unit: .token
        )

        XCTAssertEqual(UsageFormatter.amount(metric), "9.2M / 10M")
        XCTAssertEqual(UsageFormatter.remaining(metric), "800K")
        XCTAssertEqual(
            UsageFormatter.fullAmount(metric),
            "9,200,000 / 10,000,000 Token"
        )
    }

    func test重置时间使用当前时区() {
        let 原时区 = NSTimeZone.default
        NSTimeZone.default = TimeZone(secondsFromGMT: 8 * 60 * 60)!
        defer { NSTimeZone.default = 原时区 }
        let metric = makeMetric(
            used: 1,
            limit: 10,
            remaining: 9,
            unit: .usd,
            windowEnd: Date(timeIntervalSince1970: 1_786_341_600)
        )

        XCTAssertEqual(UsageFormatter.resetTime(metric), "14:00")
    }

    func test缺少重置时间显示双短横线() {
        let metric = makeMetric(used: 1, limit: 10, remaining: 9, unit: .usd)

        XCTAssertEqual(UsageFormatter.resetTime(metric), "--")
    }

    func test过期缓存同时说明具体请求错误() {
        let state = makeState(
            snapshot: makePeriodicSnapshot(fiveHourPercent: 68),
            isStale: true,
            error: .invalidKey
        )

        XCTAssertEqual(
            UsageFormatter.statusText(state: state),
            "缓存已过期，Key 无效"
        )
    }

    func test无缓存网络错误提供可操作文案() {
        let state = makeState(snapshot: nil, error: .network)

        XCTAssertEqual(UsageFormatter.statusText(state: state), "网络错误，请刷新重试")
    }

    func test非有限百分比显示安全错误状态() {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: .nan))

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .fiveHour),
            "!"
        )
        XCTAssertEqual(
            UsageFormatter.statusText(state: state, dimension: .fiveHour),
            "用量数据异常"
        )
    }

    func test超出整数范围的百分比显示安全错误状态() {
        let state = makeState(
            snapshot: makePeriodicSnapshot(fiveHourPercent: .greatestFiniteMagnitude)
        )

        XCTAssertEqual(
            UsageFormatter.menuBarText(state: state, dimension: .fiveHour),
            "!"
        )
        XCTAssertEqual(
            UsageFormatter.statusText(state: state, dimension: .fiveHour),
            "用量数据异常"
        )
    }

    func test当前Key的可访问性标签和提示明确表达选中状态() throws {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))
        let metric = try XCTUnwrap(state.snapshot?.fiveHour)

        XCTAssertEqual(
            UsageRowAccessibility.label(
                state: state,
                metric: metric,
                dimension: .fiveHour,
                isSelected: true
            ),
            "当前，主账号，已使用 68%，$6.80 / $10.00"
        )
        XCTAssertEqual(
            UsageRowAccessibility.hint(isSelected: true),
            "已是菜单栏当前 Key"
        )
    }

    func test非当前Key保留设为当前的可访问性提示() throws {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))
        let metric = try XCTUnwrap(state.snapshot?.fiveHour)

        XCTAssertEqual(
            UsageRowAccessibility.label(
                state: state,
                metric: metric,
                dimension: .fiveHour,
                isSelected: false
            ),
            "主账号，已使用 68%，$6.80 / $10.00"
        )
        XCTAssertEqual(
            UsageRowAccessibility.hint(isSelected: false),
            "点击后设为菜单栏当前 Key"
        )
    }
}

private extension UsageFormatterTests {
    func makeState(
        snapshot: UsageSnapshot?,
        isRefreshing: Bool = false,
        isStale: Bool = false,
        error: UsageDisplayError? = nil
    ) -> KeyUsageState {
        KeyUsageState(
            configuration: KeyConfiguration(
                id: UUID(uuidString: "00000000-0000-0000-0000-000000000001")!,
                name: "主账号",
                keySuffix: "8F2A",
                sortOrder: 0
            ),
            snapshot: snapshot,
            lastSuccessAt: snapshot?.fetchedAt,
            isRefreshing: isRefreshing,
            isStale: isStale,
            error: error
        )
    }

    func makePeriodicSnapshot(fiveHourPercent: Double) -> UsageSnapshot {
        UsageSnapshot(
            planName: "Pro",
            kind: .periodic,
            fiveHour: makeMetric(
                used: 6.8,
                limit: 10,
                remaining: 3.2,
                percent: fiveHourPercent,
                unit: .usd
            ),
            weekly: makeMetric(
                used: 20,
                limit: 50,
                remaining: 30,
                percent: 40,
                unit: .usd
            ),
            token: nil,
            allowedModels: [],
            fetchedAt: Date(timeIntervalSince1970: 1_786_370_400)
        )
    }

    func makeTokenSnapshot(percent: Double) -> UsageSnapshot {
        UsageSnapshot(
            planName: "资源包",
            kind: .tokenPack,
            fiveHour: nil,
            weekly: nil,
            token: makeMetric(
                used: 9_200_000,
                limit: 10_000_000,
                remaining: 800_000,
                percent: percent,
                unit: .token
            ),
            allowedModels: [],
            fetchedAt: Date(timeIntervalSince1970: 1_786_370_400)
        )
    }

    func makeMetric(
        used: Decimal,
        limit: Decimal,
        remaining: Decimal,
        percent: Double = 68,
        unit: UsageUnit,
        windowEnd: Date? = nil
    ) -> UsageMetric {
        UsageMetric(
            used: used,
            limit: limit,
            remaining: remaining,
            percent: percent,
            unit: unit,
            windowEnd: windowEnd
        )
    }
}
