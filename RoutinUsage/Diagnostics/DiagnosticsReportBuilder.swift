import Foundation

struct DiagnosticsReportMetadata: Sendable {
    let version: String
    let operatingSystem: String
    let architecture: String
    let exportedAt: Date
    let updateStatus: String
    let refreshMinutes: Int
    let notificationsEnabled: Bool
    let widgetSyncEnabled: Bool
    let configuredCredentialCount: Int
    let enabledCredentialCount: Int
    let providerCounts: [String: Int]
}

enum DiagnosticsReportBuilder {
    static let maximumLogCharacters = 128 * 1024

    static func makeReport(
        metadata: DiagnosticsReportMetadata,
        diagnostics: EnergyDiagnosticsBuffer,
        logs: String
    ) async -> String {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        var lines: [String] = []
        lines.append("# MyToken 诊断报告")
        lines.append("")

        lines.append("## 环境")
        lines.append("- 版本：\(metadata.version)")
        lines.append("- 系统：\(metadata.operatingSystem)")
        lines.append("- 架构：\(metadata.architecture)")
        lines.append("- 导出时间：\(formatter.string(from: metadata.exportedAt))")
        lines.append("- 更新状态：\(metadata.updateStatus)")
        lines.append("- 刷新间隔：\(metadata.refreshMinutes) 分钟")
        lines.append("- 通知：\(metadata.notificationsEnabled ? "启用" : "关闭")")
        lines.append("- Widget 同步：\(metadata.widgetSyncEnabled ? "启用" : "禁用")")
        lines.append("- 配置凭证：\(metadata.configuredCredentialCount)")
        lines.append("- 启用凭证：\(metadata.enabledCredentialCount)")
        if !metadata.providerCounts.isEmpty {
            let counts = metadata.providerCounts
                .sorted { $0.key < $1.key }
                .map { "\($0.key): \($0.value)" }
                .joined(separator: ", ")
            lines.append("- 供应商分布：\(counts)")
        }

        let events = await diagnostics.currentEvents()
        lines.append("")
        lines.append("## 能耗诊断事件")
        if events.isEmpty {
            lines.append("暂无事件")
        } else {
            for event in events {
                let details = event.details
                    .sorted { $0.key < $1.key }
                    .map { "\($0.key)=\($0.value)" }
                    .joined(separator: " ")
                let suffix = details.isEmpty ? "" : " \(details)"
                lines.append("\(formatter.string(from: event.timestamp)) \(event.name)\(suffix)")
            }
        }

        lines.append("")
        lines.append("## 最近应用日志")
        lines.append("<text>")
        let suffix = String(logs.suffix(maximumLogCharacters))
        lines.append(DiagnosticsRedactor.redact(suffix))
        lines.append("</text>")
        lines.append("")
        lines.append("以上内容由 MyToken 自动生成并脱敏；发送前请再次检查。")
        return lines.joined(separator: "\n")
    }
}
