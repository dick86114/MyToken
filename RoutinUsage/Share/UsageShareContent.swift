import Foundation

enum UsageShareTemplate: String, CaseIterable, Identifiable, Equatable, Sendable {
    case ticket
    case ticketLight
    case dark
    case light

    var id: String { rawValue }

    var title: String {
        switch self {
        case .ticket: return "票根 Pass"
        case .ticketLight: return "浅色票根"
        case .dark: return "暗色极客"
        case .light: return "雅致浅色"
        }
    }

    var previewTag: String {
        switch self {
        case .ticket: return "票根 Pass 模式"
        case .ticketLight: return "浅色票根模式"
        case .dark: return "极客暗色模式"
        case .light: return "雅致浅色模式"
        }
    }
}

struct UsageShareMetricItem: Equatable, Identifiable, Sendable {
    let id: String
    let title: String
    let headline: String
    let percent: Double?
    let amountDetails: [String]
    let timeDetails: [String]
    let usedText: String?
    let limitText: String?
    let remainingAmountText: String?
    let resetBadgeText: String?
    let companionText: String?
    let health: UsageMetricHealthState
    let spansFullWidth: Bool
}

struct UsageShareContent: Equatable, Sendable {
    let displayName: String
    let providerName: String
    let planName: String
    let subtitle: String
    let subscriptionStartText: String?
    let subscriptionEndText: String?
    let expiryText: String?
    let cycleRemainingText: String?
    let tokenPercentText: String?
    let groupMultiplierText: String?
    let detectionText: String?
    let metrics: [UsageShareMetricItem]
    let capturedAt: Date
    let capturedAtText: String
    let providerID: ProviderID
    let passCode: String
    let avatarLetter: String
    let isAvailable: Bool
}

struct UsageShareDraft: Equatable, Sendable {
    var displayName: String
    var subtitle: String
    var note: String
    var showsSubtitle: Bool
    var showsSubscriptionDates: Bool
    var showsExpiry: Bool
    var showsTokenPercent: Bool
    var showsGroupMultiplier: Bool
    var showsDetection: Bool
    var showsAmounts: Bool
    var showsResetTimes: Bool
    var showsWatermark: Bool
    var showsStatus: Bool
    var showsNote: Bool
    var hiddenMetricIDs: Set<String>
    var template: UsageShareTemplate

    static func make(from content: UsageShareContent) -> UsageShareDraft {
        UsageShareDraft(
            displayName: content.displayName,
            subtitle: content.planName.isEmpty ? content.providerName : content.planName,
            note: "",
            showsSubtitle: true,
            showsSubscriptionDates: content.subscriptionStartText != nil || content.subscriptionEndText != nil || content.cycleRemainingText != nil,
            showsExpiry: content.expiryText != nil || content.cycleRemainingText != nil,
            showsTokenPercent: content.tokenPercentText != nil,
            showsGroupMultiplier: content.groupMultiplierText != nil,
            showsDetection: content.detectionText != nil,
            showsAmounts: true,
            showsResetTimes: true,
            showsWatermark: true,
            showsStatus: true,
            showsNote: true,
            hiddenMetricIDs: [],
            template: .ticket
        )
    }

    func isMetricVisible(_ id: String) -> Bool {
        !hiddenMetricIDs.contains(id)
    }

    mutating func setMetricVisible(_ id: String, _ visible: Bool) {
        if visible {
            hiddenMetricIDs.remove(id)
        } else {
            hiddenMetricIDs.insert(id)
        }
    }

    mutating func showAll(from content: UsageShareContent) {
        showsSubtitle = true
        showsSubscriptionDates = true
        showsExpiry = true
        showsTokenPercent = true
        showsGroupMultiplier = true
        showsDetection = true
        showsAmounts = true
        showsResetTimes = true
        showsWatermark = true
        showsStatus = true
        showsNote = true
        hiddenMetricIDs = []
    }
}

struct UsageShareRenderedCard: Equatable, Sendable {
    let displayName: String
    let providerName: String
    let subtitle: String
    let note: String?
    let subscriptionStartText: String?
    let subscriptionEndText: String?
    let expiryText: String?
    let cycleRemainingText: String?
    let tokenPercentText: String?
    let groupMultiplierText: String?
    let detectionText: String?
    let metrics: [UsageShareMetricItem]
    let capturedAtText: String
    let showsWatermark: Bool
    let showsStatus: Bool
    let template: UsageShareTemplate
    let passCode: String
    let avatarLetter: String
    let isAvailable: Bool
}

