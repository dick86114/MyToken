import AppKit
import Foundation

struct IssueReportContext: Sendable {
    let version: String
    let operatingSystem: String
    let architecture: String
    let updateStatus: String
    let logs: String
}

enum IssueReporter {
    static let issuesURL = URL(string: "https://github.com/dick86114/MyToken/issues/new")!
    static let maxLogCharacters = 6000
    static let maxBodyCharacters = 9000

    static func makeIssueURL(context: IssueReportContext) -> URL? {
        let header = """
        ## 环境信息
        - 应用版本：\(context.version)
        - 系统版本：\(context.operatingSystem)
        - 架构：\(context.architecture)
        - 更新状态：\(context.updateStatus)

        ## 脱敏日志
        ```text
        """
        let footer = """
        ```

        > 日志由 MyToken 自动脱敏，提交前请再次确认内容。
        """
        let availableLogCharacters = max(0, maxBodyCharacters - header.count - footer.count)
        let logs = String(context.logs.suffix(min(maxLogCharacters, availableLogCharacters)))
        let body = String((header + logs + footer).prefix(maxBodyCharacters))

        var components = URLComponents(
            url: issuesURL,
            resolvingAgainstBaseURL: false
        )
        components?.queryItems = [
            URLQueryItem(name: "title", value: "MyToken 问题反馈"),
            URLQueryItem(name: "body", value: body)
        ]
        return components?.url
    }

    @MainActor
    static func openIssueURL(_ url: URL) -> Bool {
        NSWorkspace.shared.open(url)
    }

    static func architecture() -> String {
        #if arch(arm64)
        return "arm64"
        #elseif arch(x86_64)
        return "x86_64"
        #else
        return "unknown"
        #endif
    }

    static func statusDescription(_ status: AppUpdateStatus) -> String {
        switch status {
        case .idle:
            return "空闲"
        case .checking:
            return "检查中"
        case let .available(update):
            return "发现 \(update.version)"
        case let .downloading(progress):
            return progress.map { "下载中 \(Int($0 * 100))%" } ?? "下载中"
        case let .completed(version):
            return "已完成 \(version)"
        case let .failed(message):
            return message
        }
    }
}
