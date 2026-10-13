import XCTest
@testable import RoutinUsage

@MainActor
final class WidgetSnapshotStoreTests: XCTestCase {
    private var directory: URL!

    override func setUpWithError() throws {
        directory = FileManager.default.temporaryDirectory
            .appendingPathComponent("widget-snapshot-tests-\(UUID().uuidString)", isDirectory: true)
        try FileManager.default.createDirectory(at: directory, withIntermediateDirectories: true)
    }

    override func tearDownWithError() throws {
        try? FileManager.default.removeItem(at: directory)
        directory = nil
    }

    func test_snapshot_store_round_trips_and_replaces_atomically() throws {
        let store = WidgetSnapshotStore(directory: directory)
        let first = makeBundle(planName: "第一版")
        let second = makeBundle(planName: "第二版")

        try store.save(first)
        XCTAssertEqual(try store.load(), first)

        try store.save(second)
        XCTAssertEqual(try store.load(), second)
        XCTAssertTrue(
            FileManager.default.fileExists(
                atPath: directory.appendingPathComponent("widget-snapshot-v1.json.previous").path
            )
        )
    }

    func test_snapshot_store_ignores_corrupt_file_and_keeps_previous_snapshot_available() throws {
        let store = WidgetSnapshotStore(directory: directory)
        let first = makeBundle(planName: "第一版")
        let second = makeBundle(planName: "第二版")

        try store.save(first)
        try store.save(second)
        try Data("not-json".utf8).write(
            to: directory.appendingPathComponent("widget-snapshot-v1.json")
        )

        XCTAssertEqual(try store.load(), first)
    }

    func test_snapshot_publish_reloads_widgets_and_excludes_credential_material() throws {
        let keyID = UUID()
        let configuration = KeyConfiguration(
            id: keyID,
            name: "测试凭证",
            keySuffix: "ZZZ9",
            sortOrder: 0,
            providerID: .deepseek,
            credentialKind: .apiKey
        )
        let state = KeyUsageState(
            configuration: configuration,
            snapshot: UsageSnapshot(
                planName: "API 余额",
                kind: .tokenPack,
                fiveHour: nil,
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: Date(timeIntervalSince1970: 100),
                providerID: .deepseek,
                credentialID: keyID,
                metrics: [
                    NormalizedUsageMetric(
                        id: "balance",
                        label: "账户余额",
                        value: 10,
                        unit: .currency,
                        presentation: .balance,
                        semantic: .balance,
                        currencyCode: "CNY"
                    )
                ]
            ),
            lastSuccessAt: Date(timeIntervalSince1970: 100),
            isRefreshing: false,
            isStale: false,
            error: nil
        )
        let reloader = WidgetReloaderSpy()
        let synchronizer = WidgetSnapshotSynchronizer(
            store: WidgetSnapshotStore(directory: directory),
            reloader: reloader
        )

        synchronizer.publish(states: [keyID: state], orderedKeyIDs: [keyID])

        XCTAssertEqual(reloader.reloadCount, 1)
        let data = try Data(
            contentsOf: directory.appendingPathComponent("widget-snapshot-v1.json")
        )
        let json = String(decoding: data, as: UTF8.self)
        XCTAssertFalse(json.contains("sk-test-secret-value"))
        XCTAssertFalse(json.contains("planKey."))
        XCTAssertFalse(json.contains("keySuffix"))
        XCTAssertFalse(json.contains("ZZZ9"))
    }

    private func makeBundle(planName: String) -> WidgetSnapshotBundle {
        WidgetSnapshotBundle(
            schemaVersion: WidgetSnapshotBundle.currentSchemaVersion,
            generatedAt: Date(timeIntervalSince1970: 100),
            credentials: [
                WidgetSnapshotCredential(
                    id: UUID(uuidString: "AAAAAAAA-AAAA-AAAA-AAAA-AAAAAAAAAAAA")!,
                    providerID: "deepseek",
                    providerName: "DeepSeek",
                    name: "按量余额",
                    planName: planName,
                    isEnabled: true,
                    sortOrder: 0,
                    fetchedAt: Date(timeIntervalSince1970: 100),
                    lastSuccessAt: Date(timeIntervalSince1970: 100),
                    isStale: false,
                    subscriptionEndAt: nil,
                    metrics: []
                )
            ]
        )
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
