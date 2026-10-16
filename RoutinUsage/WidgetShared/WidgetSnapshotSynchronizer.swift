import Foundation

@MainActor
final class WidgetSnapshotSynchronizer {
    private let store: WidgetSnapshotStore
    private let reloader: any WidgetReloading
    private let logWriter: any AppLogWriting
    private let now: @Sendable () -> Date
    private(set) var isDisabledForSession = false
    private var lastPublishedCredentials: [WidgetSnapshotCredential] = []

    init(
        store: WidgetSnapshotStore,
        reloader: any WidgetReloading,
        logWriter: any AppLogWriting = NoopAppLogWriter(),
        now: @escaping @Sendable () -> Date = Date.init
    ) {
        self.store = store
        self.reloader = reloader
        self.logWriter = logWriter
        self.now = now
    }

    func publish(states: [UUID: KeyUsageState], orderedKeyIDs: [UUID]) {
        guard !isDisabledForSession else {
            Task {
                await EnergyDiagnosticsBuffer.shared.record(
                    name: "widget_snapshot_skipped",
                    details: ["reason": "disabled_after_failure"]
                )
            }
            return
        }

        let bundle = makeBundle(states: states, orderedKeyIDs: orderedKeyIDs)
        guard bundle.credentials != lastPublishedCredentials else {
            Task {
                await EnergyDiagnosticsBuffer.shared.record(
                    name: "widget_snapshot_skipped",
                    details: ["reason": "unchanged"]
                )
            }
            return
        }

        do {
            try store.save(bundle)
            reloader.reloadAllTimelines()
            lastPublishedCredentials = bundle.credentials
            Task {
                await EnergyDiagnosticsBuffer.shared.record(
                    name: "widget_snapshot_saved",
                    details: ["credential_count": String(orderedKeyIDs.count)]
                )
            }
        } catch {
            isDisabledForSession = true
            let nsError = error as NSError
            let logWriter = logWriter
            Task {
                await EnergyDiagnosticsBuffer.shared.record(
                    name: "widget_snapshot_failed",
                    details: ["domain": String(nsError.domain), "code": String(nsError.code)]
                )
                await logWriter.log(
                    level: .error,
                    event: "widget_snapshot_write_failed",
                    details: "domain=\(nsError.domain) code=\(nsError.code)"
                )
            }
        }
    }

    private func makeBundle(
        states: [UUID: KeyUsageState],
        orderedKeyIDs: [UUID]
    ) -> WidgetSnapshotBundle {
        WidgetSnapshotBundle(
            schemaVersion: WidgetSnapshotBundle.currentSchemaVersion,
            generatedAt: now(),
            credentials: orderedKeyIDs.compactMap { keyID in
                states[keyID].map(makeCredential)
            }
        )
    }

    private func makeCredential(_ state: KeyUsageState) -> WidgetSnapshotCredential {
        let configuration = state.configuration
        return WidgetSnapshotCredential(
            id: configuration.id,
            providerID: configuration.providerID.rawValue,
            providerName: ProviderRegistry.builtInDescriptors
                .first { $0.id == configuration.providerID }?
                .displayName ?? configuration.providerID.rawValue,
            name: configuration.name,
            planName: state.snapshot?.planName ?? "",
            isEnabled: configuration.isEnabled,
            sortOrder: configuration.sortOrder,
            fetchedAt: state.snapshot?.fetchedAt,
            lastSuccessAt: state.lastSuccessAt,
            isStale: state.isStale,
            subscriptionEndAt: state.snapshot?.subscriptionEndAt,
            metrics: state.snapshot?.normalizedMetrics.map(makeMetric) ?? [],
            failure: makeFailure(state),
            isRefreshing: state.isRefreshing
        )
    }

    private func makeFailure(_ state: KeyUsageState) -> WidgetSnapshotFailure? {
        guard let error = state.error else { return nil }
        if state.failureIsAuthentication { return .authentication }
        switch error {
        case .invalidKey:
            return .authentication
        case .network:
            return .network
        case .invalidResponse:
            return .invalidResponse
        case .server:
            return .server
        case .noSubscription:
            return .unknown
        }
    }

    private func makeMetric(_ metric: NormalizedUsageMetric) -> WidgetSnapshotMetric {
        WidgetSnapshotMetric(
            id: metric.id,
            label: metric.label,
            used: metric.used,
            limit: metric.limit,
            remaining: metric.remaining,
            value: metric.value,
            unit: WidgetSnapshotMetricUnit(metric.unit),
            windowStart: metric.windowStart,
            windowEnd: metric.windowEnd,
            presentation: WidgetSnapshotMetricPresentation(metric.presentation),
            semantic: WidgetSnapshotMetricSemantic(metric.semantic),
            currencyCode: metric.currencyCode,
            healthState: WidgetSnapshotMetricHealthState(metric.healthState)
        )
    }
}

private extension WidgetSnapshotMetricUnit {
    init(_ unit: UsageMetricUnit) {
        switch unit {
        case .token: self = .token
        case .currency: self = .currency
        case .request: self = .request
        case .boolean: self = .boolean
        case .text: self = .text
        }
    }
}

private extension WidgetSnapshotMetricPresentation {
    init(_ presentation: NormalizedUsageMetricPresentation) {
        switch presentation {
        case .progress: self = .progress
        case .balance: self = .balance
        case .status: self = .status
        case .value: self = .value
        }
    }
}

private extension WidgetSnapshotMetricSemantic {
    init(_ semantic: NormalizedUsageMetricSemantic) {
        switch semantic {
        case .usedQuota: self = .usedQuota
        case .remainingQuota: self = .remainingQuota
        case .balance: self = .balance
        case .status: self = .status
        case .value: self = .value
        }
    }
}

private extension WidgetSnapshotMetricHealthState {
    init(_ healthState: UsageMetricHealthState) {
        switch healthState {
        case .normal: self = .normal
        case .warning: self = .warning
        case .critical: self = .critical
        case .unavailable: self = .unavailable
        case .stale: self = .stale
        case .unknown: self = .unknown
        }
    }
}
