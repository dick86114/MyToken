import CryptoKit
import Foundation
import Observation

enum UsageDisplayError: Equatable, Sendable {
    case noSubscription
    case invalidKey
    case network
    case invalidResponse
    case server(statusCode: Int)
}

enum UsageStoreError: Error, Equatable, Sendable {
    case invalidName
    case invalidSecret
    case invalidKey
    case network
    case invalidResponse
    case server(statusCode: Int)
    case persistence
}

struct KeyUsageState: Equatable, Sendable {
    var configuration: KeyConfiguration
    var snapshot: UsageSnapshot?
    var lastSuccessAt: Date?
    var isRefreshing: Bool
    var isStale: Bool
    var error: UsageDisplayError?
}

@MainActor
@Observable
final class UsageStore {
    private(set) var states: [UUID: KeyUsageState] = [:]
    private(set) var orderedKeyIDs: [UUID] = []
    private(set) var selectedKeyID: UUID?
    private(set) var isRefreshing = false

    @ObservationIgnored private let keyRepository: KeyRepository
    @ObservationIgnored private let keychain: any KeychainStoring
    @ObservationIgnored private let apiClient: any UsageFetching
    @ObservationIgnored private let cache: any UsageCaching
    @ObservationIgnored private let alertEvaluator: AlertEvaluator
    @ObservationIgnored private let notificationSender: any NotificationSending
    @ObservationIgnored private let defaults: UserDefaults
    @ObservationIgnored private let refreshMinutes: Int
    @ObservationIgnored private let thresholds: AlertThresholds
    @ObservationIgnored private let notificationsEnabled: Bool
    @ObservationIgnored private let now: @Sendable () -> Date
    @ObservationIgnored private var refreshingKeyIDs: Set<UUID> = []

    private static let selectedKeyStorageKey = "selectedKeyID"

    init(
        keyRepository: KeyRepository,
        keychain: any KeychainStoring,
        apiClient: any UsageFetching,
        cache: any UsageCaching,
        alertEvaluator: AlertEvaluator,
        notificationSender: any NotificationSending,
        defaults: UserDefaults = .standard,
        refreshMinutes: Int = 5,
        thresholds: AlertThresholds = AlertThresholds(),
        notificationsEnabled: Bool = true,
        now: @escaping @Sendable () -> Date = Date.init
    ) {
        self.keyRepository = keyRepository
        self.keychain = keychain
        self.apiClient = apiClient
        self.cache = cache
        self.alertEvaluator = alertEvaluator
        self.notificationSender = notificationSender
        self.defaults = defaults
        self.refreshMinutes = refreshMinutes
        self.thresholds = thresholds
        self.notificationsEnabled = notificationsEnabled
        self.now = now

        restoreState()
    }

    func state(for keyID: UUID) -> KeyUsageState? {
        states[keyID]
    }

    func refreshAll() async {
        synchronizeConfigurations()
        let requests = orderedKeyIDs.compactMap(prepareRefreshRequest(for:))
        await perform(requests)
    }

    func refresh(keyID: UUID) async {
        synchronizeConfigurations()
        guard let request = prepareRefreshRequest(for: keyID) else {
            return
        }
        await perform([request])
    }

    func selectKey(_ id: UUID) {
        guard states[id] != nil else {
            return
        }
        selectedKeyID = id
        persistSelection()
    }

    func addValidatedKey(name: String, secret: String) async throws {
        let normalizedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedName.isEmpty else {
            throw UsageStoreError.invalidName
        }
        guard secret.hasPrefix("plan-"), secret.count > "plan-".count else {
            throw UsageStoreError.invalidSecret
        }

        let validationTime = now()
        let result: UsageSnapshot?
        do {
            result = try await apiClient.fetchUsage(apiKey: secret, now: validationTime)
        } catch {
            throw Self.storeError(from: error)
        }

        let configuration: KeyConfiguration
        do {
            configuration = try keyRepository.add(name: normalizedName, secret: secret)
        } catch {
            throw Self.storeError(from: error)
        }

        orderedKeyIDs.append(configuration.id)
        states[configuration.id] = KeyUsageState(
            configuration: configuration,
            snapshot: result,
            lastSuccessAt: result?.fetchedAt ?? validationTime,
            isRefreshing: false,
            isStale: false,
            error: result == nil ? .noSubscription : nil
        )
        if selectedKeyID == nil {
            selectedKeyID = configuration.id
            persistSelection()
        }

        if let result {
            try? cache.save(result, for: configuration.id)
            await evaluateNotifications(for: configuration, snapshot: result)
        } else {
            try? cache.delete(for: configuration.id)
        }
    }

