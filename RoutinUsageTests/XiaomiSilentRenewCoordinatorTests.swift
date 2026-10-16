import CryptoKit
import Foundation
import XCTest
@testable import RoutinUsage

@MainActor
final class XiaomiSilentRenewCoordinatorTests: XCTestCase {
    func test同一凭证并发续期只执行一次并共享结果() async {
        let coordinator = XiaomiSilentRenewCoordinator(now: { Date(timeIntervalSince1970: 100) })
        let gate = OperationGate()
        let operationCalls = XCTestLockedCounter()
        let credentialID = UUID()

        async let first: String? = coordinator.renew(
            credentialID: credentialID,
            previousSecret: "old-cookie"
        ) {
            await gate.waitUntilSecondCaller()
            operationCalls.increment()
            return "new-cookie"
        }
        async let second: String? = coordinator.renew(
            credentialID: credentialID,
            previousSecret: "old-cookie"
        ) {
            operationCalls.increment()
            return "new-cookie"
        }

        await gate.release()
        let results = await [first, second]
        XCTAssertEqual(results, ["new-cookie", "new-cookie"])
        XCTAssertEqual(operationCalls.value, 1)
    }

    func test续期失败后进入一小时退避且换新凭证立即重试() async {
        var currentTime = Date(timeIntervalSince1970: 100)
        let coordinator = XiaomiSilentRenewCoordinator(now: { currentTime })
        let credentialID = UUID()
        let operationCalls = XCTestLockedCounter()

        let firstFailure = await coordinator.renew(
            credentialID: credentialID,
            previousSecret: "old-cookie"
        ) {
            operationCalls.increment()
            return nil
        }
        XCTAssertEqual(firstFailure, nil)
        XCTAssertEqual(operationCalls.value, 1)

        let backedOff = await coordinator.renew(
            credentialID: credentialID,
            previousSecret: "old-cookie"
        ) {
            operationCalls.increment()
            XCTFail("退避期内不应调用")
            return nil
        }
        XCTAssertEqual(backedOff, nil)
        XCTAssertEqual(operationCalls.value, 1)

        currentTime = Date(timeIntervalSince1970: 100 + 3601)
        let retried = await coordinator.renew(
            credentialID: credentialID,
            previousSecret: "old-cookie"
        ) {
            operationCalls.increment()
            return nil
        }
        XCTAssertEqual(retried, nil)
        XCTAssertEqual(operationCalls.value, 2)

        let newCredentialRetry = await coordinator.renew(
            credentialID: UUID(),
            previousSecret: "old-cookie"
        ) {
            operationCalls.increment()
            return "new-cookie"
        }
        XCTAssertEqual(newCredentialRetry, "new-cookie")
        XCTAssertEqual(operationCalls.value, 3)
    }

    func test连续失败退避按小时翻倍且最长六小时() {
        XCTAssertEqual(XiaomiSilentRenewCoordinator.backoffInterval(forFailureCount: 1), 3600)
        XCTAssertEqual(XiaomiSilentRenewCoordinator.backoffInterval(forFailureCount: 2), 7200)
        XCTAssertEqual(XiaomiSilentRenewCoordinator.backoffInterval(forFailureCount: 3), 14400)
        XCTAssertEqual(XiaomiSilentRenewCoordinator.backoffInterval(forFailureCount: 4), 21600)
        XCTAssertEqual(XiaomiSilentRenewCoordinator.backoffInterval(forFailureCount: 5), 21600)
    }

    func test连续两次失败后按二小时退避并在到期后成功清除() async {
        var currentTime = Date(timeIntervalSince1970: 100)
        let coordinator = XiaomiSilentRenewCoordinator(now: { currentTime })
        let credentialID = UUID()
        let calls = XCTestLockedCounter()

        await coordinator.renew(credentialID: credentialID, previousSecret: "old") {
            calls.increment()
            return nil
        }
        currentTime = Date(timeIntervalSince1970: 100 + 3601)
        await coordinator.renew(credentialID: credentialID, previousSecret: "old") {
            calls.increment()
            return nil
        }

        // 第二次失败发生在 4702，2 小时退避到 11902。
        let stillBackedOff = await coordinator.renew(credentialID: credentialID, previousSecret: "old") {
            calls.increment()
            XCTFail("仍在退避期内")
            return nil
        }
        XCTAssertEqual(stillBackedOff, nil)
        XCTAssertEqual(calls.value, 2)

        currentTime = Date(timeIntervalSince1970: 100 + 11802)
        let success = await coordinator.renew(credentialID: credentialID, previousSecret: "old") {
            calls.increment()
            return "fresh"
        }
        XCTAssertEqual(success, "fresh")
        XCTAssertEqual(calls.value, 3)

        let immediateSuccess = await coordinator.renew(credentialID: credentialID, previousSecret: "old") {
            calls.increment()
            return "fresh-2"
        }
        XCTAssertEqual(immediateSuccess, "fresh-2")
        XCTAssertEqual(calls.value, 4)
    }
}

final class OperationGate: @unchecked Sendable {
    private let semaphore = DispatchSemaphore(value: 0)

    func waitUntilSecondCaller() {
        semaphore.wait()
    }

    func release() {
        semaphore.signal()
    }
}

private final class XCTestLockedCounter: @unchecked Sendable {
    private let lock = NSLock()
    private var storedValue = 0

    var value: Int {
        lock.withLock { storedValue }
    }

    @discardableResult
    func increment() -> Int {
        lock.withLock {
            storedValue += 1
            return storedValue
        }
    }
}
