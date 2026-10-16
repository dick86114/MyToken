import Foundation

/// 将内存诊断事件和最近日志写入用户选择的本地文件；NSSavePanel 入口由 AppEnvironment 调用。
enum DiagnosticsLogExporter {
    static func fileName(for date: Date) -> String {
        let formatter = DateFormatter()
        formatter.dateFormat = "yyyyMMdd-HHmmss"
        formatter.timeZone = TimeZone(identifier: "Asia/Shanghai")
        formatter.locale = Locale(identifier: "en_US_POSIX")
        return "MyToken-diagnostics-\(formatter.string(from: date)).txt"
    }

    static func write(_ report: String, to url: URL) throws {
        let data = Data(report.utf8)
        try url.deletingLastPathComponent().ensureLocalDirectoryExistsIfNeeded()
        try data.write(to: url, options: .atomic)
    }
}

private extension URL {
    func ensureLocalDirectoryExistsIfNeeded() throws {
        var isDirectory: ObjCBool = false
        if FileManager.default.fileExists(atPath: path, isDirectory: &isDirectory), isDirectory.boolValue {
            return
        }
        try FileManager.default.createDirectory(at: self, withIntermediateDirectories: true)
    }
}
