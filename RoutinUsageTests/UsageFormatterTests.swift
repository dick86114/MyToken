import AppKit
import Foundation
import XCTest
@testable import RoutinUsage

final class UsageFormatterTests: XCTestCase {
    @MainActor
    func test菜单栏独立竖条图像只承载彩色用量条() {
        let image = MenuBarVerticalUsageIcon.image(percent: 35)

        XCTAssertEqual(image.size.width, 7)
        XCTAssertEqual(image.size.height, 18)
        XCTAssertFalse(image.isTemplate)
    }

    @MainActor
    func test菜单栏Logo进度图标使用品牌轮廓和填充蒙版() {
        let image = MenuBarLogoUsageIcon.image(percent: 35)

        XCTAssertEqual(image.size.width, 18)
        XCTAssertEqual(image.size.height, 18)
        XCTAssertFalse(image.isTemplate)
        XCTAssertNotNil(NSImage(named: "MenuBarLogoOutline"))
        XCTAssertNotNil(NSImage(named: "MenuBarLogoMask"))
    }

    @MainActor
    func test菜单栏Logo轮廓在浅色系统外观保持品牌白边() throws {
        let appearance = try XCTUnwrap(NSAppearance(named: .aqua))
        var brightPixelCount = 0

        appearance.performAsCurrentDrawingAppearance {
            let image = MenuBarLogoUsageIcon.image(percent: 0)
            let representation = try? NSBitmapImageRep(
                data: try XCTUnwrap(image.tiffRepresentation)
            )
            guard let representation else {
                return
            }
            for x in 0..<representation.pixelsWide {
                for y in 0..<representation.pixelsHigh {
                    guard let color = representation.colorAt(x: x, y: y)?
                        .usingColorSpace(.deviceRGB) else {
                        continue
                    }
                    if color.redComponent > 0.7,
                       color.greenComponent > 0.7,
                       color.blueComponent > 0.7 {
                        brightPixelCount += 1
                    }
                }
            }
        }

        XCTAssertGreaterThan(brightPixelCount, 50)
    }

    @MainActor
    func test菜单栏Logo进度图标在有用量时绘制彩色填充() throws {
        let image = MenuBarLogoUsageIcon.image(percent: 35)
        let representation = try XCTUnwrap(
            NSBitmapImageRep(data: try XCTUnwrap(image.tiffRepresentation))
        )
        var coloredPixelCount = 0

        for x in 0..<representation.pixelsWide {
            for y in 0..<representation.pixelsHigh {
                guard let color = representation.colorAt(x: x, y: y)?
                    .usingColorSpace(.deviceRGB) else {
                    continue
                }
                if color.greenComponent > color.redComponent * 1.2,
                   color.greenComponent > color.blueComponent * 1.2 {
                    coloredPixelCount += 1
                }
            }
        }

        XCTAssertGreaterThan(coloredPixelCount, 130)
    }

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

    func test菜单栏别名超长时截断为前五个字符加省略号() {
        XCTAssertEqual(UsageFormatter.truncatedMenuBarAlias("主账号名称很长"), "主账号名称…")
        XCTAssertEqual(UsageFormatter.truncatedMenuBarAlias("主账号"), "主账号")
    }

