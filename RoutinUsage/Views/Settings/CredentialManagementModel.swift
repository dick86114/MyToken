import Foundation
import Observation

struct CredentialOperationNotice: Equatable, Sendable {
    let title: String
    let message: String
}

enum CredentialAttention: Equatable, Sendable {
    case refreshFailed
    case subscriptionExpired
    case criticalMetric(NormalizedUsageMetric)
    case staleData
    case warningMetric(NormalizedUsageMetric)

    var title: String {
        switch self {
        case .refreshFailed:
            "刷新失败"
        case .subscriptionExpired:
            "订阅已过期"
        case let .criticalMetric(metric):
            metric.label.contains("余额") ? "余额不足" : "\(metric.label) 接近上限"
        case .staleData:
            "缓存已过期"
        case let .warningMetric(metric):
            "\(metric.label) 需注意"
        }
    }

    var metric: NormalizedUsageMetric? {
        switch self {
        case let .criticalMetric(metric), let .warningMetric(metric):
            metric
        case .refreshFailed, .subscriptionExpired, .staleData:
            nil
        }
    }

    var accessibilityText: String {
        switch self {
        case .refreshFailed:
            "需要注意，最近一次刷新失败"
        case .subscriptionExpired:
            "需要注意，订阅已过期"
        case let .criticalMetric(metric):
            "需要注意，\(Self.metricAccessibilityText(metric))"
        case .staleData:
            "需要注意，缓存数据已过期"
        case let .warningMetric(metric):
            "需要注意，\(Self.metricAccessibilityText(metric))"
        }
    }

    private static func metricAccessibilityText(
        _ metric: NormalizedUsageMetric
    ) -> String {
        switch metric.presentation {
        case .progress:
            guard let percent = metric.displayedPercent else {
                return metric.label
            }
            let clamped = min(max(percent, 0), 100)
            let direction = metric.displaysRemainingPercent ? "剩余" : "已用"
            return "\(metric.label)，\(direction) \(UsageFormatter.displayPercentText(clamped))"
        case .balance:
            let value = metric.value ?? metric.remaining
            return "\(metric.label) \(UsageFormatter.currencyText(value, currencyCode: metric.currencyCode))"
        case .status:
            return metric.label
        case .value:
            guard let value = metric.value else {
                return metric.label
            }
            switch metric.unit {
            case .currency:
                return "\(metric.label) \(UsageFormatter.currencyText(value, currencyCode: metric.currencyCode))"
            case .token:
                return "\(metric.label) \(UsageFormatter.compactMetricValue(value))"
            case .request:
                return "\(metric.label) \(UsageFormatter.numberText(value)) 次"
            case .boolean, .text:
                return metric.label
            }
        }
    }
}

struct CredentialProviderGroup: Identifiable, Equatable {
    let provider: ProviderDescriptor
    let states: [KeyUsageState]
    let attentionCount: Int

    var id: ProviderID { provider.id }

    var enabledCount: Int {
        states.filter { $0.configuration.isEnabled }.count
    }
}

@MainActor
@Observable
final class CredentialManagementModel {
    private let store: UsageStore
    private let settings: AppSettings
    @ObservationIgnored private let ordering: CredentialOrderingController
    @ObservationIgnored private let updateCredential: @MainActor (
        UUID,
        ValidatedCredentialInput
    ) async throws -> KeyEditorSaveResult
    @ObservationIgnored private let now: () -> Date

    var filter = CredentialFilter()
    var expandedProviderIDs: Set<ProviderID> = []
    var pendingDeletion: KeyConfiguration?
    private(set) var operationNotice: CredentialOperationNotice?

    init(
        store: UsageStore,
        settings: AppSettings,
        ordering: CredentialOrderingController,
        updateCredential: @escaping @MainActor (
            UUID,
            ValidatedCredentialInput
        ) async throws -> KeyEditorSaveResult,
        now: @escaping () -> Date = Date.init
    ) {
        self.store = store
        self.settings = settings
        self.ordering = ordering
        self.updateCredential = updateCredential
        self.now = now

        let usedProviderIDs = Set(store.states.values.map(\.configuration.providerID))
        let attentionProviderID = ProviderID.allCases.first { providerID in
            store.states.values.contains { state in
                state.configuration.providerID == providerID
                    && Self.attention(for: state, now: now()) != nil
            }
        }
        if let initialProviderID = attentionProviderID
            ?? ProviderID.allCases.first(where: usedProviderIDs.contains) {
            expandedProviderIDs = [initialProviderID]
        }
    }

    convenience init(
        environment: AppEnvironment,
        ordering: CredentialOrderingController
    ) {
        self.init(
            store: environment.store,
            settings: environment.settings,
            ordering: ordering,
            updateCredential: { id, input in
                try await environment.updateValidatedCredential(id: id, input: input)
            }
        )
    }

