import Foundation
import XCTest
@testable import RoutinUsage

final class CodexGroupDetectionServiceTests: XCTestCase {
    func test成功检测会保存当前账号和分组记录() async throws {
        let keyID = UUID()
        let web = GroupDetectionWebFake(
            authenticated: true,
            identity: RoutinAccountIdentity.make(email: "member@example.com", displayName: "会员")
        )
        let probe = GroupDetectionProbeFake()
        let repository = GroupDetectionRecordFake()
        await web.setGroupName("Codex")
        let service = await MainActor.run {
            CodexGroupDetectionService(webSession: web, probeClient: probe, repository: repository)
        }

        await service.start(keyID: keyID, secret: "plan-test-1234")

        let record = try await repository.load(for: keyID)
        XCTAssertEqual(record?.groupName, "Codex")
        XCTAssertEqual(record?.accountDisplayName, "会员")
        let count = await probe.callCount()
        XCTAssertEqual(count, 1)
        await MainActor.run {
            XCTAssertEqual(service.state(for: keyID), .succeeded)
        }
    }

    func test已绑定不同账号会在发请求前阻止() async throws {
        let keyID = UUID()
        let repository = GroupDetectionRecordFake()
        try await repository.save(
            CodexGroupDetectionRecord(
                keyID: keyID,
                accountFingerprint: RoutinAccountIdentity.make(
                    email: "old@example.com",
                    displayName: "旧账号"
                ).fingerprint,
                accountDisplayName: "旧账号",
                groupName: "Codex",
                detectedAt: .distantPast
            )
        )
        let web = GroupDetectionWebFake(
            authenticated: true,
            identity: RoutinAccountIdentity.make(email: "new@example.com", displayName: "新账号")
        )
        let probe = GroupDetectionProbeFake()
        let service = await MainActor.run {
            CodexGroupDetectionService(webSession: web, probeClient: probe, repository: repository)
        }

        await service.start(keyID: keyID, secret: "plan-test-1234")

        let count = await probe.callCount()
        XCTAssertEqual(count, 0)
        await MainActor.run {
            XCTAssertEqual(service.state(for: keyID), .accountMismatch)
        }
    }

    func test首次检测未找到日志不会误报账号不匹配() async {
        let keyID = UUID()
        let web = GroupDetectionWebFake(
            authenticated: true,
            identity: RoutinAccountIdentity.make(email: "member@example.com", displayName: "会员")
        )
        await web.setFindError(.logTimeout)
        let probe = GroupDetectionProbeFake()
        let repository = GroupDetectionRecordFake()
        let service = await MainActor.run {
            CodexGroupDetectionService(webSession: web, probeClient: probe, repository: repository)
        }

        await service.start(keyID: keyID, secret: "plan-test-1234")

        await MainActor.run {
            XCTAssertEqual(service.state(for: keyID), .failed(.logTimeout))
        }
    }

    func test未登录后登录完成会续接原检测() async throws {
        let keyID = UUID()
        let web = GroupDetectionWebFake(
            authenticated: false,
            identity: RoutinAccountIdentity.make(email: "member@example.com", displayName: "会员")
        )
        await web.setGroupName("Codex")
        let probe = GroupDetectionProbeFake()
        let repository = GroupDetectionRecordFake()
        let service = await MainActor.run {
            CodexGroupDetectionService(webSession: web, probeClient: probe, repository: repository)
        }

        await service.start(keyID: keyID, secret: "plan-test-1234")
        await MainActor.run {
            XCTAssertEqual(service.state(for: keyID), .needsLogin)
        }
        await web.setAuthenticated(true)
        await service.didFinishLogin()

        let record = try await repository.load(for: keyID)
        XCTAssertEqual(record?.groupName, "Codex")
    }
}

actor GroupDetectionWebFake: RoutinGroupDetectionWebSessionManaging {
    private var authenticated: Bool
    private let identity: RoutinAccountIdentity
    private var groupName: String?
    private var findError: RoutinGroupDetectionWebError?

    init(authenticated: Bool, identity: RoutinAccountIdentity) {
        self.authenticated = authenticated
        self.identity = identity
    }

    func hasAuthenticatedSession() async -> Bool { authenticated }
    func prepareLogin() async {}
    func readCurrentAccountIdentity() async throws -> RoutinAccountIdentity { identity }

    func findGroupName(marker _: CodexGroupProbeRequestMarker) async throws -> String {
        if let findError {
            throw findError
        }
        return groupName ?? "Codex"
    }

    func setAuthenticated(_ value: Bool) { authenticated = value }
    func setGroupName(_ value: String) { groupName = value }
    func setFindError(_ value: RoutinGroupDetectionWebError?) { findError = value }
}

actor GroupDetectionProbeFake: CodexGroupProbing {
    private var calls = 0

    func probe(apiKey _: String, marker _: CodexGroupProbeRequestMarker) async throws {
        calls += 1
    }

    func callCount() -> Int { calls }
}

final class GroupDetectionRecordFake: CodexGroupDetectionStoring, @unchecked Sendable {
    private let lock = NSLock()
    private var records: [UUID: CodexGroupDetectionRecord] = [:]

    func load(for keyID: UUID) throws -> CodexGroupDetectionRecord? {
        lock.withLock { records[keyID] }
    }

    func save(_ record: CodexGroupDetectionRecord) throws {
        lock.withLock { records[record.keyID] = record }
    }

    func delete(for keyID: UUID) throws {
        lock.withLock { records.removeValue(forKey: keyID) }
    }
}
