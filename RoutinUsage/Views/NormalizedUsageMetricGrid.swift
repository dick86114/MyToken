import SwiftUI

struct UsageMetricGridLayout: Equatable {
    let metrics: [NormalizedUsageMetric]
    let columns: Int
}

enum NormalizedUsageMetricResetStyle {
    case fullDateTime
    case relativeDuration
}

enum UsageMetricGridPolicy {
    static func layout(
        providerID: ProviderID,
        metrics: [NormalizedUsageMetric]
    ) -> UsageMetricGridLayout {
        switch providerID {
        case .glm:
            return UsageMetricGridLayout(
                metrics: metrics.filter { $0.id != "zcode-mcp" },
                columns: 2
            )
        case .volcengine:
            return UsageMetricGridLayout(metrics: metrics, columns: 2)
        case .deepseek:
            return UsageMetricGridLayout(metrics: metrics, columns: 2)
        case .routin:
            return UsageMetricGridLayout(metrics: metrics, columns: 2)
        }
    }
}

struct NormalizedUsageMetricGrid: View {
    let metrics: [NormalizedUsageMetric]
    let columns: Int
    var resetTimeStyle: NormalizedUsageMetricResetStyle = .fullDateTime
    var now: Date = .now

    var body: some View {
        let rows = stride(from: 0, to: metrics.count, by: columns).map {
            Array(metrics[$0..<min($0 + columns, metrics.count)])
        }

        Grid(horizontalSpacing: 16, verticalSpacing: 14) {
            ForEach(Array(rows.enumerated()), id: \.offset) { _, row in
                GridRow {
                    ForEach(0..<columns, id: \.self) { columnIndex in
                        if columnIndex < row.count {
                            NormalizedUsageMetricCell(
                                metric: row[columnIndex],
                                resetTimeStyle: resetTimeStyle,
                                now: now
                            )
                        } else {
                            Color.clear
                        }
                    }
                }
            }
        }
    }
}

private struct NormalizedUsageMetricCell: View {
    let metric: NormalizedUsageMetric
    let resetTimeStyle: NormalizedUsageMetricResetStyle
    let now: Date

    var body: some View {
        switch metric.presentation {
        case .progress:
            progressCell
        case .balance:
            balanceCell
        case .status:
            statusCell
        case .value:
            valueCell
        }
    }

    private var progressCell: some View {
        let percent = metric.displayedPercent ?? 0
        let percentText = "\(Int(percent.rounded()))%"

        return VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                Spacer(minLength: 4)
                Text(percentText)
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(healthColor)
                    .monospacedDigit()
            }

            UsageMetricProgressBar(percent: percent)

            if let used = metric.used, let limit = metric.limit {
                Text("已用 \(decimalText(used)) / \(decimalText(limit))")
                    .font(.caption2)
                    .foregroundStyle(.secondary)
                    .monospacedDigit()
                    .fixedSize(horizontal: false, vertical: true)

                if let remaining = metric.remaining {
                    Text("剩余 \(decimalText(remaining))")
                        .font(.caption2)
                        .foregroundStyle(.secondary)
                        .monospacedDigit()
                }

                if let windowEnd = metric.windowEnd {
                    switch resetTimeStyle {
                    case .fullDateTime:
                        Text("重置 \(UsageFormatter.fullDateTime(windowEnd))")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .monospacedDigit()
                            .fixedSize(horizontal: false, vertical: true)
                    case .relativeDuration:
                        Text("重置 \(UsageFormatter.resetTime(windowEnd, now: now))")
                            .font(.caption2)
                            .foregroundStyle(.secondary)
                            .monospacedDigit()
                            .fixedSize(horizontal: false, vertical: true)
                            .help("重置 \(UsageFormatter.fullDateTime(windowEnd))")

                        Text("剩余 \(UsageFormatter.remainingDurationText(until: windowEnd, now: now))")
                            .font(.caption2)
                            .foregroundStyle(
                                UsageFormatter.shouldHighlightRemainingDuration(
                                    for: metric,
                                    now: now
                                ) ? Color.green : Color.secondary
                            )
                            .monospacedDigit()
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "\(metric.label)，已使用 \(percentText)，剩余 \(decimalText(metric.remaining))"
        )
    }

    private var balanceCell: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                Spacer(minLength: 4)
                Text("\(decimalText(metric.value)) \(metric.currencyCode ?? "元")")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(healthColor)
                    .monospacedDigit()
            }
            Text("账户余额")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "\(metric.label)，\(decimalText(metric.value)) \(metric.currencyCode ?? "元")"
        )
    }

    private var statusCell: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                Spacer(minLength: 4)
                Text(metric.healthState == .unavailable ? "不可用" : "可用")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .foregroundStyle(healthColor)
            }
            Text("账户状态")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel(
            "\(metric.label)，\(metric.healthState == .unavailable ? "不可用" : "可用")"
        )
    }

    private var valueCell: some View {
        let suffix = metric.unit == .request ? " 次" : ""

        return VStack(alignment: .leading, spacing: 4) {
            HStack(alignment: .firstTextBaseline, spacing: 6) {
                Text(metric.label)
                    .font(.caption)
                    .foregroundStyle(.tertiary)
                Spacer(minLength: 4)
                Text("\(decimalText(metric.value))\(suffix)")
                    .font(.system(.headline, design: .rounded, weight: .semibold))
                    .monospacedDigit()
            }
            Text("账户信息")
                .font(.caption2)
                .foregroundStyle(.secondary)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(metric.label)，\(decimalText(metric.value))\(suffix)")
    }

    private var healthColor: Color {
        switch metric.healthState {
        case .normal: return .green
        case .warning: return .orange
        case .critical, .unavailable: return .red
        case .stale, .unknown: return .secondary
        }
    }

    private func decimalText(_ value: Decimal?) -> String {
        guard let value else { return "—" }
        return NSDecimalNumber(decimal: value).stringValue
    }
}
