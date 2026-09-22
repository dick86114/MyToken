import XCTest
@testable import RoutinUsage

final class CompactUsageCardPresentationTests: XCTestCase {
    func test头像取显示名首字() {
        XCTAssertEqual(
            CompactUsageCardPresentation.avatarLetter(displayName: "武", providerID: .glm),
            "武"
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.avatarLetter(displayName: "  洋", providerID: .commandCode),
            "洋"
        )
        XCTAssertFalse(
            CompactUsageCardPresentation.avatarLetter(displayName: "   ", providerID: .routin).isEmpty
        )
    }

    func test指标排布按数量和类型切换() {
        XCTAssertEqual(
            CompactUsageCardPresentation.arrangement(for: [balanceMetric()]),
            .balance
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.arrangement(for: [progressMetric(id: "five-hour", percent: 10)]),
            .horizontalGauges
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.arrangement(for: [
                progressMetric(id: "five-hour", percent: 10),
                progressMetric(id: "weekly", percent: 20)
            ]),
            .horizontalGauges
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.arrangement(for: [
                progressMetric(id: "five-hour", percent: 10),
                progressMetric(id: "weekly", percent: 20),
                progressMetric(id: "monthly", percent: 30)
            ]),
            .verticalGauges
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.arrangement(for: [
                NormalizedUsageMetric(
                    id: "today-token",
                    label: "今日 Token",
                    value: 12,
                    unit: .token,
                    presentation: .value,
                    semantic: .value
                )
            ]),
            .valueTiles
        )
    }

    func test高占用显示即将耗尽且余额不误报() {
        XCTAssertTrue(
            CompactUsageCardPresentation.isNearlyExhausted(metrics: [
                progressMetric(id: "five-hour", percent: 96, health: .critical)
            ])
        )
        XCTAssertFalse(
            CompactUsageCardPresentation.isNearlyExhausted(metrics: [balanceMetric()])
        )
        XCTAssertFalse(
            CompactUsageCardPresentation.isNearlyExhausted(metrics: [
                progressMetric(id: "weekly", percent: 19, health: .normal)
            ])
        )
    }

    func test环形百分比最多一位小数() {
        XCTAssertEqual(CompactUsageCardPresentation.compactPercentText(96), "96%")
        XCTAssertEqual(CompactUsageCardPresentation.compactPercentText(21.34), "21%")
        XCTAssertEqual(CompactUsageCardPresentation.compactPercentText(0), "0%")
        XCTAssertEqual(CompactUsageCardPresentation.compactPercentText(nil), "—")
        XCTAssertEqual(CompactUsageCardPresentation.compactPercentNumberText(96), "96")
        XCTAssertEqual(CompactUsageCardPresentation.compactPercentNumberText(21.34), "21")
        XCTAssertEqual(CompactUsageCardPresentation.compactPercentNumberText(nil), "—")
    }

    func test重置文案当天带前缀跨天只保留日期() {
        let now = date("2026-09-22 17:00:00")
        let sameDay = progressMetric(
            id: "five-hour",
            percent: 21.3,
            windowEnd: date("2026-09-22 17:22:00")
        )
        let nextDay = progressMetric(
            id: "weekly",
            percent: 12.2,
            windowEnd: date("2026-09-28 00:00:00")
        )
        let warning = progressMetric(
            id: "monthly",
            percent: 69.7,
            health: .warning,
            windowEnd: date("2026-09-24 18:29:00")
        )
        let idle = progressMetric(id: "five-hour", percent: 0)

        let zone = TimeZone(secondsFromGMT: 8 * 3600)!
        XCTAssertEqual(
            CompactUsageCardPresentation.subtitle(for: sameDay, now: now, style: .horizontal, timeZone: zone),
            "重置 17:22"
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.subtitle(for: nextDay, now: now, style: .vertical, timeZone: zone),
            "09-28 00:00"
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.subtitle(for: warning, now: now, style: .vertical, timeZone: zone),
            "用量偏高"
        )
        XCTAssertEqual(
            CompactUsageCardPresentation.subtitle(for: idle, now: now, style: .horizontal, timeZone: zone),
            "待命中"
        )
    }

    func test余额徽章按健康状态映射() {
        XCTAssertEqual(CompactUsageCardPresentation.balanceStatusText(healthState: .normal), "充足")
        XCTAssertEqual(CompactUsageCardPresentation.balanceStatusText(healthState: .warning), "偏低")
        XCTAssertEqual(CompactUsageCardPresentation.balanceStatusText(healthState: .critical), "不足")
        XCTAssertEqual(CompactUsageCardPresentation.balanceStatusText(healthState: .unavailable), "不足")
    }

    private func balanceMetric() -> NormalizedUsageMetric {
        NormalizedUsageMetric(
            id: "balance",
            label: "余额",
            value: 26.62,
            unit: .currency,
            presentation: .balance,
            semantic: .balance,
            currencyCode: "CNY",
            healthState: .normal
        )
    }

    private func progressMetric(
        id: String,
        percent: Double,
        health: UsageMetricHealthState = .normal,
        windowEnd: Date? = nil
    ) -> NormalizedUsageMetric {
        let used = Decimal(percent)
        return NormalizedUsageMetric(
            id: id,
            label: id,
            used: used,
            limit: 100,
            remaining: 100 - used,
            unit: .request,
            windowEnd: windowEnd,
            presentation: .progress,
            semantic: .usedQuota,
            healthState: health
        )
    }

    private func date(_ text: String) -> Date {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 8 * 3600)
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.date(from: text)!
    }
}
