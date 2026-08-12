import Foundation
import Observation

protocol RoutinWebSessionManaging: Sendable {
    func hasAuthenticatedSession() async -> Bool
    func prepareLogin() async
    func performCheckIn() async throws -> RoutinCheckInOutcome
    func clearRoutinWebsiteData() async
}

@MainActor
@Observable
final class RoutinCheckInService {
    private enum PendingAction {
        case checkIn
    }

    private let session: any RoutinWebSessionManaging
    private var pendingAction: PendingAction?

    private(set) var state: RoutinCheckInState = .idle

    init(session: any RoutinWebSessionManaging) {
        self.session = session
    }

    func startCheckIn() async {
        guard !state.isBusy else {
            return
        }
        pendingAction = .checkIn
        guard await session.hasAuthenticatedSession() else {
            state = .needsLogin
            return
        }
        guard pendingAction != nil else {
            state = .loggedIn
            return
        }
        await performPendingAction()
    }

    func beginLogin() async {
        guard !state.isBusy else {
            return
        }
        state = .loggingIn
        await session.prepareLogin()
    }

    func didFinishLogin() async {
        guard state == .loggingIn else {
            return
        }
        guard await session.hasAuthenticatedSession() else {
            state = .needsLogin
            return
        }
        guard pendingAction != nil else {
            state = .loggedIn
            return
        }
        await performPendingAction()
    }

    func cancelLogin() {
        guard state == .loggingIn else {
            return
        }
        state = .failed(.cancelled)
        pendingAction = nil
    }

    func signOut() async {
        guard !state.isBusy else {
            return
        }
        pendingAction = nil
        await session.clearRoutinWebsiteData()
        state = .idle
    }

    private func performPendingAction() async {
        guard pendingAction != nil else {
            state = .loggedIn
            return
        }
        state = .checkingIn
        do {
            switch try await session.performCheckIn() {
            case .succeeded:
                state = .succeeded
            case .alreadyCheckedIn:
                state = .alreadyCheckedIn
            case .needsLogin:
                state = .needsLogin
            case .cannotConfirm, .suspended:
                state = .failed(.pageChanged)
            }
        } catch is CancellationError {
            state = .failed(.cancelled)
        } catch {
            state = .failed(.network)
        }
        pendingAction = nil
    }
}
