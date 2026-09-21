import XCTest
@testable import RoutinUsage

final class UsageShareCardTests: XCTestCase {
    private let timeZone = TimeZone(identifier: "Asia/Shanghai")!

    func test深浅票根使用不同标题和图标() {
        XCTAssertEqual(UsageShareTemplate.ticket.title, "深色票根")
        XCTAssertEqual(UsageShareTemplate.ticketLight.title, "浅色票根")

        let source = try? TestSourceReader.read([
            "RoutinUsage", "Views", "UsageShareEditorView.swift"
        ])
        XCTAssertNotNil(source)
        XCTAssertTrue(source?.contains("case .ticket: return \"ticket.fill\"") == true)
        XCTAssertTrue(source?.contains("case .ticketLight: return \"ticket\"") == true)
    }

    func test票根使用真实镂空遮罩() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsageShareCardView.swift"
        ])

        XCTAssertTrue(source.contains("ticketCutoutMask"))
        XCTAssertTrue(source.contains("private static let perforationY: CGFloat = 170"))
        XCTAssertFalse(source.contains("@State private var perforationY"))
        XCTAssertTrue(source.contains("perforationY: Self.perforationY"))
        XCTAssertFalse(source.contains("private func cutoutCircle"))
    }

    func test周期卡片把全部用量字段带入分享内容() throws {
        let now = date(2026, 9, 20, 21, 40)
        let start = date(2026, 9, 1, 0, 0)
        let end = date(2026, 10, 1, 0, 0)
        let fiveHourEnd = now.addingTimeInterval((2 * 60 + 18) * 60)
        let weeklyEnd = now.addingTimeInterval((3 * 24 + 11) * 60 * 60)
        let state = makeState(
            name: "工作机",
            snapshot: UsageSnapshot(
                planName: "Plus",
                kind: .periodic,
                fiveHour: UsageMetric(used: 7.60, limit: 20, remaining: 12.40, percent: 38, unit: .usd, windowEnd: fiveHourEnd),
                weekly: UsageMetric(used: 82, limit: 200, remaining: 118, percent: 41, unit: .usd, windowEnd: weeklyEnd),
                token: UsageMetric(used: 1_200_000, limit: 3_000_000, remaining: 1_800_000, percent: 40, unit: .token, windowEnd: nil),
                allowedModels: [],
                fetchedAt: now,
                groupMultipliers: [UsageGroupMultiplier(name: "default", multiplier: 1)],
                subscriptionStartAt: start,
                subscriptionEndAt: end
            )
        )
        let detection = CodexGroupDetectionRecord(
            keyID: state.configuration.id,
            accountFingerprint: "abc",
            accountDisplayName: "工作机",
            groupName: "default",
            detectedAt: now
        )

        let content = UsageShareContentBuilder.build(
            state: state,
            detectionRecord: detection,
            now: now,
            timeZone: timeZone
        )

        let built = try XCTUnwrap(content)
        XCTAssertEqual(built.displayName, "工作机")
        XCTAssertEqual(built.subtitle, "Routin · Plus")
        XCTAssertEqual(built.subscriptionStartText, "2026-09-01 00:00")
        XCTAssertEqual(built.subscriptionEndText, "2026-10-01 00:00")
        XCTAssertEqual(built.tokenPercentText, "40%")
        XCTAssertEqual(built.groupMultiplierText, "default ×1")
        XCTAssertEqual(built.detectionText, "Codex 当前分组：default")
        XCTAssertEqual(built.metrics.map(\.id), ["fiveHour", "weekly", "token"])
        XCTAssertEqual(built.metrics[0].title, "5 小时")
        XCTAssertEqual(built.metrics[0].headline, "38%")
        XCTAssertEqual(built.metrics[0].amountDetails[0], "已用 $7.60 / $20.00")
        XCTAssertEqual(built.metrics[0].amountDetails[1], "剩余 $12.40")
        XCTAssertTrue(built.metrics[0].timeDetails.contains("剩余 2小时 18分钟"))
        XCTAssertEqual(built.metrics[1].headline, "41%")
        XCTAssertEqual(built.metrics[1].amountDetails[0], "已用 $82.00 / $200.00")
        XCTAssertEqual(built.capturedAtText, "2026.09.20 21:40")
    }

    func test余额卡使用货币文案而不是假进度() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "DeepSeek",
            providerID: .deepseek,
            snapshot: UsageSnapshot(
                planName: "API 余额",
                kind: .periodic,
                fiveHour: nil,
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: now,
                metrics: [
                    NormalizedUsageMetric(
                        id: "balance",
                        label: "余额",
                        value: Decimal(string: "12.36")!,
                        unit: .currency,
                        presentation: .balance,
                        semantic: .balance,
                        currencyCode: "CNY",
                        healthState: .normal
                    ),
                    NormalizedUsageMetric(
                        id: "availability",
                        label: "状态",
                        value: 1,
                        unit: .boolean,
                        presentation: .status,
                        semantic: .status,
                        healthState: .normal
                    )
                ]
            )
        )

        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )

        XCTAssertEqual(content.metrics.map(\.id), ["balance", "availability"])
        XCTAssertEqual(content.metrics[0].headline, "¥12.36")
        XCTAssertNil(content.metrics[0].percent)
        XCTAssertEqual(content.metrics[1].headline, "可用")
    }

    func testGLM分享图不补充卡片未展示的金额() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "GLM",
            providerID: .glm,
            snapshot: UsageSnapshot(
                planName: "Coding Plan",
                kind: .periodic,
                fiveHour: nil,
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: now,
                metrics: [
                    NormalizedUsageMetric(
                        id: "fiveHour",
                        label: "5 小时",
                        used: 20,
                        limit: 100,
                        remaining: 80,
                        unit: .token,
                        windowEnd: now.addingTimeInterval(3600),
                        presentation: .progress,
                        semantic: .usedQuota
                    )
                ]
            )
        )

        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        XCTAssertEqual(content.metrics[0].headline, "20%")
        XCTAssertTrue(content.metrics[0].amountDetails.isEmpty)
        XCTAssertFalse(content.metrics[0].timeDetails.isEmpty)
    }

    func test草稿只改展示不改真实用量数字() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "工作机",
            snapshot: UsageSnapshot(
                planName: "Plus",
                kind: .periodic,
                fiveHour: UsageMetric(used: 7.60, limit: 20, remaining: 12.40, percent: 38, unit: .usd, windowEnd: nil),
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: now
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        var draft = UsageShareDraft.make(from: content)
        XCTAssertEqual(UsageShareTemplate.allCases.first, .ticket)
        XCTAssertEqual(draft.template, .ticket)
        draft.displayName = "对外别名"
        draft.note = "今天又肝完一轮"
        draft.showsAmounts = false

        let rendered = UsageShareContentBuilder.render(content: content, draft: draft)
        XCTAssertEqual(rendered.displayName, "对外别名")
        XCTAssertEqual(rendered.note, "今天又肝完一轮")
        XCTAssertEqual(rendered.metrics[0].headline, "38%")
        XCTAssertTrue(rendered.metrics[0].amountDetails.isEmpty)
        XCTAssertEqual(rendered.template, .ticket)
    }

    func test可以单独隐藏重置时间和单个指标() throws {
        let now = date(2026, 9, 20, 21, 40)
        let weeklyEnd = now.addingTimeInterval(3 * 24 * 60 * 60)
        let state = makeState(
            name: "工作机",
            snapshot: UsageSnapshot(
                planName: "Plus",
                kind: .periodic,
                fiveHour: UsageMetric(used: 7.60, limit: 20, remaining: 12.40, percent: 38, unit: .usd, windowEnd: weeklyEnd),
                weekly: UsageMetric(used: 82, limit: 200, remaining: 118, percent: 41, unit: .usd, windowEnd: weeklyEnd),
                token: nil,
                allowedModels: [],
                fetchedAt: now
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        var draft = UsageShareDraft.make(from: content)
        draft.showsResetTimes = false
        draft.showsSubtitle = false
        draft.setMetricVisible("fiveHour", false)

        let rendered = UsageShareContentBuilder.render(content: content, draft: draft)
        XCTAssertEqual(rendered.subtitle, "")
        XCTAssertEqual(rendered.metrics.map(\.id), ["weekly"])
        XCTAssertTrue(rendered.metrics[0].timeDetails.isEmpty)
        XCTAssertFalse(rendered.metrics[0].amountDetails.isEmpty)
    }

    func test票根窗口高度按卡片字段估算而不是渲染结果() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "工作机",
            snapshot: UsageSnapshot(
                planName: "Plus",
                kind: .periodic,
                fiveHour: UsageMetric(used: 7.60, limit: 20, remaining: 12.40, percent: 38, unit: .usd, windowEnd: nil),
                weekly: UsageMetric(used: 82, limit: 200, remaining: 118, percent: 41, unit: .usd, windowEnd: nil),
                token: nil,
                allowedModels: [],
                fetchedAt: now
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        let card = UsageShareContentBuilder.render(
            content: content,
            draft: UsageShareDraft.make(from: content)
        )
        let height = UsageShareWindowFrame.estimatedCardHeight(for: card)
        XCTAssertGreaterThanOrEqual(height, 520)
        XCTAssertGreaterThan(height, 108 + 200)
    }

    func test没有快照时不能生成分享内容() {
        let state = KeyUsageState(
            configuration: KeyConfiguration(id: UUID(), name: "空", keySuffix: "abcd", sortOrder: 0),
            snapshot: nil,
            lastSuccessAt: nil,
            isRefreshing: false,
            isStale: false,
            error: nil
        )
        XCTAssertNil(UsageShareContentBuilder.build(state: state))
    }

    func test过长数值会改成整行展示避免截断() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "NewAPI",
            providerID: .newAPI,
            snapshot: UsageSnapshot(
                planName: "One-API",
                kind: .periodic,
                fiveHour: nil,
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: now,
                metrics: [
                    NormalizedUsageMetric(
                        id: "today-token",
                        label: "今日 Token",
                        value: Decimal(string: "1234567890123")!,
                        unit: .token,
                        presentation: .value,
                        semantic: .value
                    )
                ]
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        XCTAssertEqual(content.metrics[0].headline, "1,234,567,890,123")
        XCTAssertTrue(content.metrics[0].spansFullWidth)
    }

    func test文件名包含别名且不含非法字符() {
        let now = date(2026, 9, 20, 21, 40)
        let name = UsageShareContentBuilder.fileName(
            displayName: "工作机/主",
            date: now,
            timeZone: timeZone
        )
        XCTAssertEqual(name, "MyToken-工作机-主-20260920-2140.png")
    }

    func test消费指标附带成本且不把成本拆成独立格子() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "NewAPI",
            providerID: .newAPI,
            snapshot: UsageSnapshot(
                planName: "One-API",
                kind: .periodic,
                fiveHour: nil,
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: now,
                metrics: [
                    NormalizedUsageMetric(
                        id: "today-token",
                        label: "今日 Token",
                        value: 12345,
                        unit: .token,
                        presentation: .value,
                        semantic: .value
                    ),
                    NormalizedUsageMetric(
                        id: "today-token-cost",
                        label: "今日花费",
                        value: Decimal(string: "1.25")!,
                        unit: .currency,
                        presentation: .value,
                        semantic: .value,
                        currencyCode: "USD"
                    )
                ]
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        XCTAssertEqual(content.metrics.map(\.id), ["today-token"])
        XCTAssertEqual(content.metrics[0].headline, "12,345")
        XCTAssertEqual(content.metrics[0].amountDetails, ["≈ $1.25"])
    }

    @MainActor
    func test分享图能渲染出有效PNG() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "工作机",
            snapshot: UsageSnapshot(
                planName: "Plus",
                kind: .periodic,
                fiveHour: UsageMetric(used: 7.60, limit: 20, remaining: 12.40, percent: 38, unit: .usd, windowEnd: nil),
                weekly: UsageMetric(used: 82, limit: 200, remaining: 118, percent: 41, unit: .usd, windowEnd: nil),
                token: nil,
                allowedModels: [],
                fetchedAt: now
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        let card = UsageShareContentBuilder.render(
            content: content,
            draft: UsageShareDraft.make(from: content)
        )
        let image = try XCTUnwrap(UsageShareExport.image(for: card))
        XCTAssertGreaterThan(image.size.width, 0)
        XCTAssertGreaterThan(image.size.height, 0)
        let png = try XCTUnwrap(UsageShareExport.pngData(from: image))
        XCTAssertGreaterThan(png.count, 100)
    }

    @MainActor
    func test票根模板按登机牌外形渲染() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "工作机",
            snapshot: UsageSnapshot(
                planName: "Plus",
                kind: .periodic,
                fiveHour: UsageMetric(used: 7.60, limit: 20, remaining: 12.40, percent: 38, unit: .usd, windowEnd: nil),
                weekly: UsageMetric(used: 82, limit: 200, remaining: 118, percent: 41, unit: .usd, windowEnd: nil),
                token: nil,
                allowedModels: [],
                fetchedAt: now
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        var draft = UsageShareDraft.make(from: content)
        draft.template = .ticket
        draft.note = "今天又肝完一轮"
        let card = UsageShareContentBuilder.render(content: content, draft: draft)
        let image = try XCTUnwrap(UsageShareExport.image(for: card))
        XCTAssertGreaterThan(image.size.height, image.size.width * 0.8)
        let png = try XCTUnwrap(UsageShareExport.pngData(from: image))
        XCTAssertGreaterThan(png.count, 100)
    }

    @MainActor
    func test浅色票根模板可渲染() throws {
        let now = date(2026, 9, 20, 21, 40)
        let state = makeState(
            name: "工作机",
            snapshot: UsageSnapshot(
                planName: "Plus",
                kind: .periodic,
                fiveHour: UsageMetric(used: 7.60, limit: 20, remaining: 12.40, percent: 38, unit: .usd, windowEnd: nil),
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: now
            )
        )
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(state: state, now: now, timeZone: timeZone)
        )
        var draft = UsageShareDraft.make(from: content)
        draft.template = .ticketLight
        let card = UsageShareContentBuilder.render(content: content, draft: draft)
        XCTAssertEqual(card.template, .ticketLight)
        let image = try XCTUnwrap(UsageShareExport.image(for: card))
        XCTAssertGreaterThan(image.size.width, 0)
        XCTAssertGreaterThan(image.size.height, image.size.width * 0.8)
    }

    func test分享功能已接入弹窗和凭证页() throws {
        let popover = try TestSourceReader.read(["RoutinUsage", "Views", "UsagePopoverView.swift"])
        let row = try TestSourceReader.read(["RoutinUsage", "Views", "UsageRowView.swift"])
        XCTAssertTrue(row.contains("square.and.arrow.up"))
        XCTAssertTrue(row.contains("onShare"))
        XCTAssertTrue(popover.contains("UsageSharePanelController"))
    }

    func test字段开关按票面顺序单列展示() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsageShareEditorView.swift"
        ])

        XCTAssertTrue(source.contains("展示字段开关 (按票面顺序，隐藏即不导出)"))
        XCTAssertTrue(source.contains("LazyVStack(alignment: .leading, spacing: 8)"))
        let statusIndex = try XCTUnwrap(source.range(of: "可用状态徽章")?.lowerBound)
        let planIndex = try XCTUnwrap(source.range(of: "套餐规格")?.lowerBound)
        let watermarkIndex = try XCTUnwrap(source.range(of: "快照水印与防伪")?.lowerBound)
        XCTAssertLessThan(statusIndex, planIndex)
        XCTAssertLessThan(planIndex, watermarkIndex)
    }

    private func makeState(
        name: String,
        providerID: ProviderID = .routin,
        snapshot: UsageSnapshot
    ) -> KeyUsageState {
        KeyUsageState(
            configuration: KeyConfiguration(
                id: UUID(),
                name: name,
                keySuffix: "abcd",
                sortOrder: 0,
                providerID: providerID
            ),
            snapshot: snapshot,
            lastSuccessAt: snapshot.fetchedAt,
            isRefreshing: false,
            isStale: false,
            error: nil
        )
    }

    private func date(_ year: Int, _ month: Int, _ day: Int, _ hour: Int, _ minute: Int) -> Date {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = timeZone
        return calendar.date(
            from: DateComponents(year: year, month: month, day: day, hour: hour, minute: minute)
        )!
    }
}
