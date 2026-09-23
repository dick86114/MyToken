import Foundation

/// 当前版本的完整发布日志缓存。缓存与应用版本绑定，版本未变化时直接复用。
struct CachedReleaseHistory: Codable, Equatable, Sendable {
    let appVersion: String
    let releases: [AppReleaseHistoryItem]
}

protocol ReleaseHistoryCacheStoring {
    func load(appVersion: String) -> [AppReleaseHistoryItem]?
    func save(appVersion: String, releases: [AppReleaseHistoryItem])
}

struct UserDefaultsReleaseHistoryCache: ReleaseHistoryCacheStoring {
    static let storageKey = "releaseHistoryCache.v1"

    let defaults: UserDefaults

    init(defaults: UserDefaults = .standard) {
        self.defaults = defaults
    }

    func load(appVersion: String) -> [AppReleaseHistoryItem]? {
        guard let data = defaults.data(forKey: Self.storageKey),
              let cache = try? JSONDecoder().decode(CachedReleaseHistory.self, from: data),
              cache.appVersion == appVersion else {
            return nil
        }
        return cache.releases
    }

    func save(appVersion: String, releases: [AppReleaseHistoryItem]) {
        let cache = CachedReleaseHistory(appVersion: appVersion, releases: releases)
        guard let data = try? JSONEncoder().encode(cache) else { return }
        defaults.set(data, forKey: Self.storageKey)
    }
}
