import SwiftUI

struct CredentialAlertSettingsView: View {
    let title: String
    @Bindable var model: CredentialAlertSettingsModel
    let onClose: () -> Void
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        ViewThatFits(in: .vertical) {
            VStack(spacing: 0) {
                headerBlock
                alertContent
                    .padding(SettingsDesign.modalPadding)
            }
            .fixedSize(horizontal: false, vertical: true)

            VStack(spacing: 0) {
                headerBlock
                ScrollView(.vertical, showsIndicators: false) {
                    alertContent
                        .padding(SettingsDesign.modalPadding)
                }
            }
            .frame(minHeight: 300, idealHeight: 520, maxHeight: 680)
        }
        .frame(width: 680)
    }

    private var headerBlock: some View {
        VStack(spacing: 0) {
            header
                .padding(.horizontal, SettingsDesign.modalPadding)
                .padding(.top, SettingsDesign.modalPadding)
                .padding(.bottom, SettingsDesign.modalHeaderSpacing)

            Rectangle()
                .fill(SettingsDesign.dividerColor(colorScheme))
                .frame(height: 0.8)
        }
    }

    private var alertContent: some View {
        VStack(alignment: .leading, spacing: 16) {
            masterToggle

            if model.notificationsEnabled {
                if model.rules.isEmpty {
                    SettingsNotice(
                        tone: .neutral,
                        message: "此凭证当前没有可配置的额度或状态指标。",
                        title: "暂无可提醒的指标",
                        systemImage: "bell.slash"
                    )
                } else {
                    ruleGrid
                }
            } else {
                SettingsNotice(
                    tone: .neutral,
                    message: "开启总开关后，可以继续调整各指标的提醒规则。",
                    title: "提醒已关闭",
                    systemImage: "bell.slash"
                )
            }
        }
        .animation(
            reduceMotion
                ? nil
                : .spring(response: 0.3, dampingFraction: 0.9),
            value: model.notificationsEnabled
        )
    }

    private var ruleGrid: some View {
        LazyVGrid(
            columns: Array(
                repeating: GridItem(.flexible(), spacing: 14),
                count: model.rules.count > 1 ? 2 : 1
            ),
            alignment: .leading,
            spacing: 14
        ) {
            ForEach(model.rules) { rule in
                MetricAlertRuleEditor(
                    rule: Binding(
                        get: { rule },
                        set: { model.updateRule($0) }
                    ),
                    metric: model.metric(for: rule)
                )
            }
        }
    }

    private var header: some View {
        HStack(spacing: 12) {
            SettingsModalTitle(
                title,
                subtitle: "控制此凭证的额度与状态提醒"
            )

            Spacer()

            SettingsCloseButton(action: onClose)
        }
    }

    private var masterToggle: some View {
        SettingsGroup {
            SettingsRow(
                systemImage: "bell.badge",
                title: "提醒此凭证",
                detail: "关闭后将暂停此凭证的全部提醒。"
            ) {
                Toggle("提醒此凭证", isOn: $model.notificationsEnabled)
                    .toggleStyle(.switch)
                    .labelsHidden()
                    .accessibilityLabel("提醒此凭证")
            }
        }
    }
}

private struct MetricAlertRuleEditor: View {
    @Binding var rule: MetricAlertRule
    let metric: NormalizedUsageMetric?
    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    var body: some View {
        SettingsPanel {
            VStack(alignment: .leading, spacing: 12) {
                HStack(spacing: 10) {
                    VStack(alignment: .leading, spacing: 3) {
                        Text(metric?.label ?? rule.metricID)
                            .font(SettingsTypography.rowTitle)
                        if let metric {
                            Text(metricSummary(metric))
                                .font(SettingsTypography.secondary)
                                .foregroundStyle(.secondary)
                        }
                    }
                    Spacer(minLength: 8)
                    Toggle("启用提醒", isOn: $rule.isEnabled)
                        .toggleStyle(.switch)
                        .controlSize(.small)
                        .labelsHidden()
                        .accessibilityLabel("\(metric?.label ?? rule.metricID)提醒")
                }

                if rule.isEnabled {
                    MetricAlertThresholdControls(rule: $rule, metric: metric)
                } else {
                    SettingsNotice(
                        tone: .neutral,
                        message: "已暂停此类提醒。",
                        systemImage: "pause.circle.fill"
                    )
                }
            }
        }
        .frame(maxWidth: .infinity, minHeight: 124, alignment: .topLeading)
        .animation(
            reduceMotion
                ? nil
                : .spring(response: 0.28, dampingFraction: 0.9),
            value: rule.isEnabled
        )
    }