    func test菜单栏别名百分比样式返回别名和百分比() {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))

        XCTAssertEqual(
            UsageFormatter.menuBarText(
                state: state,
                dimension: .fiveHour,
                style: .aliasPercent
            ),
            "主账号 68%"
        )
    }

    func test菜单栏别名竖线样式有效时返回别名加载时返回省略号() {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))
        XCTAssertEqual(
            UsageFormatter.menuBarText(
                state: state,
                dimension: .fiveHour,
                style: .aliasVerticalBar
            ),
            "主账号"
        )

        let loadingState = makeState(snapshot: nil, isRefreshing: true)
        XCTAssertEqual(
            UsageFormatter.menuBarText(
                state: loadingState,
                dimension: .fiveHour,
                style: .aliasVerticalBar
            ),
            "…"
        )
    }

    func test菜单栏别名Logo进度样式有效时返回别名加载时返回省略号() {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))
        XCTAssertEqual(
            UsageFormatter.menuBarText(
                state: state,
                dimension: .fiveHour,
                style: .aliasLogoProgress
            ),
            "主账号"
        )

        let loadingState = makeState(snapshot: nil, isRefreshing: true)
        XCTAssertEqual(
            UsageFormatter.menuBarText(
                state: loadingState,
                dimension: .fiveHour,
                style: .aliasLogoProgress
            ),
            "…"
        )
    }

    func test菜单栏仅Logo进度样式有效时不显示别名() {
        let state = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))

        XCTAssertEqual(
            UsageFormatter.menuBarText(
                state: state,
                dimension: .fiveHour,
                style: .logoProgress
            ),
            ""
        )
    }

    func test菜单栏别名竖线样式资源包仍返回Token百分比() {
        let state = makeState(snapshot: makeTokenSnapshot(percent: 92.4))

        XCTAssertEqual(
            UsageFormatter.menuBarText(
                state: state,
                dimension: .weekly,
                style: .aliasVerticalBar
            ),
            "92%"
        )
    }

    func test菜单栏竖条风险等级在50和80分界() {
        XCTAssertEqual(MenuBarUsageRisk.level(for: 49.9), .normal)
        XCTAssertEqual(MenuBarUsageRisk.level(for: 50), .warning)
        XCTAssertEqual(MenuBarUsageRisk.level(for: 79.9), .warning)
        XCTAssertEqual(MenuBarUsageRisk.level(for: 80), .critical)
    }

    func test菜单栏为竖条和Logo进度样式提供有效周期指标() throws {
        let periodicState = makeState(snapshot: makePeriodicSnapshot(fiveHourPercent: 67.5))

        XCTAssertEqual(
            try XCTUnwrap(
                MenuBarVerticalUsage.metric(
                    state: periodicState,
                    dimension: .fiveHour,
                    style: .aliasVerticalBar
                )
            ).percent,
            67.5
        )
        XCTAssertEqual(
            try XCTUnwrap(
                MenuBarVerticalUsage.metric(
                    state: periodicState,
                    dimension: .fiveHour,
                    style: .logoProgress
                )
            ).percent,
            67.5
        )
        XCTAssertNil(
            MenuBarVerticalUsage.metric(
                state: periodicState,
                dimension: .fiveHour,
                style: .percent
            )
        )
        XCTAssertEqual(
            try XCTUnwrap(
                MenuBarVerticalUsage.metric(
                    state: periodicState,
                    dimension: .fiveHour,
                    style: .aliasLogoProgress
                )
            ).percent,
            67.5
        )
        XCTAssertNil(
            MenuBarVerticalUsage.metric(
                state: makeState(snapshot: nil, isRefreshing: true),
                dimension: .fiveHour,
                style: .aliasVerticalBar
            )
        )
        XCTAssertNil(
            MenuBarVerticalUsage.metric(
                state: makeState(snapshot: nil, error: .network),
                dimension: .fiveHour,
                style: .aliasVerticalBar
            )
        )
        XCTAssertNil(
            MenuBarVerticalUsage.metric(
                state: makeState(snapshot: makeTokenSnapshot(percent: 92.4)),
                dimension: .weekly,
                style: .aliasVerticalBar
            )
        )
    }

    func test分组倍率合并为一行() {
        XCTAssertEqual(
            UsageFormatter.groupMultiplierText([
                UsageGroupMultiplier(name: "Codex", multiplier: 1),
                UsageGroupMultiplier(name: "Codex Pro", multiplier: 2),
            ]),
            "Codex ×1、Codex Pro ×2"
        )
    }

    func test倒计时在一分钟后使用新时刻更新文本() {
        let now = Date(timeIntervalSince1970: 1_786_320_000)
        let end = now.addingTimeInterval(3 * 60 * 60 + 45 * 60)

        XCTAssertEqual(UsageFormatter.remainingDurationText(until: end, now: now), "3h45m")
        XCTAssertEqual(
            UsageFormatter.remainingDurationText(until: end, now: now.addingTimeInterval(60)),
            "3h44m"
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

    func test缓存仍新鲜但刷新失败时说明暂显示上次数据() {
        let state = makeState(
            snapshot: makePeriodicSnapshot(fiveHourPercent: 81.2),
            error: .network
        )

        XCTAssertEqual(
            UsageFormatter.statusText(state: state),
            "刷新失败，暂显示上次数据：网络错误，将自动重试"
        )
    }

    func test刷新进行中优先说明当前显示上次数据() {
        let state = makeState(
            snapshot: makePeriodicSnapshot(fiveHourPercent: 81.2),
            isRefreshing: true,
            isStale: true,
            error: .network
        )

        XCTAssertEqual(
            UsageFormatter.statusText(state: state),
            "正在刷新，当前显示上次数据"
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

    func test重置时间在同一天使用显式时区仅显示时分() {
        let 时区 = TimeZone(secondsFromGMT: 8 * 60 * 60)!
        let now = Date(timeIntervalSince1970: 1_786_320_000)
        let metric = makeMetric(
            used: 1,
            limit: 10,
            remaining: 9,
            unit: .usd,
            windowEnd: Date(timeIntervalSince1970: 1_786_341_600)
        )

        XCTAssertEqual(UsageFormatter.resetTime(metric, now: now, timeZone: 时区), "14:00")
    }

    func test重置时间跨日时显示日期和时分() {
        let 时区 = TimeZone(secondsFromGMT: 8 * 60 * 60)!
        let now = Date(timeIntervalSince1970: 1_786_320_000)
        let metric = makeMetric(
            used: 1,
            limit: 10,
            remaining: 9,
            unit: .usd,
            windowEnd: Date(timeIntervalSince1970: 1_786_406_400)
        )

        XCTAssertEqual(
            UsageFormatter.resetTime(metric, now: now, timeZone: 时区),
            "08-11 08:00"
        )
    }

    func test缺少重置时间显示双短横线() {
        let metric = makeMetric(used: 1, limit: 10, remaining: 9, unit: .usd)

        XCTAssertEqual(UsageFormatter.resetTime(metric), "--")
    }

    func test剩余时长在一天内显示小时和分钟() {
        let now = Date(timeIntervalSince1970: 1_786_320_000)
        let interval: TimeInterval = 13_500
        let end = now.addingTimeInterval(interval)

        XCTAssertEqual(
            UsageFormatter.remainingDurationText(until: end, now: now),
            "3h45m"
        )
    }

    func test剩余时长跨天显示天小时和分钟() {
        let now = Date(timeIntervalSince1970: 1_786_320_000)
        let interval: TimeInterval = 359_100
        let end = now.addingTimeInterval(interval)

        XCTAssertEqual(
            UsageFormatter.remainingDurationText(until: end, now: now),
            "4d3h45m"
        )
    }

    func test剩余时长在结束或过期时显示已结束() {
        let now = Date(timeIntervalSince1970: 1_786_320_000)

        XCTAssertEqual(
            UsageFormatter.remainingDurationText(until: now, now: now),
            "已结束"
        )
        XCTAssertEqual(
            UsageFormatter.remainingDurationText(
                until: now.addingTimeInterval(-60),
                now: now
            ),
            "已结束"
        )
    }

    func test剩余时长按绝对时间计算而不受时区影响() {
        let now = Date(timeIntervalSince1970: 1_786_320_000)
        let end = Date(timeIntervalSince1970: 1_786_333_500)

        XCTAssertEqual(
            UsageFormatter.remainingDurationText(until: end, now: now),
            "3h45m"
        )
    }

    func test完整时间使用统一的本地日期格式() {
        let 时区 = TimeZone(secondsFromGMT: 8 * 60 * 60)!

        XCTAssertEqual(
            UsageFormatter.fullDateTime(
                Date(timeIntervalSince1970: 1_786_341_600),
                timeZone: 时区
            ),
            "2026-08-10 14:00:00"
        )
        XCTAssertEqual(UsageFormatter.fullDateTime(nil), "—")
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

        XCTAssertEqual(UsageFormatter.statusText(state: state), "网络错误，将自动重试")
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
            "当前，主账号，已使用 68%，$6.80 / $10.00，5 小时剩余 —，周剩余 —"
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
            "主账号，已使用 68%，$6.80 / $10.00，5 小时剩余 —，周剩余 —"
        )
        XCTAssertEqual(
            UsageRowAccessibility.hint(isSelected: false),
            "点击后设为菜单栏当前 Key"
        )
    }

    func test周期套餐可访问性标签包含两个倒计时与分组倍率() throws {
        let now = Date(timeIntervalSince1970: 1_786_320_000)
        let snapshot = UsageSnapshot(
            planName: "Pro",
            kind: .periodic,
            fiveHour: makeMetric(
                used: 6.8,
                limit: 10,
                remaining: 3.2,
                unit: .usd,
                windowEnd: now.addingTimeInterval(3 * 60 * 60 + 45 * 60)
            ),
            weekly: makeMetric(
                used: 20,
                limit: 50,
                remaining: 30,
                percent: 40,
                unit: .usd,
                windowEnd: now.addingTimeInterval(2 * 24 * 60 * 60 + 15 * 60)
            ),
            token: nil,
            allowedModels: [],
            fetchedAt: now,
            groupMultipliers: [
                UsageGroupMultiplier(name: "Codex", multiplier: 1),
                UsageGroupMultiplier(name: "Codex Pro", multiplier: 2),
            ]
        )
        let state = makeState(snapshot: snapshot)
        let metric = try XCTUnwrap(snapshot.fiveHour)

        XCTAssertEqual(
            UsageRowAccessibility.label(
                state: state,
                metric: metric,
                dimension: .fiveHour,
                isSelected: false,
                now: now
            ),
            "主账号，已使用 68%，$6.80 / $10.00，5 小时剩余 3h45m，周剩余 2d0h15m，Codex ×1、Codex Pro ×2"
        )
    }

    func testToken资源包可访问性标签朗读倍率但省略周期倒计时() throws {
        let snapshot = UsageSnapshot(
            planName: "资源包",
            kind: .tokenPack,
            fiveHour: nil,
            weekly: nil,
            token: makeMetric(
                used: 9_200_000,
                limit: 10_000_000,
                remaining: 800_000,
                percent: 92.4,
                unit: .token
            ),
            allowedModels: [],
            fetchedAt: Date(timeIntervalSince1970: 1_786_320_000),
            groupMultipliers: [UsageGroupMultiplier(name: "Fast", multiplier: 1.5)]
        )
        let state = makeState(snapshot: snapshot)
        let metric = try XCTUnwrap(snapshot.token)

        let label = UsageRowAccessibility.label(
            state: state,
            metric: metric,
            dimension: .fiveHour,
            isSelected: false
        )

        XCTAssertTrue(label.contains("Fast ×1.5"))
        XCTAssertFalse(label.contains("5 小时剩余"))
        XCTAssertFalse(label.contains("周剩余"))
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
