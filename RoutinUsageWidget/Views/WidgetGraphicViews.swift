import SwiftUI

struct WidgetCredentialContent: View {
    let item: WidgetCredentialPresentation
    let style: WidgetDisplayStyle

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            header
            switch style {
            case .signalTiles:
                signalTiles
            case .scanBand:
                scanBand
            case .focusRing, .auto:
                focusRing
            }
            Spacer(minLength: 0)
            footer
        }
        .padding(2)
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 1) {
            Text(item.providerName.uppercased())
                .font(.system(size: 9, weight: .semibold))
                .foregroundStyle(.secondary)
                .lineLimit(1)
            Text(item.name)
                .font(.system(size: 13, weight: .bold))
                .lineLimit(1)
                .truncationMode(.tail)
        }
    }

    private var focusRing: some View {
        HStack(spacing: 10) {
            if let primary = item.primary {
                WidgetProgressRing(metric: primary)
                    .frame(width: 62, height: 62)
                VStack(alignment: .leading, spacing: 6) {
                    ForEach(item.secondary.prefix(2)) { metric in
                        WidgetMetricBar(metric: metric)
                    }
                }
            } else {
                Text("该套餐暂无可用指标")
                    .font(.system(size: 11, weight: .medium))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
            }
        }
    }

    private var scanBand: some View {
        VStack(alignment: .leading, spacing: 7) {
            ForEach(item.orderedMetrics.prefix(3)) { metric in
                HStack(spacing: 7) {
                    Text(metric.label)
                        .font(.system(size: 10, weight: .medium))
                        .lineLimit(1)
                        .truncationMode(.tail)
                    Spacer(minLength: 4)
                    Text(metric.valueText)
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .lineLimit(1)
                }
                WidgetMetricBar(metric: metric)
            }
        }
    }

    private var signalTiles: some View {
        LazyVGrid(
            columns: Array(repeating: GridItem(.flexible(), spacing: 6), count: 3),
            spacing: 6
        ) {
            ForEach(item.orderedMetrics.prefix(6)) { metric in
                VStack(alignment: .leading, spacing: 3) {
                    Text(metric.label)
                        .font(.system(size: 8, weight: .medium))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.tail)
                    Text(metric.valueText)
                        .font(.system(size: 11, weight: .bold, design: .rounded))
                        .lineLimit(1)
                    WidgetMetricBar(metric: metric)
                }
                .padding(5)
                .background(.primary.opacity(0.05), in: RoundedRectangle(cornerRadius: 8))
            }
        }
    }

    private var footer: some View {
        HStack(spacing: 5) {
            if item.fixedMetricUnavailable {
                Text("主指标暂不可用")
            } else if item.isStale {
                Text("缓存数据")
            } else if let fetchedAt = item.fetchedAt {
                Text(fetchedAt, style: .time)
            }
            Spacer(minLength: 0)
            if let resetText = item.primary?.resetText {
                Text(resetText)
            }
        }
        .font(.system(size: 8))
        .foregroundStyle(.secondary)
        .lineLimit(1)
    }
}

private struct WidgetProgressRing: View {
    let metric: WidgetMetricPresentation

    var body: some View {
        ZStack {
            Circle()
                .stroke(.primary.opacity(0.12), lineWidth: 6)
            Circle()
                .trim(from: 0, to: progress)
                .stroke(severityColor, style: StrokeStyle(lineWidth: 6, lineCap: .round))
                .rotationEffect(.degrees(-90))
            VStack(spacing: 1) {
                Text(metric.valueText)
                    .font(.system(size: 15, weight: .heavy, design: .rounded))
                    .lineLimit(1)
                Text(metric.label)
                    .font(.system(size: 7, weight: .medium))
                    .foregroundStyle(.secondary)
                    .lineLimit(1)
                    .truncationMode(.tail)
            }
            .padding(4)
        }
    }

    private var progress: Double {
        Double(metric.progressPercent ?? 0) / 100
    }

    private var severityColor: Color {
        switch metric.severity {
        case .normal: .blue
        case .warning: .orange
        case .critical: .red
        case .unavailable: .secondary
        }
    }
}

private struct WidgetMetricBar: View {
    let metric: WidgetMetricPresentation

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(.primary.opacity(0.12))
                Capsule()
                    .fill(severityColor)
                    .frame(width: proxy.size.width * progress)
            }
        }
        .frame(height: 4)
    }

    private var progress: Double {
        Double(metric.progressPercent ?? 0) / 100
    }

    private var severityColor: Color {
        switch metric.severity {
        case .normal: .blue
        case .warning: .orange
        case .critical: .red
        case .unavailable: .secondary
        }
    }
}

struct WidgetEmptyView: View {
    var body: some View {
        VStack(spacing: 7) {
            Image(systemName: "widget.small")
                .font(.system(size: 24))
                .foregroundStyle(.secondary)
            Text("打开应用添加凭证")
                .font(.system(size: 12, weight: .semibold))
                .lineLimit(1)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
    }
}
