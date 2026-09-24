import XCTest
@testable import RoutinUsage

final class UsagePresentationPolicyTests: XCTestCase {
    func test供应商栅格策略过滤GLMZCode并按供应商分栏() {
        func metric(_ id: String) -> NormalizedUsageMetric {
            NormalizedUsageMetric(
                id: id,
                label: id,
                unit: .request,
                presentation: .value,
                semantic: .value
            )
        }

        let metrics = [
            metric("five-hour"),
            metric("weekly"),
            metric("model-calls"),
            metric("zcode-mcp")
        ]

        XCTAssertEqual(
            UsageMetricGridPolicy.layout(providerID: .glm, metrics: metrics),
            UsageMetricGridLayout(
                metrics: [
                    metric("five-hour"),
                    metric("weekly"),
                    metric("model-calls"),
                    metric("zcode-mcp")
                ],
                columns: 2,
                showsAmountDetails: false
            )
        )
        XCTAssertEqual(UsageMetricGridPolicy.layout(providerID: .volcengine, metrics: metrics).columns, 2)
        XCTAssertEqual(UsageMetricGridPolicy.layout(providerID: .deepseek, metrics: metrics).columns, 2)
        XCTAssertEqual(UsageMetricGridPolicy.layout(providerID: .routin, metrics: metrics).columns, 2)
        XCTAssertEqual(UsageMetricGridPolicy.layout(providerID: .newAPI, metrics: metrics).columns, 2)
    }

