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
    static let releasesAtomURL = URL(string: "https://github.com/\(repository)/releases.atom")!

    let session: URLSession
    let currentVersion: String

    init(session: URLSession = .shared, currentVersion: String? = nil) {
        self.session = session
        self.currentVersion = currentVersion ?? (Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String ?? "0.0.0")
    }

    func checkForUpdate() async throws -> AppUpdate? {
        var request = URLRequest(
            url: Self.releasesURL,
            cachePolicy: .reloadIgnoringLocalCacheData,
            timeoutInterval: 30
        )
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
        guard let http = response as? HTTPURLResponse else {
            throw UpdateServiceError.unavailable
        }
        if http.statusCode == 403 || http.statusCode == 429 {
            // GitHub API 的未登录请求按出口 IP 共享限额，桌面用户很容易撞到 403。
            // Atom feed 不走同一套 API 限额，且足够提供版本和发布页信息。
            return try await checkForUpdateFromAtom()
        }
        guard (200..<300).contains(http.statusCode) else {
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

    private func checkForUpdateFromAtom() async throws -> AppUpdate? {
        var request = URLRequest(
            url: Self.releasesAtomURL,
            cachePolicy: .reloadIgnoringLocalCacheData,
            timeoutInterval: 30
        )
        request.setValue("application/atom+xml", forHTTPHeaderField: "Accept")
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

        let parser = AtomReleaseParser()
        guard let release = parser.parse(data: data) else {
            throw UpdateServiceError.invalidResponse
        }
        let version = Self.normalize(release.version)
        guard Self.compare(version, currentVersion) == .orderedDescending else { return nil }

        let tag = release.version.hasPrefix("v") ? release.version : "v\(release.version)"
        guard let downloadURL = URL(string: "https://github.com/\(Self.repository)/releases/download/\(tag)/MyRoutin.dmg"),
              let releaseURL = URL(string: release.releaseURL) else {
            throw UpdateServiceError.invalidResponse
        }
        return AppUpdate(
            version: version,
            releaseURL: releaseURL,
            downloadURL: downloadURL,
            notes: release.notes
        )
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

private struct AtomRelease {
    let version: String
    let releaseURL: String
    let notes: String
}

private final class AtomReleaseParser: NSObject, XMLParserDelegate {
    private var currentText = ""
    private var inEntry = false
    private var version: String?
    private var releaseURL: String?
    private var notes = ""

    func parse(data: Data) -> AtomRelease? {
        let parser = XMLParser(data: data)
        parser.delegate = self
        guard parser.parse(), let version, let releaseURL else { return nil }
        return AtomRelease(version: version, releaseURL: releaseURL, notes: notes)
    }

    func parser(
        _ parser: XMLParser,
        didStartElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?,
        attributes attributeDict: [String: String] = [:]
    ) {
        guard !inEntry else {
            currentText = ""
            if elementName == "link", attributeDict["rel"] == "alternate" {
                releaseURL = attributeDict["href"]
            }
            return
        }
        if elementName == "entry" {
            inEntry = true
            currentText = ""
        }
    }

    func parser(_ parser: XMLParser, foundCharacters string: String) {
        guard inEntry else { return }
        currentText.append(string)
    }

    func parser(
        _ parser: XMLParser,
        didEndElement elementName: String,
        namespaceURI: String?,
        qualifiedName qName: String?
    ) {
        guard inEntry else { return }
        let text = currentText.trimmingCharacters(in: .whitespacesAndNewlines)
        switch elementName {
        case "id" where version == nil:
            version = text.split(separator: "/").last.map(String.init)
        case "title":
            if version == nil {
                version = text.split(whereSeparator: { $0 == "v" || $0 == " " }).last.map(String.init)
            }
        case "content":
            notes = text
        case "entry":
            inEntry = false
        default:
            break
        }
        currentText = ""
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
        NSWorkspace.shared.openApplication(at: destination, configuration: configuration) { _, error in
            Task { @MainActor in
                if error == nil {
                    NSApplication.shared.terminate(nil)
                    return
                }

                // 新版本已复制完成但系统拒绝自动启动时，保留旧进程并给出可操作提示。
                let alert = NSAlert()
                alert.messageText = "更新已安装"
                alert.informativeText = "新版本已安装到“应用程序”文件夹，请手动重新打开 MyRoutin。"
                alert.alertStyle = .warning
                alert.addButton(withTitle: "好")
                NSApp.activate(ignoringOtherApps: true)
                alert.runModal()
            }
        }
    }

    private static func run(_ path: String, _ arguments: [String]) -> Int32 {
        let process = Process(); process.executableURL = URL(fileURLWithPath: path); process.arguments = arguments
        do { try process.run(); process.waitUntilExit(); return process.terminationStatus } catch { return -1 }
    }
}
