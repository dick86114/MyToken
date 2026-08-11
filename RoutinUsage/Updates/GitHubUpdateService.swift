import AppKit
import Foundation

struct AppUpdate: Equatable, Sendable {
    let version: String
    let releaseURL: URL
    let downloadURL: URL
    let notes: String
}

enum UpdateCompletionNotice {
    private static let versionKey = "updateCompletionVersion"

    static func record(version: String, defaults: UserDefaults = .standard) {
        defaults.set(version, forKey: versionKey)
    }

    static func consume(defaults: UserDefaults = .standard) -> String? {
        let version = defaults.string(forKey: versionKey)
        defaults.removeObject(forKey: versionKey)
        return version
    }
}

enum UpdateServiceError: Error, Equatable, Sendable {
    case unavailable
    case invalidResponse
    case downloadFailed
    case installFailed(String)
}

protocol UpdateChecking: Sendable {
    func checkForUpdate() async throws -> AppUpdate?
    func download(
        _ update: AppUpdate,
        progress: @escaping @Sendable (Double?) async -> Void
    ) async throws -> URL
}

struct NoUpdateService: UpdateChecking {
    func checkForUpdate() async throws -> AppUpdate? { nil }
    func download(
        _ update: AppUpdate,
        progress: @escaping @Sendable (Double?) async -> Void
    ) async throws -> URL {
        throw UpdateServiceError.unavailable
    }
}

struct GitHubUpdateService: UpdateChecking, Sendable {
    static let repository = "dick86114/MyRoutin"
    static let releasesURL = URL(string: "https://api.github.com/repos/\(repository)/releases/latest")!

    let session: URLSession
    let currentVersion: String

    init(session: URLSession = .shared, currentVersion: String? = nil) {
        self.session = session
        self.currentVersion = currentVersion ?? (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0")
    }

    func checkForUpdate() async throws -> AppUpdate? {
        var request = URLRequest(url: Self.releasesURL)
        request.setValue("application/vnd.github+json", forHTTPHeaderField: "Accept")
        request.setValue("MyRoutin/\(currentVersion)", forHTTPHeaderField: "User-Agent")
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            throw UpdateServiceError.unavailable
        }
        guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
            throw UpdateServiceError.unavailable
        }
        let release: ReleaseDTO
        do { release = try JSONDecoder().decode(ReleaseDTO.self, from: data) } catch {
            throw UpdateServiceError.invalidResponse
        }
        let version = Self.normalize(release.tagName)
        guard Self.compare(version, currentVersion) == .orderedDescending else { return nil }
        guard let asset = release.assets.first(where: { $0.name.hasSuffix(".dmg") }),
              let downloadURL = URL(string: asset.browserDownloadURL) else {
            throw UpdateServiceError.invalidResponse
        }
        guard let releaseURL = URL(string: release.htmlURL) else { throw UpdateServiceError.invalidResponse }
        return AppUpdate(version: version, releaseURL: releaseURL, downloadURL: downloadURL, notes: release.body ?? "")
    }

    func download(
        _ update: AppUpdate,
        progress: @escaping @Sendable (Double?) async -> Void
    ) async throws -> URL {
        do {
            let (bytes, response) = try await session.bytes(from: update.downloadURL)
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else {
                throw UpdateServiceError.downloadFailed
            }

            let totalBytes = response.expectedContentLength > 0 ? response.expectedContentLength : nil
            var data = Data()
            if let totalBytes {
                data.reserveCapacity(Int(totalBytes))
            }
            var receivedBytes: Int64 = 0
            var lastReportedProgress = -1.0

            for try await byte in bytes {
                data.append(byte)
                receivedBytes += 1
                guard let totalBytes else {
                    if receivedBytes == 1 {
                        await progress(nil)
                    }
                    continue
                }
                let currentProgress = min(max(Double(receivedBytes) / Double(totalBytes), 0), 1)
                if currentProgress - lastReportedProgress >= 0.01 || currentProgress >= 1 {
                    lastReportedProgress = currentProgress
                    await progress(currentProgress)
                }
            }

            guard !data.isEmpty else {
                throw UpdateServiceError.downloadFailed
            }
            let url = FileManager.default.temporaryDirectory.appendingPathComponent("MyRoutin-\(update.version).dmg")
            try data.write(to: url, options: .atomic)
            return url
        } catch is CancellationError {
            throw CancellationError()
        } catch let error as UpdateServiceError {
            throw error
        } catch {
            throw UpdateServiceError.downloadFailed
        }
    }

    private struct ReleaseDTO: Decodable {
        let tagName: String
        let htmlURL: String
        let body: String?
        let assets: [AssetDTO]
        enum CodingKeys: String, CodingKey { case tagName = "tag_name", htmlURL = "html_url", body, assets }
    }
    private struct AssetDTO: Decodable {
        let name: String
        let browserDownloadURL: String
        enum CodingKeys: String, CodingKey { case name, browserDownloadURL = "browser_download_url" }
    }
    private static func normalize(_ value: String) -> String {
        value.trimmingCharacters(in: .whitespacesAndNewlines).replacingOccurrences(of: "^v", with: "", options: .regularExpression)
    }
    private static func compare(_ lhs: String, _ rhs: String) -> ComparisonResult {
        let a = lhs.split(separator: ".").compactMap { Int($0) }
        let b = rhs.split(separator: ".").compactMap { Int($0) }
        for index in 0..<max(a.count, b.count) {
            let av = index < a.count ? a[index] : 0; let bv = index < b.count ? b[index] : 0
            if av != bv { return av < bv ? .orderedAscending : .orderedDescending }
        }
        return .orderedSame
    }
}