    func test简洁模式按供应商返回写死字段() {
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(providerID: .routin, metadata: [:]).metricIDs,
            ["fiveHour", "weekly"]
        )
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(
                providerID: .routin,
                metadata: ["usageKind": "tokenPack"]
            ).metricIDs,
            ["token"]
        )
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(providerID: .deepseek, metadata: [:]).metricIDs,
            ["balance"]
        )
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(
                providerID: .xiaomi,
                metadata: ["usageKind": "api"]
            ).metricIDs,
            ["account-balance"]
        )
        let xiaomiPlan = UsageCardDensityPolicy.compactSpec(
            providerID: .xiaomi,
            metadata: ["usageKind": "plan"]
        )
        XCTAssertEqual(xiaomiPlan.metricIDs, ["plan-total"])
        XCTAssertFalse(xiaomiPlan.showsResetTime)
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(providerID: .glm, metadata: [:]).metricIDs,
            ["five-hour", "weekly"]
        )
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(providerID: .volcengine, metadata: [:]).metricIDs,
            ["fiveHour", "weekly", "monthly"]
        )
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(providerID: .newAPI, metadata: [:]).metricIDs,
            ["today-token", "one-day-token", "seven-day-token", "thirty-day-token"]
        )
        XCTAssertEqual(
            UsageCardDensityPolicy.compactSpec(providerID: .commandCode, metadata: [:]).metricIDs,
            ["five-hour", "weekly", "credit-progress"]
        )
        XCTAssertTrue(
            UsageCardDensityPolicy.compactSpec(providerID: .glm, metadata: [:]).showsResetTime
        )
    }

    func test简洁模式指标按短周期到长周期排序() {
        func metric(_ id: String) -> NormalizedUsageMetric {
            NormalizedUsageMetric(
                id: id,
                label: id,
                unit: .currency,
                presentation: .progress,
                semantic: .usedQuota
            )
        }

        let ordered = UsageCardDensityPolicy.orderedMetrics(
            providerID: .commandCode,
            metadata: [:],
            metrics: [
                metric("credit-progress"),
                metric("weekly"),
                metric("five-hour"),
                metric("request-count")
            ]
        )

        XCTAssertEqual(ordered.map(\.id), ["five-hour", "weekly", "credit-progress"])

        let volcengine = UsageCardDensityPolicy.orderedMetrics(
            providerID: .volcengine,
            metadata: [:],
            metrics: [
                metric("monthly"),
                metric("fiveHour"),
                metric("weekly")
            ]
        )

        XCTAssertEqual(volcengine.map(\.id), ["fiveHour", "weekly", "monthly"])
    }

    func test弹窗为CommandCode接入专用指标视图() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("UsageRowView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("providerID == .commandCode"))
        XCTAssertTrue(source.contains("CommandCodeUsageMetricsView("))
        let detailSource = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("Settings")
                .appendingPathComponent("CredentialDetailsView.swift"),
            encoding: .utf8
        )
        XCTAssertTrue(detailSource.contains("CommandCodeUsageMetricsView("))
        XCTAssertTrue(detailSource.contains("providerID == .commandCode"))
    }

    func testCommandCode指标按三行布局排列并隐藏额外摘要() {
        let metrics = [
            "credit-progress", "credit-balance", "monthly-remaining",
            "purchased-remaining", "free-remaining", "period-spent",
            "request-count", "five-hour", "weekly"
        ].map { id in
            NormalizedUsageMetric(
                id: id,
                label: id,
                unit: .currency,
                presentation: .value,
                semantic: .value
            )
        }

        let layout = CommandCodeMetricLayoutPolicy.layout(metrics: metrics)

        XCTAssertEqual(layout.fiveHour?.id, "five-hour")
        XCTAssertEqual(layout.weekly?.id, "weekly")
        XCTAssertEqual(layout.monthly?.id, "credit-progress")
        XCTAssertEqual(layout.requestCount?.id, "request-count")
        XCTAssertEqual(layout.purchasedRemaining?.id, "purchased-remaining")
        XCTAssertEqual(layout.freeRemaining?.id, "free-remaining")
        XCTAssertEqual(layout.displayedMetrics.map(\.id), [
            "five-hour", "weekly", "credit-progress", "request-count",
            "purchased-remaining", "free-remaining"
        ])
    }

    func testCommandCode周期详情按纵向顺序输出四行() throws {
        let now = Date(timeIntervalSince1970: 1_789_048_800)
        let metric = NormalizedUsageMetric(
            id: "five-hour",
            label: "5 小时",
            used: 2.56,
            limit: 14,
            remaining: 11.44,
            unit: .currency,
            windowEnd: now.addingTimeInterval(23 * 60),
            presentation: .progress,
            semantic: .usedQuota,
            currencyCode: "$"
        )

        let lines = CommandCodeMetricLayoutPolicy.progressDetailLines(for: metric, now: now)

        XCTAssertEqual(lines.map(\.text), [
            "已用 $2.56 / $14.00",
            "剩余 $11.44",
            "重置 \(UsageFormatter.resetTime(metric.windowEnd!, now: now))",
            "剩余 23分钟"
        ])
        XCTAssertTrue(lines.last?.highlights == true)
    }

    func testCommandCode摘要卡保留请求次数并改为左右布局() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("CommandCodeUsageMetricsView.swift"),
            encoding: .utf8
        )

        XCTAssertFalse(source.contains("额度单位：美元（USD）"))
        XCTAssertTrue(source.contains("private var summaryMetricsCell: some View"))
        XCTAssertTrue(source.contains("case .card:"))
        XCTAssertTrue(source.contains("requestCountSummaryCell(layout.requestCount)"))
        XCTAssertTrue(source.contains("case .details:"))
        XCTAssertTrue(source.contains("requestCountCell(layout.requestCount)"))
        XCTAssertTrue(source.contains("valueCell(layout.purchasedRemaining, fallbackLabel: \"购买剩余\")"))
        XCTAssertTrue(source.contains("valueCell(layout.freeRemaining, fallbackLabel: \"赠送剩余\")"))
        XCTAssertTrue(source.contains("Text(metric.label.isEmpty ? \"累计请求\" : metric.label)"))
        XCTAssertTrue(source.contains("UsageFormatter.currencyText(metric.used, currencyCode: metric.currencyCode)"))
        XCTAssertTrue(source.contains("UsageFormatter.currencyText(metric.remaining, currencyCode: metric.currencyCode)"))
        XCTAssertTrue(source.contains("UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode)"))
    }

    func testCommandCode金额四舍五入保留两位小数() {
        XCTAssertEqual(
            CommandCodeMetricFormatter.amount(Decimal(string: "12.345")),
            "12.35"
        )
        XCTAssertEqual(
            CommandCodeMetricFormatter.amount(Decimal(string: "7.2")),
            "7.20"
        )
    }

    func test弹窗按供应商使用统一栅格并隐藏GLMZCode指标() throws {
        let popoverRow = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("UsageRowView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(popoverRow.contains("UsageMetricGridPolicy.layout("))
        XCTAssertTrue(popoverRow.contains("NormalizedUsageMetricGrid("))
        XCTAssertFalse(popoverRow.contains("ForEach(snapshot.normalizedMetrics)"))
    }

    func test凭证管理页保持轻量且不复制完整用量栅格() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("Settings")
                .appendingPathComponent("CredentialManagementView.swift"),
            encoding: .utf8
        )

        XCTAssertFalse(source.contains("UsageMetricGridPolicy.layout("))
        XCTAssertFalse(source.contains("NormalizedUsageMetricGrid("))
    }

    func test通用用量卡片使用Routin小字号并完整单独显示重置时间() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("NormalizedUsageMetricGrid.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(
            source.contains("Text(metric.label)\n                    .font(.system(size: 12))")
        )
        XCTAssertFalse(source.contains(".font(.body)"))
        XCTAssertTrue(
            source.contains("Text(\"重置 \\(UsageFormatter.fullDateTime(windowEnd))\")")
        )
        XCTAssertTrue(source.contains("case .relativeDuration"))
        XCTAssertTrue(
            source.contains("UsageFormatter.remainingDurationText(until: windowEnd, now: now)")
        )
        XCTAssertTrue(
            source.contains("UsageFormatter.shouldHighlightRemainingDuration(")
        )
        XCTAssertTrue(
            source.contains("? CompactPopoverPalette.positive(colorScheme) : Color.secondary")
        )
        XCTAssertTrue(source.contains("case .relativeDuration"))
        XCTAssertTrue(
            source.contains("UsageFormatter.remainingDurationText(until: windowEnd, now: now)")
        )
    }

    func test简洁进度格只渲染重置时刻() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/NormalizedUsageMetricGrid.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("case .resetTimeOnly"))
        XCTAssertTrue(source.contains("UsageFormatter.resetTime(windowEnd, now: now)"))
    }

    func test弹窗通用卡片按Routin逻辑显示重置剩余时长() throws {
        let source = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("UsageRowView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(source.contains("normalizedMetricsContent(snapshot: snapshot, now: now)"))
        XCTAssertTrue(source.contains("resetTimeStyle: .relativeDuration"))
        XCTAssertTrue(source.contains("now: now"))
    }

    func test弹窗卡片按密度渲染并默认完整以免详情页误伤() throws {
        let row = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsageRowView.swift"),
            encoding: .utf8
        )
        let popover = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsagePopoverView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(row.contains("var density: UsageCardDensity = .full"))
        XCTAssertTrue(popover.contains("density: settings.usageCardDensity"))
        XCTAssertFalse(row.contains("groupMultiplierText(currentGroupMultiplier)"))
    }

    func test完整卡片保留内边距() throws {
        let row = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsageRowView.swift"),
            encoding: .utf8
        )
        let fullStart = try XCTUnwrap(row.range(of: "private func fullCard(now: Date)"))
        let compactStart = try XCTUnwrap(row.range(of: "private func compactCard(now: Date)"))
        let fullBody = row[fullStart.lowerBound..<compactStart.lowerBound]

        XCTAssertTrue(fullBody.contains(".padding(.vertical, 10)"))
        XCTAssertTrue(fullBody.contains(".padding(.horizontal, 8)"))
    }

    func test简洁卡片统一纵向排列指标() throws {
        let sections = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/ProviderUsageMetricSections.swift"),
            encoding: .utf8
        )
        let command = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/CommandCodeUsageMetricsView.swift"),
            encoding: .utf8
        )
        let row = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/UsageRowView.swift"),
            encoding: .utf8
        )
        let details = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage/Views/Settings/CredentialDetailsView.swift"),
            encoding: .utf8
        )

        XCTAssertFalse(sections.contains("var density: UsageCardDensity"))
        XCTAssertFalse(command.contains("var density: UsageCardDensity"))
        XCTAssertTrue(row.contains("func compactCard(now: Date)"))
        XCTAssertTrue(row.contains("func compactMetricRow(_ metric: NormalizedUsageMetric, now: Date)"))
        XCTAssertTrue(row.contains("UsageCardDensityPolicy.orderedMetrics("))
        XCTAssertFalse(details.contains("density: .compact"))
    }

    func test弹窗供应商信息显示供应商与套餐() throws {
        XCTAssertEqual(
            UsageRowPresentation.subscriptionDescription(providerID: .glm, planName: "Coding Plan"),
            "GLM · Coding Plan"
        )
        XCTAssertEqual(
            UsageRowPresentation.subscriptionDescription(providerID: .routin, planName: "成长版"),
            "Routin · 成长版"
        )
        XCTAssertEqual(
            UsageRowPresentation.subscriptionDescription(providerID: .routin, planName: ""),
            "Routin"
        )
    }

    func test凭证卡片供应商名称恢复官网链接交互() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsageRowView.swift"
        ])

        let identityStart = try XCTUnwrap(source.range(of: "private var compactIdentity"))
        let identityEnd = try XCTUnwrap(
            source.range(
                of: "private var compactActionButtons",
                range: identityStart.lowerBound..<source.endIndex
            )
        )
        let identity = String(source[identityStart.lowerBound..<identityEnd.lowerBound])

        XCTAssertFalse(identity.contains("Text(UsageRowPresentation.subscriptionDescription("))
        XCTAssertTrue(identity.contains("providerSubtitle"))

        let componentStart = try XCTUnwrap(source.range(of: "private struct ProviderWebsiteLink"))
        let component = String(source[componentStart.lowerBound...])
        XCTAssertTrue(component.contains("Link(destination: destination)"))
        XCTAssertTrue(component.contains(".help(\"打开 \\(providerName) 官网\")"))
        XCTAssertTrue(component.contains("Image(systemName: \"arrow.up.right\")"))
        XCTAssertTrue(component.contains("Capsule().fill"))
        XCTAssertTrue(component.contains("isHovering ? 0.14 : 0.06"))
        XCTAssertTrue(component.contains("NSCursor.pointingHand.push()"))
        XCTAssertTrue(component.contains("NSCursor.pop()"))
        XCTAssertTrue(component.contains(".onDisappear"))
    }

    func test设置窗口存在时显示Dock图标关闭后恢复菜单栏形态() throws {
        let coordinator = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("App")
                .appendingPathComponent("SettingsWindowActivationPolicy.swift"),
            encoding: .utf8
        )
        let app = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("App")
                .appendingPathComponent("RoutinUsageApp.swift"),
            encoding: .utf8
        )
        let settings = try String(
            contentsOf: URL(fileURLWithPath: #filePath)
                .deletingLastPathComponent()
                .deletingLastPathComponent()
                .appendingPathComponent("RoutinUsage")
                .appendingPathComponent("Views")
                .appendingPathComponent("Settings")
                .appendingPathComponent("SettingsWindowView.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(coordinator.contains(".regular : .accessory"))
        XCTAssertTrue(app.contains("SettingsWindowActivationPolicy.refresh()"))
        XCTAssertTrue(settings.contains("SettingsWindowDockIconAnchor()"))
    }
}