    private func metricSummary(_ metric: NormalizedUsageMetric) -> String {
        switch metric.semantic {
        case .usedQuota:
            return "按已用百分比提醒"
        case .remainingQuota:
            return "按剩余百分比提醒"
        case .balance:
            return "按余额阈值提醒"
        case .status:
            return "凭证状态异常时提醒"
        case .value:
            return "数值提醒"
        }
    }
}

private struct MetricAlertThresholdControls: View {
    @Binding var rule: MetricAlertRule
    let metric: NormalizedUsageMetric?

    var body: some View {
        switch rule.valueSource {
        case .usedPercent:
            VStack(alignment: .leading, spacing: 8) {
                ForEach(rule.thresholds.indices, id: \.self) { index in
                    thresholdRow(
                        label: index == 0 ? "提醒达到" : "严重达到",
                        index: index
                    )
                }
            }
        case .remainingPercent:
            VStack(alignment: .leading, spacing: 8) {
                ForEach(rule.thresholds.indices, id: \.self) { index in
                    thresholdRow(
                        label: index == 0 ? "提醒低于" : "严重低于",
                        index: index
                    )
                }
            }
        case .absoluteValue:
            if !rule.thresholds.isEmpty {
                HStack(spacing: 10) {
                    Text("余额低于")
                        .font(SettingsTypography.body)
                        .foregroundStyle(.secondary)
                    Spacer(minLength: 8)
                    SettingsFieldSurface(height: 34) {
                        TextField("余额", value: amountBinding, format: .number)
                            .textFieldStyle(.plain)
                            .font(SettingsTypography.numeric)
                            .multilineTextAlignment(.trailing)
                    }
                    .frame(width: 118)
                    Text(metric?.currencyCode ?? "未提供币种")
                        .font(SettingsTypography.caption)
                        .foregroundStyle(.secondary)
                }
            }
        case .healthState:
            SettingsNotice(
                tone: .info,
                message: "凭证状态变为异常时提醒。",
                systemImage: "exclamationmark.triangle.fill"
            )
        }
    }

    private func thresholdRow(label: String, index: Int) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            HStack {
                Text(label)
                    .font(SettingsTypography.body)
                    .foregroundStyle(.secondary)

                Spacer(minLength: 8)

                Text("\(percent(at: index))%")
                    .font(SettingsTypography.numeric.weight(.semibold))
                    .monospacedDigit()
                    .foregroundStyle(
                        index == 0
                            ? CompactPopoverPalette.warningColor
                            : CompactPopoverPalette.criticalColor
                    )
            }

            Slider(
                value: percentSliderBinding(at: index),
                in: 1...99,
                step: 1
            )
            .tint(
                index == 0
                    ? CompactPopoverPalette.warningColor
                    : CompactPopoverPalette.criticalColor
            )
            .accessibilityLabel(label)
            .accessibilityValue("\(percent(at: index))%")
        }
    }

    private func percent(at index: Int) -> Int {
        NSDecimalNumber(decimal: rule.thresholds[index].value ?? 0).intValue
    }

    private func percentSliderBinding(at index: Int) -> Binding<Double> {
        Binding(
            get: { Double(percent(at: index)) },
            set: { newValue in
                let roundedValue = Int(newValue.rounded())
                let boundedValue: Int
                switch rule.valueSource {
                case .usedPercent:
                    if index == 0 {
                        boundedValue = min(roundedValue, max(1, percent(at: 1) - 1))
                    } else {
                        boundedValue = max(roundedValue, min(99, percent(at: 0) + 1))
                    }
                case .remainingPercent:
                    if index == 0 {
                        boundedValue = max(roundedValue, min(99, percent(at: 1) + 1))
                    } else {
                        boundedValue = min(roundedValue, max(1, percent(at: 0) - 1))
                    }
                default:
                    boundedValue = roundedValue
                }
                rule.thresholds[index].value = Decimal(boundedValue)
            }
        )
    }

    private var amountBinding: Binding<Decimal> {
        Binding(
            get: { rule.thresholds[0].value ?? 0 },
            set: { rule.thresholds[0].value = $0 }
        )
    }
}
