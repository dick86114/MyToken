import SwiftUI

struct XiaomiAPIMetricsView: View {
    let metrics: [NormalizedUsageMetric]
    var density: UsageCardDensity = .full

    private let accountIDs = [
        "account-balance",
        "total-consumption",
        "cash-balance",
        "gift-balance"
    ]
    private let tokenIDs = [
        "total-tokens",
        "output-tokens",
        "cache-tokens",
        "input-tokens"
    ]

    var body: some View {
        if density == .compact {
            HStack(alignment: .top, spacing: 12) {
                ForEach(compactMetrics) { metric in
                    metricCell(metric)
                        .frame(maxWidth: .infinity, alignment: .leading)
                }
            }
        } else {
            VStack(alignment: .leading, spacing: 12) {
                metricRow(accountIDs)

                VStack(alignment: .leading, spacing: 5) {
                    Text("Token")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.tertiary)
                    metricRow(tokenIDs)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var compactMetrics: [NormalizedUsageMetric] {
        let allowed = Set(
            UsageCardDensityPolicy.compactSpec(
                providerID: .xiaomi,
                metadata: ["usageKind": "api"]
            ).metricIDs
        )
        return metrics.filter { allowed.contains($0.id) }
    }

    private func metricRow(_ ids: [String]) -> some View {
        HStack(alignment: .top, spacing: 12) {
            ForEach(ids, id: \.self) { id in
                metricCell(metrics.first(where: { $0.id == id }))
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
    }

    private func metricCell(_ metric: NormalizedUsageMetric?) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(metric?.label ?? "—")
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(valueText(metric))
                .font(.system(.headline, design: .rounded, weight: .semibold))
                .foregroundStyle(color(metric?.healthState))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric?.label ?? "—")，\(valueText(metric))")
    }

    private func valueText(_ metric: NormalizedUsageMetric?) -> String {
        guard let metric else { return "—" }
        switch metric.unit {
        case .currency:
            return UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode)
        case .token:
            return UsageFormatter.exactTokenText(metric.value)
        case .request:
            return "\(UsageFormatter.numberText(metric.value, grouping: true)) 次"
        case .boolean, .text:
            return UsageFormatter.numberText(metric.value, grouping: true)
        }
    }

    private func color(_ healthState: UsageMetricHealthState?) -> Color {
        switch healthState {
        case .normal: return .green
        case .warning: return .orange
        case .critical, .unavailable: return .red
        case .stale, .unknown: return .secondary
        case nil: return .secondary
        }
    }
}

struct VolcengineCodingPlanMetricsView: View {
    let metrics: [NormalizedUsageMetric]
    var density: UsageCardDensity = .full
    let now: Date

    private var session: NormalizedUsageMetric? { metric("fiveHour") }
    private var weekly: NormalizedUsageMetric? { metric("weekly") }
    private var monthly: NormalizedUsageMetric? { metric("monthly") }

    var body: some View {
        if density == .compact {
            NormalizedUsageMetricGrid(
                metrics: compactMetrics,
                columns: 2,
                resetTimeStyle: .resetTimeOnly,
                now: now,
                showsAmountDetails: false
            )
        } else {
            VStack(alignment: .leading, spacing: 14) {
                HStack(alignment: .top, spacing: 16) {
                    codingQuotaCell(session, fallbackTitle: "session")
                    codingQuotaCell(weekly, fallbackTitle: "weekly")
                }
                if let monthly {
                    codingMonthlyCell(monthly)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var compactMetrics: [NormalizedUsageMetric] {
        let allowed = Set(
            UsageCardDensityPolicy.compactSpec(providerID: .volcengine, metadata: [:]).metricIDs
        )
        return metrics.filter { allowed.contains($0.id) }
    }

    private func codingQuotaCell(
        _ metric: NormalizedUsageMetric?,
        fallbackTitle: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack(alignment: .firstTextBaseline) {
                Text(metric?.label ?? fallbackTitle)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer(minLength: 4)
                Text(percentText(metric))
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(color(metric))
                    .monospacedDigit()
            }
            UsageMetricProgressBar(percent: metric?.displayedPercent ?? 0)
            detailLines(metric)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func codingMonthlyCell(_ metric: NormalizedUsageMetric) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack(alignment: .firstTextBaseline) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.secondary)
                Spacer(minLength: 4)
                Text(percentText(metric))
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(color(metric))
                    .monospacedDigit()
            }
            UsageMetricProgressBar(percent: metric.displayedPercent ?? 0)
            HStack(alignment: .top, spacing: 16) {
                VStack(alignment: .leading, spacing: 5) {
                    amountText("已用", metric.used, metric.limit)
                    resetText(metric)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                VStack(alignment: .leading, spacing: 5) {
                    amountText("剩余", metric.remaining, nil)
                    remainingText(metric)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    @ViewBuilder
    private func detailLines(_ metric: NormalizedUsageMetric?) -> some View {
        if let metric {
            amountText("已用", metric.used, metric.limit)
            amountText("剩余", metric.remaining, nil)
            resetText(metric)
            remainingText(metric)
        } else {
            Text("暂无数据")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
    }

    private func amountText(_ title: String, _ value: Decimal?, _ limit: Decimal?) -> some View {
        Text("\(title) \(decimalText(value))\(limit.map { " / \(decimalText($0))" } ?? "")")
            .font(.caption2)
            .foregroundStyle(.secondary)
            .monospacedDigit()
            .fixedSize(horizontal: false, vertical: true)
    }

    private func resetText(_ metric: NormalizedUsageMetric) -> some View {
        Text("重置 \(metric.windowEnd.map { UsageFormatter.resetTime($0, now: now) } ?? "—")")
            .font(.caption2)
            .foregroundStyle(.secondary)
            .monospacedDigit()
            .fixedSize(horizontal: false, vertical: true)
    }

    private func remainingText(_ metric: NormalizedUsageMetric) -> some View {
        Text("剩余 \(metric.windowEnd.map { UsageFormatter.remainingDurationText(until: $0, now: now) } ?? "—")")
            .font(.caption2)
            .foregroundStyle(.secondary)
            .monospacedDigit()
            .fixedSize(horizontal: false, vertical: true)
    }

    private func metric(_ id: String) -> NormalizedUsageMetric? {
        metrics.first { $0.id == id }
    }

    private func percentText(_ metric: NormalizedUsageMetric?) -> String {
        UsageFormatter.displayPercentText(metric?.displayedPercent)
    }

    private func decimalText(_ value: Decimal?) -> String {
        UsageFormatter.numberText(value)
    }

    private func color(_ metric: NormalizedUsageMetric?) -> Color {
        guard let metric else { return .secondary }
        switch metric.healthState {
        case .normal: return .green
        case .warning: return .orange
        case .critical, .unavailable: return .red
        case .stale, .unknown: return .secondary
        }
    }
}

/// 火山两个计划共用周期额度卡片；月度额度横跨整行，避免 Coding Plan 的第三项挤压前两项。
struct VolcenginePlanUsageMetricsView: View {
    let metrics: [NormalizedUsageMetric]
    var density: UsageCardDensity = .full
    let now: Date

    var body: some View {
        if density == .compact {
            NormalizedUsageMetricGrid(
                metrics: compactMetrics,
                columns: 2,
                resetTimeStyle: .resetTimeOnly,
                now: now,
                showsAmountDetails: false
            )
        } else {
            VStack(alignment: .leading, spacing: 10) {
                NormalizedUsageMetricGrid(
                    metrics: metrics.filter { $0.presentation == .progress },
                    columns: 2,
                    resetTimeStyle: .relativeDuration,
                    now: now
                )
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }

    private var compactMetrics: [NormalizedUsageMetric] {
        let allowed = Set(
            UsageCardDensityPolicy.compactSpec(providerID: .volcengine, metadata: [:]).metricIDs
        )
        return metrics.filter { allowed.contains($0.id) && $0.presentation == .progress }
    }
}

/// New API 的指标语义与订阅型供应商不同：额度有上限，消费和请求是活动统计。
struct NewAPIUsageMetricsView: View {
    let metrics: [NormalizedUsageMetric]
    var density: UsageCardDensity = .full
    let now: Date

    private var quotaMetric: NormalizedUsageMetric? {
        metric("quota-progress")
    }

    private var consumptionMetrics: [NormalizedUsageMetric] {
        [
            "today-token", "one-day-token",
            "seven-day-token", "thirty-day-token"
        ].compactMap(metric)
    }

    private var currencySymbol: String {
        quotaMetric?.currencyCode ?? consumptionMetrics.first?.currencyCode ?? "额度"
    }

    private var displaysRawQuota: Bool {
        currencySymbol == "额度"
    }

    var body: some View {
        if density == .compact {
            LazyVGrid(
                columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 2),
                alignment: .leading,
                spacing: 10
            ) {
                ForEach(compactTokenMetrics) { metric in
                    compactTokenCell(metric)
                }
            }
        } else {
            VStack(alignment: .leading, spacing: 10) {
                if let quotaMetric {
                    quotaCard(quotaMetric)
                }

                if !consumptionMetrics.isEmpty {
                    consumptionSection
                }

                activitySection
            }
        }
    }

    private var compactTokenMetrics: [NormalizedUsageMetric] {
        let allowed = Set(
            UsageCardDensityPolicy.compactSpec(providerID: .newAPI, metadata: [:]).metricIDs
        )
        return consumptionMetrics.filter { allowed.contains($0.id) }
    }

    private func quotaCard(_ metric: NormalizedUsageMetric) -> some View {
        let percent = metric.displayedPercent ?? 0
        let percentText = metric.limit == 0
            ? "—"
            : UsageFormatter.displayPercentText(metric.displayedPercent)

        return VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.tertiary)

                Spacer(minLength: 4)

                Text(percentText)
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(healthColor(metric.healthState))
                    .monospacedDigit()
            }

            UsageMetricProgressBar(percent: percent)

            HStack(alignment: .firstTextBaseline, spacing: 12) {
                Text("已用 \(quotaText(metric.used)) / \(quotaText(metric.limit))")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
                    .fixedSize(horizontal: false, vertical: true)

                Spacer(minLength: 12)

                Text("剩余 \(quotaText(metric.remaining))")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "\(metric.label)，已使用 \(percentText)，已用 \(quotaText(metric.used))，剩余 \(quotaText(metric.remaining))"
        )
    }

    private var consumptionSection: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("Token 消耗")
                    .font(.caption)
                    .foregroundStyle(.tertiary)

                Spacer(minLength: 4)

                Text("单位 Token")
                    .font(.caption2)
                    .foregroundStyle(.tertiary)
            }

            Grid(horizontalSpacing: 12, verticalSpacing: 8) {
                GridRow {
                    ForEach(Array(consumptionMetrics.prefix(2))) { metric in
                        consumptionCell(metric)
                    }
                }

                GridRow {
                    ForEach(Array(consumptionMetrics.dropFirst(2))) { metric in
                        consumptionCell(metric)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func consumptionCell(_ metric: NormalizedUsageMetric) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(metric.label)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)

            Text(UsageFormatter.exactTokenText(metric.value))
                .font(.system(.headline, design: .rounded, weight: .semibold))
                .foregroundStyle(.primary)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.72)

            Text("≈ \(costText(for: metric))")
                .font(.caption2)
                .foregroundStyle(.tertiary)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(UsageFormatter.exactTokenText(metric.value))，约 \(costText(for: metric))")
    }

    private var activitySection: some View {
        VStack(alignment: .leading, spacing: 6) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text("请求活动")
                    .font(.caption)
                    .foregroundStyle(.tertiary)

                Spacer(minLength: 4)
            }

            Grid(horizontalSpacing: 10, verticalSpacing: 8) {
                GridRow {
                    activityCell(
                        label: "RPM",
                        value: metric("rpm")?.value,
                        detail: "近 60 秒请求"
                    )

                    activityCell(
                        label: "TPM",
                        value: metric("tpm")?.value,
                        detail: "近 60 秒 Token"
                    )

                    activityCell(
                        label: "账户累计请求",
                        value: metric("request-count")?.value,
                        detail: "当前用户全部 API 请求"
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func compactTokenCell(_ metric: NormalizedUsageMetric) -> some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(metric.label)
                .font(.caption)
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(UsageFormatter.exactTokenText(metric.value))
                .font(.system(.headline, design: .rounded, weight: .semibold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.72)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(UsageFormatter.exactTokenText(metric.value))")
    }

    private func activityCell(
        label: String,
        value: Decimal?,
        detail: String
    ) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label)
                .font(.caption2)
                .foregroundStyle(.secondary)

            Text(UsageFormatter.compactMetricValue(value))
                .font(.system(.headline, design: .rounded, weight: .semibold))
                .foregroundStyle(.primary)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.72)

            Text(detail)
                .font(.caption2)
                .foregroundStyle(.tertiary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label)，\(UsageFormatter.compactMetricValue(value))，\(detail)")
    }

    private func costText(for metric: NormalizedUsageMetric) -> String {
        let costMetric = self.metric("\(metric.id)-cost")
        guard let costMetric else { return "—" }
        return UsageFormatter.currencyText(costMetric.value, symbol: costMetric.currencyCode ?? "¥")
    }

    private func quotaText(_ value: Decimal?) -> String {
        if displaysRawQuota {
            return UsageFormatter.compactMetricValue(value)
        }
        return UsageFormatter.currencyText(value, symbol: currencySymbol)
    }

    private func metric(_ id: String) -> NormalizedUsageMetric? {
        metrics.first(where: { $0.id == id })
    }

    private func healthColor(_ state: UsageMetricHealthState) -> Color {
        switch state {
        case .normal: return .green
        case .warning: return .orange
        case .critical, .unavailable: return .red
        case .stale, .unknown: return .secondary
        }
    }
}

/// GLM 的两个调用量是无上限统计，展示为标准“标签 + 右对齐数值”行。
struct GLMUsageMetricsView: View {
    let metrics: [NormalizedUsageMetric]
    var density: UsageCardDensity = .full
    let now: Date

    private var progressMetrics: [NormalizedUsageMetric] {
        metrics.filter { $0.presentation == .progress }
    }

    private var callMetrics: [NormalizedUsageMetric] {
        ["model-calls", "zcode-mcp"].compactMap { id in
            metrics.first(where: { $0.id == id && $0.presentation == .value })
        }
    }

    private var activityMetrics: [NormalizedUsageMetric] {
        let ids = [
            "activity-total-tokens",
            "activity-peak-tokens",
            "activity-usage-duration",
            "activity-current-streak",
            "activity-longest-streak"
        ]
        return ids.compactMap { id in
            metrics.first(where: { $0.id == id })
        }
    }

    var body: some View {
        if density == .compact {
            NormalizedUsageMetricGrid(
                metrics: compactMetrics,
                columns: 2,
                resetTimeStyle: .resetTimeOnly,
                now: now,
                showsAmountDetails: false
            )
        } else {
            fullContent
        }
    }

    private var compactMetrics: [NormalizedUsageMetric] {
        let allowed = Set(
            UsageCardDensityPolicy.compactSpec(providerID: .glm, metadata: [:]).metricIDs
        )
        return metrics.filter { allowed.contains($0.id) && $0.presentation == .progress }
    }

    private var fullContent: some View {
        VStack(alignment: .leading, spacing: 10) {
            if !progressMetrics.isEmpty {
                NormalizedUsageMetricGrid(
                    metrics: progressMetrics,
                    columns: 2,
                    resetTimeStyle: .relativeDuration,
                    now: now,
                    showsAmountDetails: false
                )
            }

            if !callMetrics.isEmpty {
                Grid(horizontalSpacing: 16, verticalSpacing: 8) {
                    GridRow {
                        callCell(callMetrics[safe: 0])
                        callCell(callMetrics[safe: 1])
                    }
                }
            }

            if !activityMetrics.isEmpty {
                VStack(alignment: .leading, spacing: 8) {
                    Text("活跃度")
                        .font(.caption2.weight(.semibold))
                        .foregroundStyle(.tertiary)

                    LazyVGrid(
                        columns: Array(repeating: GridItem(.flexible(), spacing: 12), count: 3),
                        alignment: .leading,
                        spacing: 10
                    ) {
                        ForEach(activityMetrics) { metric in
                            activityCell(metric)
                        }
                    }
                }
            }
        }
    }

    @ViewBuilder
    private func callCell(_ metric: NormalizedUsageMetric?) -> some View {
        if let metric {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.tertiary)

                Spacer(minLength: 4)

                Text("\(UsageFormatter.compactMetricValue(metric.value)) 次")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(.primary)
                    .monospacedDigit()
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("\(metric.label)，\(UsageFormatter.compactMetricValue(metric.value)) 次")
        } else {
            Color.clear
        }
    }

    private func activityCell(_ metric: NormalizedUsageMetric) -> some View {
        let text = activityValueText(metric)
        return VStack(alignment: .leading, spacing: 3) {
            Text(text)
                .font(.system(.headline, design: .rounded, weight: .semibold))
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.65)
            Text(metric.label)
                .font(.caption2)
                .foregroundStyle(.secondary)
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(text)")
    }

    private func activityValueText(_ metric: NormalizedUsageMetric) -> String {
        switch metric.id {
        case "activity-total-tokens", "activity-peak-tokens":
            return UsageFormatter.compactMetricValue(metric.value)
        case "activity-usage-duration":
            return Self.durationText(milliseconds: metric.value)
        case "activity-current-streak", "activity-longest-streak":
            return "\(UsageFormatter.numberText(metric.value))天"
        default:
            return UsageFormatter.numberText(metric.value)
        }
    }

    private static func durationText(milliseconds: Decimal?) -> String {
        guard let milliseconds else { return "—" }
        let totalMinutes = max(0, NSDecimalNumber(decimal: milliseconds).doubleValue / 60_000)
        let minutes = Int(totalMinutes.rounded(.down))
        let days = minutes / (24 * 60)
        let hours = (minutes % (24 * 60)) / 60
        let remainingMinutes = minutes % 60
        var parts: [String] = []
        if days > 0 { parts.append("\(days)天") }
        if hours > 0 { parts.append("\(hours)小时") }
        if remainingMinutes > 0 || parts.isEmpty { parts.append("\(remainingMinutes)分钟") }
        return parts.joined()
    }
}

private extension Array {
    subscript(safe index: Int?) -> Element? {
        guard let index, indices.contains(index) else { return nil }
        return self[index]
    }
}
