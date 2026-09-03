import Foundation
import OSLog

enum AppLogLevel: String, Sendable {
    case info
    case warning
    case error
}

protocol AppLogWriting: Sendable {
    func log(level: AppLogLevel, event: String, details: String?) async
    func recentText(maxCharacters: Int) async -> String
}

struct NoopAppLogWriter: AppLogWriting {
    func log(level: AppLogLevel, event: String, details: String?) async {}
    func recentText(maxCharacters: Int) async -> String { "" }
}

actor AppLogStore: AppLogWriting {
    static let shared = AppLogStore()

    private let fileURL: URL
    private let maxBytes: Int
    private let systemLogger = Logger(subsystem: "ai.routin.usage-monitor", category: "diagnostics")

    init(fileURL: URL = AppLogStore.defaultFileURL, maxBytes: Int = 128 * 1024) {
        self.fileURL = fileURL
        self.maxBytes = max(1, maxBytes)
    }

    func log(level: AppLogLevel, event: String, details: String? = nil) async {
        let timestamp = ISO8601DateFormatter().string(from: Date())
        var line = "\(timestamp) [\(level.rawValue)] \(event)"
        if let details, !details.isEmpty {
            line += " - \(Self.sanitize(details))"
        }
        line += "\n"

        guard let data = line.data(using: .utf8) else { return }
        do {
            try FileManager.default.createDirectory(
                at: fileURL.deletingLastPathComponent(),
                withIntermediateDirectories: true
            )
            var existing = (try? Data(contentsOf: fileURL)) ?? Data()
            existing.append(data)
            if existing.count > maxBytes {
                existing = Data(existing.suffix(maxBytes))
            }
            try existing.write(to: fileURL, options: .atomic)
        } catch {
            systemLogger.error("写入日志失败：\(error.localizedDescription, privacy: .public)")
        }
    }

    func recentText(maxCharacters: Int = 6000) async -> String {
        guard maxCharacters > 0, let data = try? Data(contentsOf: fileURL) else {
            return ""
        }
        let text = String(decoding: data, as: UTF8.self)
        return String(text.suffix(maxCharacters))
    }

    static var defaultFileURL: URL {
        let library = FileManager.default.urls(for: .libraryDirectory, in: .userDomainMask).first
            ?? URL(fileURLWithPath: NSHomeDirectory()).appendingPathComponent("Library")
        return library
            .appendingPathComponent("Logs", isDirectory: true)
            .appendingPathComponent("MyToken", isDirectory: true)
            .appendingPathComponent("app.log")
    }

    private static func sanitize(_ value: String) -> String {
        let limited = String(value.prefix(2000))
        var result = limited.replacingOccurrences(
            of: #"(?i)plan-[A-Za-z0-9._-]+"#,
            with: "plan-<redacted>",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)(api[_-]?key|token|secret|password)=([^&\s]+)"#,
            with: "$1=<redacted>",
            options: .regularExpression
        )
        return result
    }
}