    func deleteKey(_ id: UUID) throws {
        do {
            try keyRepository.delete(id: id)
        } catch {
            throw Self.storeError(from: error)
        }

        var cacheDeletionFailed = false
        do {
            try cache.delete(for: id)
        } catch {
            cacheDeletionFailed = true
        }
        alertEvaluator.clearState(for: id)
        refreshingKeyIDs.remove(id)
        states.removeValue(forKey: id)
        orderedKeyIDs.removeAll { $0 == id }
        isRefreshing = !refreshingKeyIDs.isEmpty
        if selectedKeyID == id {
            selectedKeyID = orderedKeyIDs.first
            persistSelection()
        }
        if cacheDeletionFailed {
            throw UsageStoreError.persistence
        }
    }

    private func restoreState() {
        let currentTime = now()
        let configurations = keyRepository.list()
        orderedKeyIDs = configurations.map(\.id)
        states = Dictionary(uniqueKeysWithValues: configurations.map { configuration in
            let cached = try? cache.load(for: configuration.id)
            return (
                configuration.id,
                KeyUsageState(
                    configuration: configuration,
                    snapshot: cached,
                    lastSuccessAt: cached?.fetchedAt,
                    isRefreshing: false,
                    isStale: cached.map {
                        UsageFreshness.isStale(
                            lastSuccess: $0.fetchedAt,
                            now: currentTime,
                            refreshMinutes: refreshMinutes
                        )
                    } ?? false,
                    error: nil
                )
            )
        })

        let storedID = defaults.string(forKey: Self.selectedKeyStorageKey)
            .flatMap(UUID.init(uuidString:))
        selectedKeyID = storedID.flatMap { states[$0] == nil ? nil : $0 } ?? orderedKeyIDs.first
        persistSelection()
    }

    private func synchronizeConfigurations() {
        let configurations = keyRepository.list()
        let validIDs = Set(configurations.map(\.id))
        states = states.filter { validIDs.contains($0.key) }
        orderedKeyIDs = configurations.map(\.id)

        for configuration in configurations {
            if var state = states[configuration.id] {
                state.configuration = configuration
                states[configuration.id] = state
            } else {
                let cached = try? cache.load(for: configuration.id)
                states[configuration.id] = KeyUsageState(
                    configuration: configuration,
                    snapshot: cached,
                    lastSuccessAt: cached?.fetchedAt,
                    isRefreshing: false,
                    isStale: cached.map {
                        UsageFreshness.isStale(
                            lastSuccess: $0.fetchedAt,
                            now: now(),
                            refreshMinutes: refreshMinutes
                        )
                    } ?? false,
                    error: nil
                )
            }
        }

        if selectedKeyID.flatMap({ states[$0] }) == nil {
            selectedKeyID = orderedKeyIDs.first
            persistSelection()
        }
    }

    private func prepareRefreshRequest(for keyID: UUID) -> RefreshRequest? {
        guard
            !refreshingKeyIDs.contains(keyID),
            var state = states[keyID]
        else {
            return nil
        }

        let secret: String
        do {
            guard let storedSecret = try keychain.read(for: keyID) else {
                applyFailure(.invalidKey, to: keyID)
                return nil
            }
            secret = storedSecret
        } catch {
            applyFailure(.invalidResponse, to: keyID)
            return nil
        }

        refreshingKeyIDs.insert(keyID)
        state.isRefreshing = true
        state.error = nil
        states[keyID] = state
        isRefreshing = true
        return RefreshRequest(
            keyID: keyID,
            secret: secret,
            credentialFingerprint: CredentialFingerprint(secret: secret),
            requestedAt: now()
        )
    }

    private func perform(_ requests: [RefreshRequest]) async {
        guard !requests.isEmpty else {
            isRefreshing = !refreshingKeyIDs.isEmpty
            return
        }

        let apiClient = apiClient
        await withTaskGroup(of: RefreshOutcome.self) { group in
            for request in requests {
                group.addTask {
                    do {
                        let snapshot = try await apiClient.fetchUsage(
                            apiKey: request.secret,
                            now: request.requestedAt
                        )
                        return RefreshOutcome(
                            keyID: request.keyID,
                            credentialFingerprint: request.credentialFingerprint,
                            requestedAt: request.requestedAt,
                            result: .success(snapshot)
                        )
                    } catch let error as UsageAPIError {
                        return RefreshOutcome(
                            keyID: request.keyID,
                            credentialFingerprint: request.credentialFingerprint,
                            requestedAt: request.requestedAt,
                            result: .failure(error)
                        )
                    } catch {
                        return RefreshOutcome(
                            keyID: request.keyID,
                            credentialFingerprint: request.credentialFingerprint,
                            requestedAt: request.requestedAt,
                            result: .failure(.invalidResponse)
                        )
                    }
                }
            }

            for await outcome in group {
                await merge(outcome)
            }
        }
    }

