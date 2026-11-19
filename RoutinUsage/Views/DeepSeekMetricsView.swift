import SwiftUI

/// DeepSeek 完整模式展示：参照 XiaomiAPIMetricsView 的做法，
/// 按固定字段列出接口返回的全部内容，缺值显示占位符而不是直接隐藏。
struct DeepSeekMetricsView: View {
    @Environment(\.colorScheme) private var colorScheme
    let metrics: [NormalizedUsageMetric]
    let allowedModels: [String]
    var visualStyle: UsageMetricsVisualStyle = .standard

    /// 每个币种一组：余额 / 充值 / 赠金。
    private struct CurrencyGroup: Identifiable {
        let id: String
        let balance: NormalizedUsageMetric?
        let toppedUp: NormalizedUsageMetric?
        let granted: NormalizedUsageMetric?

        var currencyCode: String? { balance?.currencyCode }
    }

    private var groups: [CurrencyGroup] {
        let balances = metrics.filter { $0.presentation == .balance }
        guard !balances.isEmpty else { return [] }
        let isMultiCurrency = balances.count > 1
        return balances.map { balance in
            let currency = balance.currencyCode?.uppercased() ?? ""
            let suffix = isMultiCurrency && !currency.isEmpty ? "-\(currency)" : ""
            return CurrencyGroup(
                id: balance.id,
                balance: balance,
                toppedUp: metrics.first { $0.id == "toppedUpBalance\(suffix)" }
                    ?? metrics.first { !isMultiCurrency && $0.id == "toppedUpBalance" },
                granted: metrics.first { $0.id == "grantedBalance\(suffix)" }
                    ?? metrics.first { !isMultiCurrency && $0.id == "grantedBalance" }
            )
        }
    }

    private var availability: NormalizedUsageMetric? {
        metrics.first { $0.id == "availability" }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: visualStyle.rowSpacing) {
            ForEach(groups) { group in
                VStack(alignment: .leading, spacing: 5) {
                    if groups.count > 1, let currency = group.currencyCode, !currency.isEmpty {
                        Text(currency)
                            .font(.system(size: 10, weight: .semibold))
                            .foregroundStyle(.tertiary)
                    }
                    metricRow([
                        ("余额", group.balance),
                        ("充值", group.toppedUp),
                        ("赠金", group.granted)
                    ])
                }
            }

            // 账户级字段与模型清单，固定展示，缺失时显示占位符。
            VStack(alignment: .leading, spacing: 5) {
                Text("账户")
                    .font(.system(size: 10, weight: .semibold))
                    .foregroundStyle(.tertiary)
                HStack(alignment: .top, spacing: visualStyle.columnSpacing) {
                    statusCell(availability)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    valueCell(
                        label: "可用模型",
                        text: allowedModels.isEmpty
                            ? "—"
                            : "\(UsageFormatter.numberText(Decimal(allowedModels.count), grouping: true)) 个"
                    )
                    .frame(maxWidth: .infinity, alignment: .leading)
                    // 保持与三个金额列相同的栅格宽度
                    Color.clear
                        .frame(maxWidth: .infinity)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    private func metricRow(
        _ items: [(label: String, metric: NormalizedUsageMetric?)]
    ) -> some View {
        HStack(alignment: .top, spacing: visualStyle.columnSpacing) {
            ForEach(Array(items.enumerated()), id: \.offset) { _, item in
                VStack(alignment: .leading, spacing: 2) {
                    Text(item.label)
                        .font(.system(size: visualStyle.valueLabelSize))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Text(valueText(item.metric))
                        .font(.system(size: visualStyle.valueSize, weight: .semibold, design: .monospaced))
                        .foregroundStyle(color(item.metric?.healthState))
                        .monospacedDigit()
                        .lineLimit(1)
                        .minimumScaleFactor(0.7)
                }
                .frame(maxWidth: .infinity, alignment: .leading)
                .accessibilityElement(children: .ignore)
                .accessibilityLabel("\(item.label)，\(valueText(item.metric))")
            }
        }
    }

    private func statusCell(_ metric: NormalizedUsageMetric?) -> some View {
        let isAvailable = (metric?.value ?? 0) > 0
        let text: String
        if metric == nil {
            text = "—"
        } else {
            text = isAvailable ? "可用" : "不可用"
        }
        let tint: Color
        if metric == nil {
            tint = .secondary
        } else {
            tint = isAvailable
                ? CompactPopoverPalette.positive(colorScheme)
                : CompactPopoverPalette.criticalColor
        }
        return VStack(alignment: .leading, spacing: 2) {
            Text("账户状态")
                .font(.system(size: visualStyle.valueLabelSize))
                .foregroundStyle(.secondary)
                .lineLimit(1)
            HStack(spacing: 4) {
                Circle()
                    .fill(tint)
                    .frame(width: 5, height: 5)
                Text(text)
                    .font(.system(size: visualStyle.valueSize, weight: .semibold))
                    .foregroundStyle(tint)
                    .lineLimit(1)
            }
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("账户状态，\(text)")
    }

    private func valueCell(label: String, text: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.system(size: visualStyle.valueLabelSize))
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(text)
                .font(.system(size: visualStyle.valueSize, weight: .semibold, design: .monospaced))
                .foregroundStyle(.primary)
                .monospacedDigit()
                .lineLimit(1)
                .minimumScaleFactor(0.7)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("\(label)，\(text)")
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
        guard let healthState else { return .secondary }
        return CompactPopoverPalette.healthColor(for: healthState, colorScheme)
    }
}
