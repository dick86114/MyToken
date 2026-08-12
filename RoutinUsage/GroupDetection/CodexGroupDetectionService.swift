import Foundation
import Observation

enum CodexGroupDetectionFailure: Equatable, Sendable {
    case accountUnavailable
    case invalidKey
    case modelUnavailable
    case network
    case logTimeout
    case pageChanged
    case logNotFound
    case unknown
}

enum CodexGroupDetectionState: Equatable, Sendable {
    case idle
    case needsLogin
    case probing
    case waitingForLog
    case succeeded
    case accountMismatch
    case failed(CodexGroupDetectionFailure)

    var isBusy: Bool {
        self == .probing || self == .waitingForLog
    }
}

@MainActor
@Observable
final class CodexGroupDetectionService {
    private struct PendingDetection {
        let keyID: UUID
        let secret: String
    }

    private(set) var states: [UUID: CodexGroupDetectionState] = [:]
    private(set) var activeKeyID: UUID?

    @ObservationIgnored private let webSession: any RoutinGroupDetectionWebSessionManaging
    @ObservationIgnored private let probeClient: any CodexGroupProbing
    @ObservationIgnored private let repository: any CodexGroupDetectionStoring
    @ObservationIgnored private var pendingDetection: PendingDetection?
    @ObservationIgnored private var activeTask: Task<Void, Never>?

    init(
        webSession: any RoutinGroupDetectionWebSessionManaging,
        probeClient: any CodexGroupProbing,
        repository: any CodexGroupDetectionStoring
    ) {
        self.webSession = webSession
        self.probeClient = probeClient
        self.repository = repository
    }

    func state(for keyID: UUID) -> CodexGroupDetectionState {
        states[keyID] ?? .idle
    }

    func record(for keyID: UUID) -> CodexGroupDetectionRecord? {
        try? repository.load(for: keyID)
    }

    func start(keyID: UUID, secret: String) async {
        guard activeTask == nil else {
            return
        }
        pendingDetection = PendingDetection(keyID: keyID, secret: secret)

        guard await webSession.hasAuthenticatedSession() else {
            states[keyID] = .needsLogin
            await webSession.prepareLogin()
            return
        }
        await performPendingDetection()
    }

    func didFinishLogin() async {
        guard pendingDetection != nil, activeTask == nil else {
            return
        }
        guard await webSession.hasAuthenticatedSession() else {
            if let pendingDetection {
                states[pendingDetection.keyID] = .needsLogin
            }
            return
        }
        await performPendingDetection()
    }

    func clearRecord(for keyID: UUID) {
        if activeKeyID == keyID {
            activeTask?.cancel()
            activeTask = nil
            activeKeyID = nil
        }
        if pendingDetection?.keyID == keyID {
            pendingDetection = nil
        }
        try? repository.delete(for: keyID)
        states[keyID] = .idle
    }

    private func performPendingDetection() async {
        guard let pendingDetection, activeTask == nil else {
            return
        }

        let identity: RoutinAccountIdentity
        do {
            identity = try await webSession.readCurrentAccountIdentity()
        } catch let error as RoutinGroupDetectionWebError {
            states[pendingDetection.keyID] = .failed(failure(from: error))
            self.pendingDetection = nil
            return
        } catch {
            states[pendingDetection.keyID] = .failed(.accountUnavailable)
            self.pendingDetection = nil
            return
        }

        if let existing = try? repository.load(for: pendingDetection.keyID),
           existing.accountFingerprint != identity.fingerprint {
            states[pendingDetection.keyID] = .accountMismatch
            self.pendingDetection = nil
            return
        }

        let request = pendingDetection
        activeKeyID = request.keyID
        states[request.keyID] = .probing
        let webSession = webSession
        let probeClient = probeClient
        let repository = repository
        activeTask = Task { [weak self] in
            let marker = CodexGroupProbeRequestMarker()
            do {
                try await probeClient.probe(apiKey: request.secret, marker: marker)
                guard !Task.isCancelled else { return }
                self?.setWaitingForLog(keyID: request.keyID)
                let groupName = try await webSession.findGroupName(marker: marker)
                guard !Task.isCancelled else { return }
                try repository.save(
                    CodexGroupDetectionRecord(
                        keyID: request.keyID,
                        accountFingerprint: identity.fingerprint,
                        accountDisplayName: identity.displayName,
                        groupName: groupName,
                        detectedAt: .now
                    )
                )
                self?.finish(keyID: request.keyID, state: .succeeded)
            } catch is CancellationError {
                self?.finish(keyID: request.keyID, state: .idle)
            } catch let error as CodexGroupProbeError {
                self?.finish(keyID: request.keyID, state: .failed(self?.failure(from: error) ?? .unknown))
            } catch let error as RoutinGroupDetectionWebError {
                self?.finish(keyID: request.keyID, state: .failed(self?.failure(from: error) ?? .unknown))
            } catch {
                self?.finish(keyID: request.keyID, state: .failed(.unknown))
            }
        }
        let task = activeTask
        await task?.value
    }

    private func setWaitingForLog(keyID: UUID) {
        states[keyID] = .waitingForLog
    }

    private func finish(keyID: UUID, state: CodexGroupDetectionState) {
        guard activeKeyID == keyID else {
            return
        }
        states[keyID] = state
        activeKeyID = nil
        activeTask = nil
        pendingDetection = nil
    }

    private func failure(from error: CodexGroupProbeError) -> CodexGroupDetectionFailure {
        switch error {
        case .invalidKey:
            return .invalidKey
        case .modelUnavailable:
            return .modelUnavailable
        case .network:
            return .network
        case .invalidResponse, .server:
            return .unknown
        }
    }

    private func failure(from error: RoutinGroupDetectionWebError) -> CodexGroupDetectionFailure {
        switch error {
        case .accountUnavailable:
            return .accountUnavailable
        case .logTimeout:
            return .logTimeout
        case .pageChanged:
            return .pageChanged
        case .logNotFound:
            return .logNotFound
        case .needsLogin:
            return .accountUnavailable
        case .ambiguousLog:
            return .pageChanged
        }
    }
}
