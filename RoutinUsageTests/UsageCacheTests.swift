import Foundation
import XCTest
@testable import RoutinUsage

final class UsageCacheTests: XCTestCase {
    func test缓存按配置标识隔离且不持久化密钥材料() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let firstID = UUID()
        let secondID = UUID()
        let first = makeSnapshot(planName: "个人版", fetchedAt: Date(timeIntervalSince1970: 100))
        let second = makeSnapshot(planName: "团队版", fetchedAt: Date(timeIntervalSince1970: 200))

        try context.cache.save(first, for: firstID)
        try context.cache.save(second, for: secondID)

        XCTAssertEqual(try context.cache.load(for: firstID), first)
        XCTAssertEqual(try context.cache.load(for: secondID), second)
        let persistedData = try XCTUnwrap(context.defaults.data(forKey: "usageSnapshots"))
        let persistedText = try XCTUnwrap(String(data: persistedData, encoding: .utf8))
        XCTAssertFalse(persistedText.contains("plan-"))
    }

    func test删除缓存仅影响指定配置() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let firstID = UUID()
        let secondID = UUID()
        let first = makeSnapshot(planName: "个人版", fetchedAt: Date(timeIntervalSince1970: 100))
        let second = makeSnapshot(planName: "团队版", fetchedAt: Date(timeIntervalSince1970: 200))
        try context.cache.save(first, for: firstID)
        try context.cache.save(second, for: secondID)

        try context.cache.delete(for: firstID)

        XCTAssertNil(try context.cache.load(for: firstID))
        XCTAssertEqual(try context.cache.load(for: secondID), second)
    }

    func test损坏缓存数据会被清理并按空缓存恢复() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let keyID = UUID()
        let snapshot = makeSnapshot(planName: "个人版", fetchedAt: Date(timeIntervalSince1970: 100))

        context.defaults.set(Data("损坏缓存".utf8), forKey: "usageSnapshots")
        XCTAssertNil(try context.cache.load(for: keyID))
        XCTAssertNil(context.defaults.data(forKey: "usageSnapshots"))

        context.defaults.set(Data("损坏缓存".utf8), forKey: "usageSnapshots")
        try context.cache.save(snapshot, for: keyID)
        XCTAssertEqual(try context.cache.load(for: keyID), snapshot)

        context.defaults.set(Data("损坏缓存".utf8), forKey: "usageSnapshots")
        try context.cache.delete(for: keyID)
        XCTAssertNil(context.defaults.data(forKey: "usageSnapshots"))
    }

    func test刷新间隔一分钟时五十九秒仍新鲜一分钟过期() {
        let lastSuccess = Date(timeIntervalSince1970: 1_000)

        XCTAssertFalse(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(59),
            refreshMinutes: 1
        ))
        XCTAssertTrue(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(60),
            refreshMinutes: 1
        ))
    }

    func test刷新间隔十五分钟时十四分五十九秒仍新鲜十五分钟过期() {
        let lastSuccess = Date(timeIntervalSince1970: 1_000)

        XCTAssertFalse(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(899),
            refreshMinutes: 15
        ))
        XCTAssertTrue(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(900),
            refreshMinutes: 15
        ))
    }

    func test旧缓存缺少新增用量字段时仍可读取() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let keyID = UUID()
        let legacySnapshots = [
            keyID: LegacyUsageSnapshot(
                planName: "旧缓存订阅",
                kind: .periodic,
                allowedModels: [],
                fetchedAt: Date(timeIntervalSince1970: 100)
            )
        ]
        context.defaults.set(
            try JSONEncoder().encode(legacySnapshots),
            forKey: "usageSnapshots"
        )

        let snapshot = try XCTUnwrap(context.cache.load(for: keyID))

        XCTAssertEqual(snapshot.groupMultipliers, [])
        XCTAssertNil(snapshot.subscriptionStartAt)
        XCTAssertNil(snapshot.subscriptionEndAt)
    }

    func test保存新缓存时写入版本化容器() throws {
        let context = makeContext()
        defer { context.cleanUp() }
        let keyID = UUID()

        try context.cache.save(makeSnapshot(planName: "新缓存", fetchedAt: Date()), for: keyID)

        let persistedData = try XCTUnwrap(context.defaults.data(forKey: "usageSnapshots"))
        let object = try XCTUnwrap(
            JSONSerialization.jsonObject(with: persistedData) as? [String: Any]
        )
        XCTAssertEqual(object["schemaVersion"] as? Int, UsageCache.currentSchemaVersion)
        XCTAssertNotNil(object["snapshots"])
    }

    private func makeContext() -> UsageCacheTestContext {
        let suiteName = "ai.routin.usage-monitor.cache-tests.\(UUID().uuidString)"
        let defaults = UserDefaults(suiteName: suiteName)!
        defaults.removePersistentDomain(forName: suiteName)
        return UsageCacheTestContext(
            suiteName: suiteName,
            defaults: defaults,
            cache: UsageCache(defaults: defaults)
        )
    }

    private func makeSnapshot(planName: String, fetchedAt: Date) -> UsageSnapshot {
        UsageSnapshot(
            planName: planName,
            kind: .periodic,
            fiveHour: UsageMetric(
                used: 25,
                limit: 100,
                remaining: 75,
                percent: 25,
                unit: .usd,
                windowEnd: nil
            ),
            weekly: nil,
            token: nil,
            allowedModels: ["gpt-5"],
            fetchedAt: fetchedAt
        )
    }
}

private struct UsageCacheTestContext {
    let suiteName: String
    let defaults: UserDefaults
    let cache: UsageCache

    func cleanUp() {
        defaults.removePersistentDomain(forName: suiteName)
    }
}

private struct LegacyUsageSnapshot: Codable {
    let planName: String
    let kind: UsageKind
    let allowedModels: [String]
    let fetchedAt: Date
}