enum UsageShareContentBuilder {
    static func build(
        state: KeyUsageState,
        detectionRecord: CodexGroupDetectionRecord? = nil,
        now: Date = .now,
        timeZone: TimeZone = .current
    ) -> UsageShareContent? {
        guard let snapshot = state.snapshot else {
            return nil
        }

        let providerID = state.configuration.providerID
        let providerName = providerDisplayName(providerID)
        let planName = snapshot.planName
        let subtitle = UsageRowPresentation.subscriptionDescription(
            providerID: providerID,
            planName: planName
        )
        let hasSubscriptionDates = snapshot.kind == .periodic
            && (snapshot.subscriptionStartAt != nil || snapshot.subscriptionEndAt != nil)
        let currentGroup = UsageFormatter.currentGroupMultiplier(
            in: snapshot.groupMultipliers,
            matching: detectionRecord?.groupName
        )
        let tokenPercent: String?
        if let token = snapshot.token, token.percent.isFinite {
            tokenPercent = UsageFormatter.displayPercentText(token.percent)
        } else {
            tokenPercent = nil
        }

        let detectionText: String?
        if let groupName = detectionRecord?.groupName, !groupName.isEmpty {
            detectionText = "Codex 当前分组：\(groupName)"
        } else {
            detectionText = nil
        }

        let credentialID = state.configuration.id
        let isExpired = snapshot.subscriptionEndAt.map { $0 <= now } ?? false
        return UsageShareContent(
            displayName: state.configuration.displayName,
            providerName: providerName,
            planName: planName,
            subtitle: subtitle,
            subscriptionStartText: hasSubscriptionDates
                ? UsageFormatter.subscriptionDateText(snapshot.subscriptionStartAt, timeZone: timeZone)
                : nil,
            subscriptionEndText: hasSubscriptionDates
                ? UsageFormatter.subscriptionDateText(snapshot.subscriptionEndAt, timeZone: timeZone)
                : nil,
            expiryText: UsageFormatter.subscriptionExpiryText(until: snapshot.subscriptionEndAt, now: now),
            cycleRemainingText: cycleRemainingText(until: snapshot.subscriptionEndAt, now: now, timeZone: timeZone),
            tokenPercentText: tokenPercent,
            groupMultiplierText: currentGroup.map { UsageFormatter.groupMultiplierText([$0]) },
            detectionText: detectionText,
            metrics: metricItems(
                snapshot: snapshot,
                providerID: providerID,
                now: now,
                timeZone: timeZone
            ),
            capturedAt: now,
            capturedAtText: timestampText(now, timeZone: timeZone),
            providerID: providerID,
            passCode: passCode(for: credentialID),
            avatarLetter: avatarLetter(for: state.configuration.displayName, providerID: providerID),
            isAvailable: state.error == nil && !isExpired
        )
    }

    static func render(content: UsageShareContent, draft: UsageShareDraft) -> UsageShareRenderedCard {
        let trimmedNote = draft.note.trimmingCharacters(in: .whitespacesAndNewlines)
        let metrics = content.metrics.compactMap { item -> UsageShareMetricItem? in
            guard draft.isMetricVisible(item.id) else { return nil }
            return UsageShareMetricItem(
                id: item.id,
                title: item.title,
                headline: item.headline,
                percent: item.percent,
                amountDetails: draft.showsAmounts ? item.amountDetails : [],
                timeDetails: draft.showsResetTimes ? item.timeDetails : [],
                usedText: draft.showsAmounts ? item.usedText : nil,
                limitText: draft.showsAmounts ? item.limitText : nil,
                remainingAmountText: draft.showsAmounts ? item.remainingAmountText : nil,
                resetBadgeText: draft.showsResetTimes ? item.resetBadgeText : nil,
                companionText: item.companionText,
                health: item.health,
                spansFullWidth: item.spansFullWidth
            )
        }

        let note = trimmedNote.isEmpty || !draft.showsNote ? nil : trimmedNote
        return UsageShareRenderedCard(
            displayName: trimmed(draft.displayName, fallback: content.displayName),
            providerName: content.providerName,
            subtitle: draft.showsSubtitle ? trimmed(draft.subtitle, fallback: content.planName.isEmpty ? content.providerName : content.planName) : "",
            note: note,
            subscriptionStartText: draft.showsSubscriptionDates ? content.subscriptionStartText : nil,
            subscriptionEndText: draft.showsSubscriptionDates ? content.subscriptionEndText : nil,
            expiryText: draft.showsExpiry ? content.expiryText : nil,
            cycleRemainingText: (draft.showsSubscriptionDates || draft.showsExpiry) ? content.cycleRemainingText : nil,
            tokenPercentText: draft.showsTokenPercent ? content.tokenPercentText : nil,
            groupMultiplierText: draft.showsGroupMultiplier ? content.groupMultiplierText : nil,
            detectionText: draft.showsDetection ? content.detectionText : nil,
            metrics: metrics,
            capturedAtText: content.capturedAtText,
            showsWatermark: draft.showsWatermark,
            showsStatus: draft.showsStatus,
            template: draft.template,
            passCode: content.passCode,
            avatarLetter: avatarLetter(for: trimmed(draft.displayName, fallback: content.displayName), providerID: content.providerID),
            isAvailable: content.isAvailable
        )
    }