@MainActor
enum UpdateInstaller {
    static func install(
        dmgURL: URL,
        appName: String = "MyRoutin",
        version: String
    ) throws {
        let mountPoint = FileManager.default.temporaryDirectory.appendingPathComponent("routin-update-\(UUID().uuidString)")
        try FileManager.default.createDirectory(at: mountPoint, withIntermediateDirectories: true)
        defer {
            _ = run("/usr/bin/hdiutil", ["detach", mountPoint.path, "-quiet"])
            try? FileManager.default.removeItem(at: mountPoint)
        }
        guard run("/usr/bin/hdiutil", ["attach", dmgURL.path, "-nobrowse", "-mountpoint", mountPoint.path]) == 0 else {
            throw UpdateServiceError.installFailed("无法挂载更新磁盘映像")
        }
        let source = mountPoint.appendingPathComponent("\(appName).app")
        guard FileManager.default.fileExists(atPath: source.path) else { throw UpdateServiceError.installFailed("更新包中未找到应用") }
        let destination = URL(fileURLWithPath: "/Applications").appendingPathComponent("\(appName).app")
        let temporaryDestination = destination.deletingLastPathComponent().appendingPathComponent(".\(appName)-new.app")
        try? FileManager.default.removeItem(at: temporaryDestination)
        guard run("/usr/bin/ditto", [source.path, temporaryDestination.path]) == 0 else { throw UpdateServiceError.installFailed("复制新版本失败") }
        try? FileManager.default.removeItem(at: destination)
        try FileManager.default.moveItem(at: temporaryDestination, to: destination)
        UpdateCompletionNotice.record(version: version)
        let configuration = NSWorkspace.OpenConfiguration()
        configuration.createsNewApplicationInstance = true
        configuration.activates = true
        NSWorkspace.shared.openApplication(at: destination, configuration: configuration)
        NSApplication.shared.terminate(nil)
    }

    private static func run(_ path: String, _ arguments: [String]) -> Int32 {
        let process = Process(); process.executableURL = URL(fileURLWithPath: path); process.arguments = arguments
        do { try process.run(); process.waitUntilExit(); return process.terminationStatus } catch { return -1 }
    }
}
