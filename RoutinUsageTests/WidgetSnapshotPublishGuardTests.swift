import Foundation
import XCTest
@testable import RoutinUsage

final class WidgetSnapshotPublishGuardTests: XCTestCase {
    func testWidgetAppGroup常量与读取端一致() throws {
        XCTAssertEqual(WidgetSharedAppGroup.identifier, "group.cc.idickies.mytoken.widgets")

        let appEnvironment = try readSourceText(at: "RoutinUsage/App/AppEnvironment.swift")
        let credentialEntity = try readSourceText(at: "RoutinUsage/WidgetShared/Intents/CredentialEntity.swift")
        let project = try readSourceText(at: "project.yml")
        let appEntitlements = try readSourceText(at: "RoutinUsage/RoutinUsage.entitlements")
        let widgetEntitlements = try readSourceText(at: "RoutinUsageWidget/RoutinUsageWidget.entitlements")

        XCTAssertTrue(appEnvironment.contains("WidgetSharedAppGroup.identifier"))
        XCTAssertTrue(credentialEntity.contains("WidgetSharedAppGroup.identifier"))
        XCTAssertTrue(project.contains("RoutinUsage/WidgetShared/WidgetSharedAppGroup.swift"))
        XCTAssertTrue(appEntitlements.contains(WidgetSharedAppGroup.identifier))
        XCTAssertTrue(widgetEntitlements.contains(WidgetSharedAppGroup.identifier))
    }

    func test可用性探测会拒绝空目录和不可写目录() throws {
        XCTAssertNil(WidgetSnapshotAvailability.validateDirectory(nil))
        XCTAssertNil(WidgetSnapshotAvailability.validateDirectory(URL(fileURLWithPath: "/proc/mytoken-invalid")))

        let directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("widget-availability-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
        let validated = WidgetSnapshotAvailability.validateDirectory(directory)
        XCTAssertEqual(validated, directory)
        XCTAssertEqual(try FileManager.default.contentsOfDirectory(atPath: directory.path).count, 0)
    }

    @MainActor
    func testWidget写入失败后禁用同步且只记录一次() async throws {
        let blockingFile = FileManager.default.temporaryDirectory
            .appendingPathComponent("widget-blocker-\(UUID().uuidString)")
        try Data("not-a-directory".utf8).write(to: blockingFile)
        let reloader = WidgetReloaderSpy()
        let logger = DiagnosticsLogWriterSpy()
        let synchronizer = WidgetSnapshotSynchronizer(
            store: WidgetSnapshotStore(directory: blockingFile),
            reloader: reloader,
            logWriter: logger
        )
        let keyID = UUID()
        let state = KeyUsageState(
            configuration: KeyConfiguration(
                id: keyID,
                name: "测试凭证",
                keySuffix: "ZZZ9",
                sortOrder: 0,
                providerID: .deepseek,
                credentialKind: .apiKey
            ),
            snapshot: nil,
            lastSuccessAt: nil,
            isRefreshing: false,
            isStale: false,
            error: nil
        )

        synchronizer.publish(states: [keyID: state], orderedKeyIDs: [keyID])
        try await logger.waitForLoggedEvent(timeout: 1)
        XCTAssertEqual(logger.eventCount, 1)
        XCTAssertEqual(logger.lastEvent, "widget_snapshot_write_failed")
        XCTAssertEqual(reloader.reloadCount, 0)

        synchronizer.publish(states: [keyID: state], orderedKeyIDs: [keyID])
        try await Task.sleep(nanoseconds: 20_000_000)
        XCTAssertEqual(logger.eventCount, 1)
        XCTAssertEqual(reloader.reloadCount, 0)

        try? FileManager.default.removeItem(at: blockingFile)
    }
}

@MainActor
private final class DiagnosticsLogWriterSpy: AppLogWriting, @unchecked Sendable {
    private let lock = NSLock()
    private var loggedEventNames: [String] = []
    private var continuation: CheckedContinuation<Void, Never>?
    private var hasWaiter = false

    var eventCount: Int {
        lock.withLock { loggedEventNames.count }
    }

    var lastEvent: String? {
        lock.withLock { loggedEventNames.last }
    }

    func log(level: AppLogLevel, event: String, details: String?) async {
        lock.lock()
        loggedEventNames.append(event)
        let waiter = continuation
        continuation = nil
        lock.unlock()
        waiter?.resume()
    }

    func recentText(maxCharacters: Int) async -> String { "" }

    func waitForLoggedEvent(timeout: TimeInterval) async throws {
        let started = Date()
        while Date().timeIntervalSince(started) < timeout {
            if eventCount > 0 { return }
            try await Task.sleep(nanoseconds: 10_000_000)
        }
    }
}

private final class WidgetReloaderSpy: WidgetReloading, @unchecked Sendable {
    private let lock = NSLock()
    private var storedReloadCount = 0

    var reloadCount: Int {
        lock.withLock { storedReloadCount }
    }

    func reloadAllTimelines() {
        lock.withLock { storedReloadCount += 1 }
    }
}