    static func fileName(displayName: String, date: Date, timeZone: TimeZone = .current) -> String {
        let trimmed = displayName.trimmingCharacters(in: .whitespacesAndNewlines)
        let safe = trimmed.isEmpty ? "账户" : sanitizeFileName(trimmed)
        return "MyToken-\(safe)-\(fileStamp(date, timeZone: timeZone)).png"
    }

    private static func metricItems(
        snapshot: UsageSnapshot,
        providerID: ProviderID,
        now: Date,
        timeZone: TimeZone
    ) -> [UsageShareMetricItem] {
        // GLM 卡片本身不展示已用/剩余金额，分享图与卡片保持一致。
        let showsAmountDetails = providerID != .glm

        let source = snapshot.normalizedMetrics.filter { metric in
            !metric.id.hasSuffix("-cost")
        }

        return source.map { metric in
            makeItem(
                metric: metric,
                allMetrics: snapshot.normalizedMetrics,
                showsAmountDetails: showsAmountDetails,
                now: now,
                timeZone: timeZone
            )
        }
    }

    private static func makeItem(
        metric: NormalizedUsageMetric,
        allMetrics: [NormalizedUsageMetric],
        showsAmountDetails: Bool,
        now: Date,
        timeZone: TimeZone
    ) -> UsageShareMetricItem {
        let spansFullWidth = metric.id == "monthly" || metric.id == "quota-progress"
        switch metric.presentation {
        case .progress:
            var amounts: [String] = []
            if showsAmountDetails, let used = metric.used, let limit = metric.limit {
                amounts.append("已用 \(amountText(used, metric: metric)) / \(amountText(limit, metric: metric))")
                if let remaining = metric.remaining {
                    amounts.append("剩余 \(amountText(remaining, metric: metric))")
                }
            }
            if let cost = allMetrics.first(where: { $0.id == "\(metric.id)-cost" }) {
                amounts.append("≈ \(UsageFormatter.currencyText(cost.value, currencyCode: cost.currencyCode))")
            }

            var times: [String] = []
            if let windowEnd = metric.windowEnd {
                times.append("重置 \(UsageFormatter.resetTime(windowEnd, now: now, timeZone: timeZone))")
                times.append("剩余 \(UsageFormatter.remainingDurationText(until: windowEnd, now: now))")
            }

            let used = showsAmountDetails ? metric.used.map { amountText($0, metric: metric) } : nil
            let limit = showsAmountDetails ? metric.limit.map { amountText($0, metric: metric) } : nil
            let remaining = showsAmountDetails ? metric.remaining.map { amountText($0, metric: metric) } : nil
            let resetBadge = metric.windowEnd.map { resetBadgeText(until: $0, now: now, timeZone: timeZone) }
            let companion = companionCostText(for: metric, allMetrics: allMetrics)
            let headline = UsageFormatter.displayPercentText(metric.displayedPercent)
            return UsageShareMetricItem(
                id: metric.id,
                title: metric.label,
                headline: headline,
                percent: metric.displayedPercent,
                amountDetails: amounts,
                timeDetails: times,
                usedText: used,
                limitText: limit,
                remainingAmountText: remaining,
                resetBadgeText: resetBadge,
                companionText: companion,
                health: metric.healthState,
                spansFullWidth: occupiesFullWidth(base: spansFullWidth, headline: headline, details: amounts + times)
            )
        case .balance:
            let headline = UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode)
            return UsageShareMetricItem(
                id: metric.id,
                title: metric.label.isEmpty ? "账户余额" : metric.label,
                headline: headline,
                percent: nil,
                amountDetails: [],
                timeDetails: [],
                usedText: nil,
                limitText: nil,
                remainingAmountText: nil,
                resetBadgeText: nil,
                companionText: nil,
                health: metric.healthState,
                spansFullWidth: occupiesFullWidth(base: false, headline: headline, details: [])
            )
        case .status:
            return UsageShareMetricItem(
                id: metric.id,
                title: metric.label.isEmpty ? "账户状态" : metric.label,
                headline: metric.healthState == .unavailable ? "不可用" : "可用",
                percent: nil,
                amountDetails: [],
                timeDetails: [],
                usedText: nil,
                limitText: nil,
                remainingAmountText: nil,
                resetBadgeText: nil,
                companionText: nil,
                health: metric.healthState,
                spansFullWidth: false
            )
        case .value:
            let headline = valueText(metric)
            let amounts = companionCostText(for: metric, allMetrics: allMetrics).map { [$0] } ?? []
            return UsageShareMetricItem(
                id: metric.id,
                title: metric.label,
                headline: headline,
                percent: nil,
                amountDetails: amounts,
                timeDetails: [],
                usedText: nil,
                limitText: nil,
                remainingAmountText: nil,
                resetBadgeText: nil,
                companionText: amounts.first,
                health: metric.healthState,
                spansFullWidth: occupiesFullWidth(base: false, headline: headline, details: amounts)
            )
        }
    }


    private static func occupiesFullWidth(base: Bool, headline: String, details: [String]) -> Bool {
        if base { return true }
        if headline.count > 9 { return true }
        return details.contains { $0.count > 18 }
    }

    private static func amountText(_ value: Decimal, metric: NormalizedUsageMetric) -> String {
        switch metric.unit {
        case .currency:
            return UsageFormatter.currencyText(value, currencyCode: metric.currencyCode)
        case .token:
            return UsageFormatter.numberText(value, grouping: true)
        case .request, .boolean, .text:
            return UsageFormatter.numberText(value, grouping: true)
        }
    }

    private static func valueText(_ metric: NormalizedUsageMetric) -> String {
        switch metric.unit {
        case .currency:
            return UsageFormatter.currencyText(metric.value, currencyCode: metric.currencyCode)
        case .token:
            return UsageFormatter.exactTokenText(metric.value)
        case .request:
            return "\(UsageFormatter.numberText(metric.value, grouping: true)) 次"
        case .boolean:
            if metric.healthState == .unavailable {
                return "不可用"
            }
            if metric.value == 1 {
                return "可用"
            }
            return UsageFormatter.numberText(metric.value, grouping: true)
        case .text:
            return UsageFormatter.numberText(metric.value, grouping: true)
        }
    }

    private static func companionCostText(
        for metric: NormalizedUsageMetric,
        allMetrics: [NormalizedUsageMetric]
    ) -> String? {
        guard let cost = allMetrics.first(where: { $0.id == "\(metric.id)-cost" }) else {
            return nil
        }
        return "≈ \(UsageFormatter.currencyText(cost.value, currencyCode: cost.currencyCode))"
    }

    private static func resetBadgeText(until end: Date, now: Date, timeZone: TimeZone) -> String {
        let duration = UsageFormatter.remainingDurationText(until: end, now: now)
        let clock = UsageFormatter.resetTime(end, now: now, timeZone: timeZone)
        if duration == "已结束" {
            return "已结束"
        }
        return "\(duration) 后重置 (\(clock))"
    }

    private static func cycleRemainingText(until end: Date?, now: Date, timeZone: TimeZone) -> String? {
        guard let end else { return nil }
        if end <= now {
            return "已过期"
        }
        let days = max(1, Int(end.timeIntervalSince(now) / 86_400))
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timeZone
        formatter.dateFormat = "MM.dd"
        return "余 \(days) 天 (\(formatter.string(from: end)))"
    }

    private static func passCode(for id: UUID) -> String {
        let hex = id.uuidString.replacingOccurrences(of: "-", with: "").prefix(4).uppercased()
        return "PASS #TK-\(hex)"
    }

    private static func avatarLetter(for name: String, providerID: ProviderID) -> String {
        let trimmed = name.trimmingCharacters(in: .whitespacesAndNewlines)
        if let first = trimmed.first {
            return String(first).uppercased()
        }
        let code = ProviderRegistry.builtInDescriptors.first(where: { $0.id == providerID })?.shortCode
        return String(code?.prefix(1) ?? "M")
    }

    private static func providerDisplayName(_ providerID: ProviderID) -> String {
        ProviderRegistry.builtInDescriptors.first(where: { $0.id == providerID })?.displayName
            ?? providerID.rawValue
    }

    private static func timestampText(_ date: Date, timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "zh_CN")
        formatter.timeZone = timeZone
        formatter.dateFormat = "yyyy.MM.dd HH:mm"
        return formatter.string(from: date)
    }

    private static func fileStamp(_ date: Date, timeZone: TimeZone) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = timeZone
        formatter.dateFormat = "yyyyMMdd-HHmm"
        return formatter.string(from: date)
    }

    private static func sanitizeFileName(_ name: String) -> String {
        let invalid = CharacterSet(charactersIn: "/\\:?*\"<>|")
        let cleaned = name.unicodeScalars.map { invalid.contains($0) ? "-" : Character($0) }
        let compact = String(cleaned)
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return compact.isEmpty ? "账户" : compact
    }

    private static func trimmed(_ value: String, fallback: String) -> String {
        let text = value.trimmingCharacters(in: .whitespacesAndNewlines)
        return text.isEmpty ? fallback : text
    }
}

struct UsageSharePresentation: Identifiable, Equatable {
    let state: KeyUsageState
    let detectionRecord: CodexGroupDetectionRecord?

    var id: UUID { state.configuration.id }
}
