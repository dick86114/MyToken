import Foundation

struct OpenCodeUsageProvider: UsageProvider {
    static let defaultBaseURL = URL(string: "https://opencode.ai/console")!

    let descriptor: ProviderDescriptor
    private let session: URLSession
    private let baseURL: URL

    init(
        session: URLSession = .shared,
        baseURL: URL = OpenCodeUsageProvider.defaultBaseURL
    ) {
        self.session = session
        self.baseURL = baseURL
        self.descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == .opencode })!
    }

    func metricCapabilities(for configuration: KeyConfiguration) -> [UsageMetricCapability] {
        guard configuration.credentialKind == .bearerAPIKey else { return [] }
        return [
            Self.capability(metricID: "fiveHour", label: "5 小时", priority: 0),
            Self.capability(metricID: "weekly", label: "周", priority: 1),
            Self.capability(metricID: "monthly", label: "月", priority: 2)
        ]
    }

    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        try await fetchUsage(credential, now: now)
    }

    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        guard credential.providerID == .opencode,
              credential.kind == .bearerAPIKey,
              !credential.secret.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else { throw UsageProviderError.invalidCredential }

        let status = try await requestStatus(credential)
        return try Self.snapshot(from: status, credential: credential, now: now)
    }

    private func requestStatus(_ credential: ProviderCredential) async throws -> OpenCodeGoStatus {
        let url = baseURL.appendingPathComponent("api/go/status")
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 15
        request.setValue("Bearer \(credential.secret)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            throw UsageProviderError.transport
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw UsageProviderError.invalidResponse
        }
        guard (200..<300).contains(httpResponse.statusCode) else {
            throw Self.mapHTTPStatus(httpResponse.statusCode)
        }

        do {
            let status = try JSONDecoder().decode(OpenCodeGoStatus?.self, from: data)
            guard let status else {
                throw UsageProviderError.providerMessage("OpenCode：未找到有效 Go 订阅")
            }
            return status
        } catch let error as UsageProviderError {
            throw error
        } catch {
            await AppLogStore.shared.log(
                level: .error,
                event: "opencode_status_decode_failed",
                details: "reason=\(error)"
            )
            throw UsageProviderError.invalidResponse
        }
    }

    private static func snapshot(
        from status: OpenCodeGoStatus,
        credential: ProviderCredential,
        now: Date
    ) throws -> UsageSnapshot {
        guard let access = status.access, let meters = access.meters,
              let fiveHour = meters.fiveHour,
              let weekly = meters.week,
              let monthly = meters.month,
              status.product == "go" || status.product == "go-plus"
        else {
            throw UsageProviderError.providerMessage("OpenCode：未找到有效 Go 订阅")
        }

        let statusText = renewalStatusText(for: status)
        return UsageSnapshot(
            planName: status.product == "go-plus" ? "OpenCode Go Plus" : "OpenCode Go",
            kind: .periodic,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: [],
            fetchedAt: now,
            statusText: statusText,
            billingMode: status.cancelAtPeriodEnd ? "取消续订" : "自动续费",
            subscriptionStartAt: date(from: access.startsAt),
            subscriptionEndAt: date(from: access.endsAt),
            providerID: .opencode,
            credentialID: credential.credentialID,
            metrics: [
                metric("fiveHour", label: "5 小时", meter: fiveHour),
                metric("weekly", label: "周", meter: weekly),
                metric("monthly", label: "月", meter: monthly)
            ]
        )
    }

    private static func renewalStatusText(for status: OpenCodeGoStatus) -> String {
        if status.renewalPending == true { return "续费处理中" }
        if status.renewalAuthorizationRequired == true { return "需要重新授权支付" }
        if status.cancelAtPeriodEnd { return "将在当前周期结束后到期" }
        return "正常续费"
    }

    private static func metric(
        _ id: String,
        label: String,
        meter: OpenCodeGoMeter
    ) -> NormalizedUsageMetric {
        let used = max(0, meter.usedMicroCents.dollars)
        let limit = max(0, meter.limitMicroCents.dollars)
        return NormalizedUsageMetric(
            id: id,
            label: label,
            used: used,
            limit: limit,
            remaining: max(0, limit - used),
            unit: .currency,
            windowStart: date(from: meter.startsAt),
            windowEnd: date(from: meter.resetsAt),
            presentation: .progress,
            semantic: .usedQuota,
            currencyCode: "$",
            healthState: healthState(used: used, limit: limit)
        )
    }

    private static func healthState(used: Decimal, limit: Decimal) -> UsageMetricHealthState {
        guard limit > 0 else { return .unknown }
        if used >= limit { return .critical }
        if used / limit >= 0.8 { return .warning }
        return .normal
    }

    private static func capability(
        metricID: String,
        label: String,
        priority: Int
    ) -> UsageMetricCapability {
        UsageMetricCapability(
            metricID: metricID,
            label: label,
            presentation: .progress,
            semantic: .usedQuota,
            isMenuBarSelectable: true,
            menuBarPriority: priority,
            defaultAlertEnabled: true,
            defaultAbsoluteAlertThreshold: nil
        )
    }

    private static func mapHTTPStatus(_ statusCode: Int) -> UsageProviderError {
        switch statusCode {
        case 400:
            return .invalidResponse
        case 401:
            return .unauthorized
        case 403:
            return .providerMessage("OpenCode：API Key 没有 Console 状态权限，请创建带读取权限的服务账号 Key")
        case 404:
            return .providerMessage("OpenCode：未找到订阅或接口路径已变化")
        case 429:
            return .rateLimited
        case 500...599:
            return .providerUnavailable
        default:
            return .invalidResponse
        }
    }

    private static func date(from value: String?) -> Date? {
        guard let value, !value.isEmpty else { return nil }
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        if let date = formatter.date(from: value) { return date }
        formatter.formatOptions = [.withInternetDateTime]
        return formatter.date(from: value)
    }
}
