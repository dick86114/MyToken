import Foundation

/// 导出报告前的第二层脱敏；日志写入时已有第一层，这里防止历史日志漏网。
enum DiagnosticsRedactor {
    static func redact(_ text: String) -> String {
        var result = text

        if let home = NSHomeDirectory().addingPercentEncoding(withAllowedCharacters: .urlPathAllowed),
           !home.isEmpty {
            result = result.replacingOccurrences(of: home, with: "~")
        }
        result = result.replacingOccurrences(
            of: #"(/Users/[^/\s]+)(/|\s|$)"#,
            with: "~$2",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)(cookie\s*:\s*)[^\r\n]+"#,
            with: "$1<redacted>",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)(authorization\s*:\s*)[^\r\n]+"#,
            with: "$1<redacted>",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"plan-[A-Za-z0-9._-]+"#,
            with: "plan-<redacted>",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)(api[_-]?key|token|secret|password)\s*[=:]\s*([^\s,&"']+)"#,
            with: "$1=<redacted>",
            options: .regularExpression
        )
        result = result.replacingOccurrences(
            of: #"(?i)([?&](?:api[_-]?key|token|secret|password)=)[^&\s]+"#,
            with: "$1<redacted>",
            options: .regularExpression
        )

        return result
    }
}