    private func merge(_ outcome: RefreshOutcome) async {
        defer {
            refreshingKeyIDs.remove(outcome.keyID)
            if var state = states[outcome.keyID] {
                state.isRefreshing = false
                states[outcome.keyID] = state
            }
            isRefreshing = !refreshingKeyIDs.isEmpty
        }

        guard var state = states[outcome.keyID] else {
            return
        }
        guard
            let currentFingerprint = credentialFingerprint(for: outcome.keyID),
            currentFingerprint == outcome.credentialFingerprint
        else {
            return
        }
        switch outcome.result {
        case let .success(snapshot):
            state.snapshot = snapshot
            state.lastSuccessAt = snapshot?.fetchedAt ?? outcome.requestedAt
            state.isStale = false
            state.error = snapshot == nil ? .noSubscription : nil
            states[outcome.keyID] = state
            if let snapshot {
                try? cache.save(snapshot, for: outcome.keyID)
                await evaluateNotifications(for: state.configuration, snapshot: snapshot)
            } else {
                try? cache.delete(for: outcome.keyID)
            }
        case let .failure(error):
            applyFailure(Self.displayError(from: error), to: outcome.keyID)
        }
    }

    private func applyFailure(_ error: UsageDisplayError, to keyID: UUID) {
        guard var state = states[keyID] else {
            return
        }
        state.error = error
        state.isStale = state.snapshot != nil
        states[keyID] = state
    }

    private func credentialFingerprint(for keyID: UUID) -> CredentialFingerprint? {
        do {
            guard let secret = try keychain.read(for: keyID) else {
                return nil
            }
            return CredentialFingerprint(secret: secret)
        } catch {
            return nil
        }
    }

    private func evaluateNotifications(
        for configuration: KeyConfiguration,
        snapshot: UsageSnapshot
    ) async {
        let manager = AlertManager(evaluator: alertEvaluator, sender: notificationSender)
        _ = try? await manager.evaluateAndNotify(
            key: configuration,
            snapshot: snapshot,
            thresholds: thresholds,
            notificationsEnabled: notificationsEnabled
        )
    }

    private func persistSelection() {
        if let selectedKeyID {
            defaults.set(selectedKeyID.uuidString, forKey: Self.selectedKeyStorageKey)
        } else {
            defaults.removeObject(forKey: Self.selectedKeyStorageKey)
        }
    }

    private static func displayError(from error: UsageAPIError) -> UsageDisplayError {
        switch error {
        case .invalidKey:
            return .invalidKey
        case .transport:
            return .network
        case .invalidResponse:
            return .invalidResponse
        case .server(statusCode: 401):
            return .invalidKey
        case let .server(statusCode):
            return .server(statusCode: statusCode)
        }
    }

    private static func storeError(from error: Error) -> UsageStoreError {
        switch error {
        case KeyRepositoryError.invalidName:
            return .invalidName
        case KeyRepositoryError.invalidSecret:
            return .invalidSecret
        case let apiError as UsageAPIError:
            switch apiError {
            case .invalidKey:
                return .invalidKey
            case .transport:
                return .network
            case .invalidResponse:
                return .invalidResponse
            case .server(statusCode: 401):
                return .invalidKey
            case let .server(statusCode):
                return .server(statusCode: statusCode)
            }
        default:
            return .persistence
        }
    }
}

private struct RefreshRequest: Sendable {
    let keyID: UUID
    let secret: String
    let credentialFingerprint: CredentialFingerprint
    let requestedAt: Date
}

private struct RefreshOutcome: Sendable {
    let keyID: UUID
    let credentialFingerprint: CredentialFingerprint
    let requestedAt: Date
    let result: Result<UsageSnapshot?, UsageAPIError>
}

private struct CredentialFingerprint: Equatable, Sendable {
    let bytes: [UInt8]

    init(secret: String) {
        bytes = Array(SHA256.hash(data: Data(secret.utf8)))
    }
}
