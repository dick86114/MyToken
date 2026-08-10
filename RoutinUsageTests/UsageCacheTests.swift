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

    func test刷新间隔一分钟时四分五十九秒仍新鲜五分钟过期() {
        let lastSuccess = Date(timeIntervalSince1970: 1_000)

        XCTAssertFalse(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(299),
            refreshMinutes: 1
        ))
        XCTAssertTrue(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(300),
            refreshMinutes: 1
        ))
    }

    func test刷新间隔十五分钟时二十九分五十九秒仍新鲜三十分钟过期() {
        let lastSuccess = Date(timeIntervalSince1970: 1_000)

        XCTAssertFalse(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(1_799),
            refreshMinutes: 15
        ))
        XCTAssertTrue(UsageFreshness.isStale(
            lastSuccess: lastSuccess,
            now: lastSuccess.addingTimeInterval(1_800),
            refreshMinutes: 15
        ))
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
