import XCTest
@testable import RoutinUsage

final class RoutinCheckInServiceTests: XCTestCase {
    func test未登录签到会进入登录状态() async {
        let session = RoutinWebSessionFake(isAuthenticated: false, outcome: .succeeded)
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        await service.startCheckIn()

        await MainActor.run {
            XCTAssertEqual(service.state, .needsLogin)
        }
        let checkInCalls = await session.checkInCallCount()
        XCTAssertEqual(checkInCalls, 0)
    }

    func test登录成功后会续接原签到请求() async {
        let session = RoutinWebSessionFake(isAuthenticated: false, outcome: .succeeded)
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        await service.startCheckIn()
        await service.beginLogin()
        await session.setAuthenticated(true)
        await service.didFinishLogin()

        await MainActor.run {
            XCTAssertEqual(service.state, .succeeded)
        }
        let checkInCalls = await session.checkInCallCount()
        XCTAssertEqual(checkInCalls, 1)
    }

    func test未发起签到时登录成功会保留已登录状态() async {
        let session = RoutinWebSessionFake(isAuthenticated: false, outcome: .succeeded)
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        await service.beginLogin()
        await session.setAuthenticated(true)
        await service.didFinishLogin()

        await MainActor.run {
            XCTAssertEqual(service.state, .loggedIn)
        }
        let checkInCalls = await session.checkInCallCount()
        XCTAssertEqual(checkInCalls, 0)
    }

    func test签到进行中重复点击只发起一次请求() async {
        let session = RoutinWebSessionFake(isAuthenticated: true, outcome: .suspended)
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        let first = Task { await service.startCheckIn() }
        await session.waitUntilCheckInCalled()
        await service.startCheckIn()
        await session.resolveSuspendedCheckIn(with: .succeeded)
        await first.value

        let checkInCalls = await session.checkInCallCount()
        XCTAssertEqual(checkInCalls, 1)
        await MainActor.run {
            XCTAssertEqual(service.state, .succeeded)
        }
    }

    func test网页显示已签到会映射为已签到状态() async {
        let session = RoutinWebSessionFake(isAuthenticated: true, outcome: .alreadyCheckedIn)
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        await service.startCheckIn()

        await MainActor.run {
            XCTAssertEqual(service.state, .alreadyCheckedIn)
        }
    }

    func test无法确认页面结果会显示页面变化错误() async {
        let session = RoutinWebSessionFake(isAuthenticated: true, outcome: .cannotConfirm)
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        await service.startCheckIn()

        await MainActor.run {
            XCTAssertEqual(service.state, .failed(.pageChanged))
        }
    }

    func test网络错误会显示网络错误状态() async {
        let session = RoutinWebSessionFake(
            isAuthenticated: true,
            outcome: .succeeded,
            checkInError: URLError(.notConnectedToInternet)
        )
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        await service.startCheckIn()

        await MainActor.run {
            XCTAssertEqual(service.state, .failed(.network))
        }
    }

    func test登出仅清除网页会话并重置状态() async {
        let session = RoutinWebSessionFake(isAuthenticated: true, outcome: .succeeded)
        let service = await MainActor.run { RoutinCheckInService(session: session) }

        await service.signOut()

        await MainActor.run {
            XCTAssertEqual(service.state, .idle)
        }
        let clearCalls = await session.clearCallCount()
        XCTAssertEqual(clearCalls, 1)
    }
}

actor RoutinWebSessionFake: RoutinWebSessionManaging {
    private var isAuthenticated: Bool
    private var outcome: RoutinCheckInOutcome
    private let checkInError: Error?
    private var checkInCalls = 0
    private var clearCalls = 0
    private var checkInContinuation: CheckedContinuation<RoutinCheckInOutcome, Error>?

    init(isAuthenticated: Bool, outcome: RoutinCheckInOutcome, checkInError: Error? = nil) {
        self.isAuthenticated = isAuthenticated
        self.outcome = outcome
        self.checkInError = checkInError
    }

    func hasAuthenticatedSession() async -> Bool {
        isAuthenticated
    }

    func prepareLogin() async {}

    func performCheckIn() async throws -> RoutinCheckInOutcome {
        checkInCalls += 1
        if let checkInError {
            throw checkInError
        }
        if outcome == .suspended {
            return try await withCheckedThrowingContinuation { continuation in
                checkInContinuation = continuation
            }
        }
        return outcome
    }

    func clearRoutinWebsiteData() async {
        clearCalls += 1
        isAuthenticated = false
    }

    func setAuthenticated(_ value: Bool) {
        isAuthenticated = value
    }

    func checkInCallCount() -> Int {
        checkInCalls
    }

    func clearCallCount() -> Int {
        clearCalls
    }

    func waitUntilCheckInCalled() async {
        while checkInCalls == 0 {
            await Task.yield()
        }
    }

    func resolveSuspendedCheckIn(with value: RoutinCheckInOutcome) {
        checkInContinuation?.resume(returning: value)
        checkInContinuation = nil
    }
}