    var hasCredentials: Bool {
        !store.orderedKeyIDs.isEmpty
    }

    var storeCount: Int {
        store.orderedKeyIDs.count
    }

    var enabledCount: Int {
        allStates.filter { $0.configuration.isEnabled }.count
    }

    var disabledCount: Int {
        allStates.filter { !$0.configuration.isEnabled }.count
    }

    var attentionCount: Int {
        allStates.filter { attention(for: $0) != nil }.count
    }

    var providerCount: Int {
        Set(allStates.map(\.configuration.providerID)).count
    }

    var allStates: [KeyUsageState] {
        let statesByID = store.states
        var seenIDs = Set<UUID>()
        let orderedIDs = settings.displayOrder.popoverCredentialIDs
            + store.orderedKeyIDs.filter {
                !settings.displayOrder.popoverCredentialIDs.contains($0)
            }

        return orderedIDs.compactMap { id in
            guard let state = statesByID[id], seenIDs.insert(id).inserted else {
                return nil
            }
            return state
        }
    }

    var visibleStates: [KeyUsageState] {
        allStates.filter { state in
            matchesStatus(state)
                && matchesProvider(state)
                && matchesSearch(state)
        }
    }

    var groups: [CredentialProviderGroup] {
        makeGroups(from: visibleStates).enumerated()
            .sorted { lhs, rhs in
                let lhsNeedsAttention = lhs.element.attentionCount > 0
                let rhsNeedsAttention = rhs.element.attentionCount > 0
                if lhsNeedsAttention != rhsNeedsAttention {
                    return lhsNeedsAttention
                }
                return lhs.offset < rhs.offset
            }
            .map(\.element)
    }

    var providerFilterCounts: [ProviderID: Int] {
        var counts: [ProviderID: Int] = [:]
        for state in allStates where matchesStatus(state) && matchesSearch(state) {
            counts[state.configuration.providerID, default: 0] += 1
        }
        return counts
    }

    var providerFilterDescriptors: [ProviderDescriptor] {
        return ProviderID.allCases.compactMap { providerID in
            guard providerFilterCounts[providerID, default: 0] > 0
                || filter.provider == providerID else {
                return nil
            }
            return ProviderRegistry.builtInDescriptors.first { $0.id == providerID }
        }
    }

    var filteredCredentialCount: Int {
        providerFilterCounts.values.reduce(0, +)
    }

    var allVisibleGroupsExpanded: Bool {
        !groups.isEmpty && groups.allSatisfy {
            expandedProviderIDs.contains($0.provider.id)
        }
    }

    var isFilterActive: Bool {
        filter.status != .all
            || filter.provider != nil
            || !filter.searchText.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
    }

    func attention(for state: KeyUsageState) -> CredentialAttention? {
        Self.attention(for: state, now: now())
    }

    func attentionMetric(for state: KeyUsageState) -> NormalizedUsageMetric? {
        if let metric = attention(for: state)?.metric,
           metric.presentation != .status {
            return metric
        }
        guard
            state.configuration.isEnabled,
            let metrics = state.snapshot?.normalizedMetrics
        else {
            return nil
        }
        return metrics.first {
            ($0.healthState == .critical || $0.healthState == .warning)
                && $0.presentation != .status
        }
    }

    func setProviderExpanded(_ providerID: ProviderID, expanded: Bool) {
        if expanded {
            expandedProviderIDs.insert(providerID)
        } else {
            expandedProviderIDs.remove(providerID)
        }
    }

    func toggleProviderExpansion(_ providerID: ProviderID) {
        setProviderExpanded(
            providerID,
            expanded: !expandedProviderIDs.contains(providerID)
        )
    }

    func toggleAllVisibleGroups() {
        let visibleProviderIDs = groups.map(\.provider.id)
        if allVisibleGroupsExpanded {
            expandedProviderIDs.subtract(visibleProviderIDs)
        } else {
            expandedProviderIDs.formUnion(visibleProviderIDs)
        }
    }

    func expandGroupsForActiveFilter() {
        guard isFilterActive else {
            return
        }
        expandedProviderIDs.formUnion(groups.map(\.provider.id))
    }

    private func providerDescriptor(for state: KeyUsageState) -> ProviderDescriptor? {
        ProviderRegistry.builtInDescriptors.first {
            $0.id == state.configuration.providerID
        }
    }

    func addValidatedCredential(
        _ input: ValidatedCredentialInput
    ) async throws -> KeyEditorSaveResult {
        let previousIDs = Set(store.orderedKeyIDs)
        let result = try await ordering.addValidatedCredential(input)
        if let addedID = store.orderedKeyIDs.first(where: { !previousIDs.contains($0) }),
           let providerID = store.state(for: addedID)?.configuration.providerID {
            expandedProviderIDs.insert(providerID)
        }
        return result
    }

