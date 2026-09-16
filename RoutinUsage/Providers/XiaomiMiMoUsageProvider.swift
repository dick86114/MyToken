import Foundation

struct XiaomiMiMoUsageProvider: UsageProvider {
    static let defaultBaseURL = URL(string: "https://platform.xiaomimimo.com/api/v1")!

    let descriptor: ProviderDescriptor
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, baseURL: URL = XiaomiMiMoUsageProvider.defaultBaseURL) {
        self.session = session
        self.baseURL = baseURL
        self.descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == .xiaomi })!
    }

    func metricCapabilities(for configuration: KeyConfiguration) -> [UsageMetricCapability] {
        guard configuration.credentialKind == .bearerAPIKey else { return [] }
        if configuration.metadata["usageKind"] == "plan" {
            return [
                UsageMetricCapability(
                    metricID: "plan-total",
                    label: "套餐总量",
                    presentation: .progress,
                    semantic: .usedQuota,
                    isMenuBarSelectable: true,
                    menuBarPriority: 0,
                    defaultAlertEnabled: true,
                    defaultAbsoluteAlertThreshold: nil
                ),
                UsageMetricCapability(
                    metricID: "plan-compensation",
                    label: "补偿 Credits",
                    presentation: .value,
                    semantic: .value,
                    isMenuBarSelectable: false,
                    menuBarPriority: nil,
                    defaultAlertEnabled: false,
                    defaultAbsoluteAlertThreshold: nil
                ),
                UsageMetricCapability(
                    metricID: "plan-status",
                    label: "订阅状态",
                    presentation: .status,
                    semantic: .status,
                    isMenuBarSelectable: false,
                    menuBarPriority: nil,
                    defaultAlertEnabled: false,
                    defaultAbsoluteAlertThreshold: nil
                )
            ]
        }

        let warningThreshold = configuration.metadata["balanceWarningThreshold"]
            .flatMap { Decimal(string: $0) }
        return [
            UsageMetricCapability(
                metricID: "account-balance",
                label: "账户余额",
                presentation: .balance,
                semantic: .balance,
                isMenuBarSelectable: true,
                menuBarPriority: 0,
                defaultAlertEnabled: true,
                defaultAbsoluteAlertThreshold: warningThreshold
            ),
            UsageMetricCapability(
                metricID: "total-consumption",
                label: "累计消费",
                presentation: .value,
                semantic: .value,
                isMenuBarSelectable: false,
                menuBarPriority: nil,
                defaultAlertEnabled: false,
                defaultAbsoluteAlertThreshold: nil
            ),
            UsageMetricCapability(
                metricID: "total-tokens",
                label: "历史消耗",
                presentation: .value,
                semantic: .value,
                isMenuBarSelectable: true,
                menuBarPriority: 1,
                defaultAlertEnabled: false,
                defaultAbsoluteAlertThreshold: nil
            )
        ]
    }

    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        try await fetchUsage(credential, now: now)
    }

    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        guard credential.providerID == .xiaomi,
              credential.kind == .bearerAPIKey,
              !credential.secret.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
        else {
            throw UsageProviderError.invalidCredential
        }

        let usageKind = credential.metadata["usageKind"] ?? "api"
        switch usageKind {
        case "plan":
            return try await fetchPlanUsage(credential, now: now)
        case "api":
            return try await fetchAPIUsage(credential, now: now)
        default:
            throw UsageProviderError.invalidCredential
        }
    }

    private func fetchAPIUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot {
        async let balanceValue = requestJSON(path: "balance", credential: credential)
        async let usageValue = requestJSON(path: "usage", credential: credential)
        let (balancePayload, usagePayload) = try await (balanceValue, usageValue)
        let balance = Self.object(from: balancePayload)
        let usage = Self.object(from: usagePayload)
        let availableBalance = Self.firstNumber(in: balance, keys: [
            "balance", "availableBalance", "totalBalance"
        ]) ?? 0
        guard availableBalance != 0 || !balance.isEmpty else {
            throw UsageProviderError.invalidResponse
        }

        let currency = Self.normalizedCurrency(Self.firstString(in: balance, keys: ["currency", "currencyCode"]))
        let warningThreshold = credential.metadata["balanceWarningThreshold"]
            .flatMap { Decimal(string: $0) }
        let health = UsageMetricHealthEvaluator.balanceState(
            balance: availableBalance,
            warningThreshold: warningThreshold,
            isAvailable: true
        )
        let tokenUsage = Self.objectValue(in: usage, key: "tokenUsage")
        let costUsage = Self.objectValue(in: usage, key: "costUsage")
        let inputTokens = Self.firstNumber(in: tokenUsage, keys: ["inputToken", "input_tokens"]) ?? 0
        let outputTokens = Self.firstNumber(in: tokenUsage, keys: ["outputToken", "output_tokens"]) ?? 0
        let cacheTokens = Self.firstNumber(in: tokenUsage, keys: ["cacheToken", "cache_tokens"]) ?? 0
        let inputMissTokens = max(0, inputTokens - cacheTokens)
        let totalTokens = inputTokens + outputTokens
        let totalCost = Self.firstNumber(in: costUsage, keys: ["totalCost", "total_cost"]) ?? 0
        let availableModels = Self.defaultAllowedModels

        let metrics = [
            NormalizedUsageMetric(
                id: "account-balance",
                label: "账户余额",
                value: availableBalance,
                unit: .currency,
                presentation: .balance,
                semantic: .balance,
                currencyCode: currency,
                healthState: health
            ),
            NormalizedUsageMetric(
                id: "cash-balance",
                label: "现金余额",
                value: Self.firstNumber(in: balance, keys: ["cashBalance", "cash_balance"]) ?? 0,
                unit: .currency,
                presentation: .value,
                semantic: .value,
                currencyCode: currency,
                healthState: health
            ),
            NormalizedUsageMetric(
                id: "gift-balance",
                label: "赠送余额",
                value: Self.firstNumber(in: balance, keys: ["giftBalance", "gift_balance"]) ?? 0,
                unit: .currency,
                presentation: .value,
                semantic: .value,
                currencyCode: currency,
                healthState: health
            ),
            NormalizedUsageMetric(
                id: "total-consumption",
                label: "累计消费",
                value: totalCost,
                unit: .currency,
                presentation: .value,
                semantic: .value,
                currencyCode: currency
            ),
            NormalizedUsageMetric(
                id: "total-tokens",
                label: "历史消耗",
                value: totalTokens,
                unit: .token,
                presentation: .value,
                semantic: .value
            ),
            NormalizedUsageMetric(
                id: "input-tokens",
                label: "未命中缓存",
                value: inputMissTokens,
                unit: .token,
                presentation: .value,
                semantic: .value
            ),
            NormalizedUsageMetric(
                id: "output-tokens",
                label: "输出",
                value: outputTokens,
                unit: .token,
                presentation: .value,
                semantic: .value
            ),
            NormalizedUsageMetric(
                id: "cache-tokens",
                label: "命中缓存",
                value: cacheTokens,
                unit: .token,
                presentation: .value,
                semantic: .value
            )
        ]

        return UsageSnapshot(
            planName: "API 按量",
            kind: .periodic,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: availableModels,
            fetchedAt: now,
            statusText: "可用",
            billingMode: "按量计费",
            providerID: .xiaomi,
            credentialID: credential.credentialID,
            metrics: metrics
        )
    }

    private func fetchPlanUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot {
        async let detailValue = requestJSON(path: "tokenPlan/detail", credential: credential)
        async let usageValue = requestJSON(path: "tokenPlan/usage", credential: credential)
        let (detailPayload, usagePayload) = try await (detailValue, usageValue)
        let detail = Self.object(from: detailPayload)
        let usage = Self.object(from: usagePayload)

        let periodEnd = Self.parseDate(Self.firstString(
            in: detail,
            keys: ["currentPeriodEnd", "current_period_end", "expireTime", "expiredTime"]
        ))
        let itemsValue = Self.objectValue(in: usage, key: "usage")?["items"] ?? usage["items"]
        let items: [GLMJSONValue]
        if case let .array(values)? = itemsValue {
            items = values
        } else {
            items = []
        }
        let metrics = items.compactMap { item -> NormalizedUsageMetric? in
            guard case let .object(object) = item else { return nil }
            let name = Self.firstString(
                in: object,
                keys: ["name", "metricName", "type"]
            ) ?? "usage"
            let used = Self.firstNumber(
                in: object,
                keys: ["used", "currentValue", "consumed"]
            ) ?? 0
            let limit = Self.firstNumber(
                in: object,
                keys: ["limit", "total", "quota"]
            ) ?? 0
            let isCompensation = name.lowercased().contains("compensation")
            guard limit > 0 || isCompensation else { return nil }

            let id = Self.metricID(for: name)
            let label = Self.metricLabel(for: name)
            let remaining = max(0, limit - used)
            let percent = Self.firstNumber(in: object, keys: ["percent", "percentage"])
                .map { NSDecimalNumber(decimal: $0).doubleValue }
                ?? Self.percent(used: used, limit: limit)
            let health = Self.percentHealthState(percent)
            let metricEnd = Self.parseDate(Self.firstString(
                in: object,
                keys: ["resetTime", "expireTime", "windowEnd", "nextResetTime"]
            )) ?? periodEnd

            if isCompensation {
                return NormalizedUsageMetric(
                    id: id,
                    label: label,
                    value: used,
                    unit: .token,
                    presentation: .value,
                    semantic: .value,
                    healthState: health
                )
            }

            return NormalizedUsageMetric(
                id: id,
                label: label,
                used: used,
                limit: limit,
                remaining: remaining,
                unit: .token,
                windowEnd: metricEnd,
                presentation: .progress,
                semantic: .usedQuota,
                healthState: health
            )
        }
        guard !metrics.isEmpty else {
            throw UsageProviderError.invalidResponse
        }

        let expired = Self.firstBool(in: detail, keys: ["expired", "isExpired"]) ?? false
        let status = expired ? "已失效" : "有效"
        let autoRenew = Self.firstBool(
            in: detail,
            keys: ["enableAutoRenew", "hasAutoRenewSubscribed", "autoRenew"]
        )
        let billingMode: String?
        if autoRenew == true {
            billingMode = "自动续费"
        } else if autoRenew == false {
            billingMode = "手动续费"
        } else {
            billingMode = nil
        }
        let statusMetric = NormalizedUsageMetric(
            id: "plan-status",
            label: "订阅状态",
            value: expired ? 0 : 1,
            unit: .boolean,
            presentation: .status,
            semantic: .status,
            healthState: expired ? .unavailable : .normal
        )

        return UsageSnapshot(
            planName: Self.firstString(in: detail, keys: ["planName", "name"])
                ?? Self.planName(for: Self.firstString(in: detail, keys: ["planCode"])),
            subscriptionId: nil,
            planId: Self.firstString(in: detail, keys: ["planCode"]),
            kind: .periodic,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: Self.defaultAllowedModels,
            fetchedAt: now,
            status: expired ? 0 : 1,
            statusText: status,
            billingMode: billingMode,
            subscriptionStartAt: Self.parseDate(Self.firstString(
                in: detail,
                keys: ["currentPeriodStart", "current_period_start", "startTime"]
            )),
            subscriptionEndAt: periodEnd,
            providerID: .xiaomi,
            credentialID: credential.credentialID,
            metrics: metrics + [statusMetric]
        )
    }

    private func requestJSON(
        path: String,
        credential: ProviderCredential
    ) async throws -> GLMJSONValue {
        guard let cookie = Self.cookieHeader(from: credential.secret) else {
            throw UsageProviderError.invalidCredential
        }
        let url = baseURL.appendingPathComponent(path)
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 15
        request.setValue(cookie, forHTTPHeaderField: "Cookie")
        request.setValue("application/json, text/plain, */*", forHTTPHeaderField: "Accept")
        request.setValue("zh-CN,zh;q=0.9,en;q=0.8", forHTTPHeaderField: "Accept-Language")
        request.setValue(TimeZone.current.identifier, forHTTPHeaderField: "x-timeZone")
        request.setValue("https://platform.xiaomimimo.com/", forHTTPHeaderField: "Referer")
        request.setValue("https://platform.xiaomimimo.com", forHTTPHeaderField: "Origin")

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
            if let message = Self.errorMessage(from: data), !message.isEmpty {
                throw UsageProviderError.providerMessage("小米 MiMo：\(message)")
            }
            switch httpResponse.statusCode {
            case 401, 403:
                throw UsageProviderError.unauthorized
            case 429:
                throw UsageProviderError.rateLimited
            case 500...599:
                throw UsageProviderError.providerUnavailable
            default:
                throw UsageProviderError.invalidResponse
            }
        }

        do {
            return try JSONDecoder().decode(GLMJSONValue.self, from: data)
        } catch {
            throw UsageProviderError.invalidResponse
        }
    }

    private static func object(from value: GLMJSONValue) -> [String: GLMJSONValue] {
        guard case let .object(root) = value else { return [:] }
        if case let .object(data)? = root["data"] {
            return data
        }
        if case let .object(result)? = root["result"] {
            return result
        }
        return root
    }

    private static func objectValue(
        in object: [String: GLMJSONValue],
        key: String
    ) -> [String: GLMJSONValue]? {
        guard case let .object(value)? = object[key] else { return nil }
        return value
    }

    private static func firstNumber(
        in object: [String: GLMJSONValue]?,
        keys: [String]
    ) -> Decimal? {
        guard let object else { return nil }
        for key in keys {
            switch object[key] {
            case let .number(value)?:
                return value
            case let .string(value)?:
                let normalized = value
                    .replacingOccurrences(of: ",", with: "")
                    .replacingOccurrences(of: "%", with: "")
                    .trimmingCharacters(in: .whitespacesAndNewlines)
                if let number = Decimal(string: normalized) {
                    return number
                }
            default:
                continue
            }
        }
        return nil
    }

    private static func firstString(
        in object: [String: GLMJSONValue],
        keys: [String]
    ) -> String? {
        for key in keys {
            if let value = object[key]?.stringValue(), !value.isEmpty {
                return value
            }
        }
        return nil
    }

    private static func firstBool(
        in object: [String: GLMJSONValue],
        keys: [String]
    ) -> Bool? {
        for key in keys {
            guard case let .bool(value)? = object[key] else { continue }
            return value
        }
        return nil
    }

    private static func normalizedCurrency(_ rawValue: String?) -> String {
        guard let value = rawValue?
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .uppercased(),
              !value.isEmpty
        else {
            return "CNY"
        }
        return value
    }

    private static func cookieHeader(from rawValue: String) -> String? {
        var value = rawValue.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty,
              !value.contains("\r"),
              !value.contains("\n")
        else {
            return nil
        }
        if value.lowercased().hasPrefix("cookie:") {
            value = String(value.dropFirst("cookie:".count)).trimmingCharacters(in: .whitespacesAndNewlines)
        }
        guard !value.isEmpty else { return nil }
        if value.contains("=") {
            return value
        }
        return "api-platform_serviceToken=\(value)"
    }

    private static func errorMessage(from data: Data) -> String? {
        guard let value = try? JSONSerialization.jsonObject(with: data) as? [String: Any] else {
            return nil
        }
        if let message = value["message"] as? String {
            return message
        }
        if let error = value["error"] as? [String: Any] {
            return error["message"] as? String
        }
        return nil
    }

    private static func metricID(for name: String) -> String {
        let normalized = name.lowercased()
        if normalized.contains("compensation") { return "plan-compensation" }
        if normalized.contains("five") || normalized.contains("5h") { return "plan-five-hour" }
        if normalized.contains("total") { return "plan-total" }
        let sanitized = normalized
            .replacingOccurrences(of: "_", with: "-")
            .filter { $0.isLetter || $0.isNumber || $0 == "-" }
        return sanitized.isEmpty ? "plan-usage" : "plan-\(sanitized)"
    }

    private static func metricLabel(for name: String) -> String {
        let normalized = name.lowercased()
        if normalized.contains("compensation") { return "补偿 Credits" }
        if normalized.contains("five") || normalized.contains("5h") { return "每 5 小时" }
        if normalized.contains("total") { return "套餐总量" }
        return name
            .replacingOccurrences(of: "_", with: " ")
            .replacingOccurrences(of: "token", with: "Credits", options: .caseInsensitive)
    }

    private static func percent(used: Decimal, limit: Decimal) -> Double {
        guard limit > 0 else { return 0 }
        return NSDecimalNumber(decimal: used)
            .dividing(by: NSDecimalNumber(decimal: limit))
            .multiplying(by: 100)
            .doubleValue
    }

    private static func percentHealthState(_ percent: Double) -> UsageMetricHealthState {
        switch percent {
        case ..<70:
            return .normal
        case ..<90:
            return .warning
        default:
            return .critical
        }
    }

    private static func parseDate(_ rawValue: String?) -> Date? {
        guard let rawValue = rawValue?.trimmingCharacters(in: .whitespacesAndNewlines),
              !rawValue.isEmpty
        else {
            return nil
        }
        if let value = Double(rawValue) {
            let seconds = value > 10_000_000_000 ? value / 1_000 : value
            return Date(timeIntervalSince1970: seconds)
        }
        if let date = ISO8601DateFormatter().date(from: rawValue) {
            return date
        }
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.date(from: rawValue)
    }

    private static func planName(for planCode: String?) -> String {
        guard let planCode, !planCode.isEmpty else { return "Token Plan" }
        return planCode
            .replacingOccurrences(of: "_year", with: " 年度套餐")
            .replacingOccurrences(of: "_", with: " ")
            .capitalized
    }

    private static let defaultAllowedModels = [
        "mimo-v2.5-pro",
        "mimo-v2.5",
        "mimo-v2.5-asr",
        "mimo-v2.5-tts",
        "mimo-v2.5-tts-voicedesign",
        "mimo-v2.5-tts-voiceclone"
    ]
}