    func updateValidatedCredential(
        id: UUID,
        input: ValidatedCredentialInput
    ) async throws -> KeyEditorSaveResult {
        try await updateCredential(id, input)
    }

    func setEnabled(_ id: UUID, enabled: Bool) {
        do {
            try ordering.setEnabled(id, enabled: enabled)
            if enabled, let providerID = store.state(for: id)?.configuration.providerID {
                expandedProviderIDs.insert(providerID)
            }
        } catch {
            operationNotice = CredentialOperationNotice(
                title: "无法完成操作",
                message: "无法更新凭证状态，请稍后重试"
            )
        }
    }

    func deletePending() {
        guard let configuration = pendingDeletion else {
            return
        }
        pendingDeletion = nil
        do {
            let outcome = try ordering.delete(configuration.id)
            settings.removeUsagePreferences(for: configuration.id)
            if !store.states.values.contains(where: {
                $0.configuration.providerID == configuration.providerID
            }) {
                expandedProviderIDs.remove(configuration.providerID)
            }
            if outcome == .cacheCleanupFailed {
                operationNotice = CredentialOperationNotice(
                    title: "缓存清理失败",
                    message: "凭证已删除，但用量缓存未能清理。"
                )
            }
        } catch {
            operationNotice = CredentialOperationNotice(
                title: "无法完成操作",
                message: "无法删除凭证，请稍后重试"
            )
        }
    }

    func clearOperationNotice() {
        operationNotice = nil
    }

    func accessibilitySummary(
        _ state: KeyUsageState,
        providerName: String,
        planType: String,
        attention: CredentialAttention? = nil
    ) -> String {
        let status = state.configuration.isEnabled ? "已启用" : "已停用"
        var parts = [
            state.configuration.displayName,
            providerName,
            planType,
            status
        ]
        if let attention {
            parts.append(attention.accessibilityText)
        }
        return parts.joined(separator: "，")
    }

    func planTitle(for configuration: KeyConfiguration) -> String {
        if configuration.providerID == .volcengine {
            return configuration.metadata["planType"] == "coding" ? "Coding Plan" : "Agent Plan"
        }
        if configuration.providerID == .xiaomi {
            return configuration.metadata["usageKind"] == "plan" ? "Token Plan" : "API 按量"
        }
        switch configuration.credentialKind {
        case .bearerAPIKey:
            return configuration.providerID == .routin ? "Plan Key" : "Bearer Token"
        case .apiKey:
            return "API Key"
        case .accessKeyPair:
            return "Access Key Pair"
        }
    }

    private func matchesStatus(_ state: KeyUsageState) -> Bool {
        switch filter.status {
        case .all:
            true
        case .enabled:
            state.configuration.isEnabled
        case .attention:
            attention(for: state) != nil
        case .disabled:
            !state.configuration.isEnabled
        }
    }

    private func matchesProvider(_ state: KeyUsageState) -> Bool {
        filter.provider == nil || filter.provider == state.configuration.providerID
    }

    private func matchesSearch(_ state: KeyUsageState) -> Bool {
        let query = filter.searchText.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !query.isEmpty else {
            return true
        }
        let providerName = providerDescriptor(for: state)?.displayName
            ?? state.configuration.providerID.rawValue
        return state.configuration.displayName.localizedCaseInsensitiveContains(query)
            || providerName.localizedCaseInsensitiveContains(query)
    }

    private func makeGroups(
        from states: [KeyUsageState]
    ) -> [CredentialProviderGroup] {
        ProviderID.allCases.compactMap { providerID in
            let providerStates = states.filter {
                $0.configuration.providerID == providerID
            }
            guard
                !providerStates.isEmpty,
                let descriptor = ProviderRegistry.builtInDescriptors.first(where: {
                    $0.id == providerID
                })
            else {
                return nil
            }
            return CredentialProviderGroup(
                provider: descriptor,
                states: providerStates,
                attentionCount: providerStates.filter { attention(for: $0) != nil }.count
            )
        }
    }

    private static func attention(
        for state: KeyUsageState,
        now: Date
    ) -> CredentialAttention? {
        guard state.configuration.isEnabled else {
            return nil
        }
        if state.error != nil {
            return .refreshFailed
        }
        if let subscriptionEndAt = state.snapshot?.subscriptionEndAt,
           subscriptionEndAt <= now {
            return .subscriptionExpired
        }
        if let criticalMetric = state.snapshot?.normalizedMetrics.first(where: {
            $0.healthState == .critical
        }) {
            return .criticalMetric(criticalMetric)
        }
        if state.isStale {
            return .staleData
        }
        if let warningMetric = state.snapshot?.normalizedMetrics.first(where: {
            $0.healthState == .warning
        }) {
            return .warningMetric(warningMetric)
        }
        return nil
    }
}
