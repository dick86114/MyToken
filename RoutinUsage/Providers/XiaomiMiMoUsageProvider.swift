import Foundation

/// 小米 MiMo 官方财务接口适配器。
///
/// `sk-` 普通 API Key 读取账户余额与最近 31 天用量；
/// `tp-` 专用 API Key 读取 Token Plan Credits、周期和过期时间。
/// 不再支持网页 Cookie 或任何网页登录态续期。
struct XiaomiMiMoUsageProvider: UsageProvider {
    static let defaultOfficialBaseURL = URL(string: "https://api.xiaomimimo.com/v1")!
    static let defaultTokenPlanBaseURL = URL(string: "https://token-plan-cn.xiaomimimo.com/v1")!
    private static let officialUsageEarliestDate = "2026-09-01"

    let descriptor: ProviderDescriptor
    private let session: URLSession
    private let officialBaseURL: URL
    private let tokenPlanBaseURL: URL

    init(
        session: URLSession = .shared,
        officialBaseURL: URL = XiaomiMiMoUsageProvider.defaultOfficialBaseURL,
        tokenPlanBaseURL: URL = XiaomiMiMoUsageProvider.defaultTokenPlanBaseURL
    ) {
        self.session = session
        self.officialBaseURL = officialBaseURL
        self.tokenPlanBaseURL = tokenPlanBaseURL
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
                label: "消费",
                presentation: .value,
                semantic: .value,
                isMenuBarSelectable: false,
                menuBarPriority: nil,
                defaultAlertEnabled: false,
                defaultAbsoluteAlertThreshold: nil
            ),
            UsageMetricCapability(
                metricID: "total-tokens",
                label: "Token 消耗",
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
              credential.kind == .bearerAPIKey
        else {
            throw UsageProviderError.invalidCredential
        }
        let secret = credential.secret.trimmingCharacters(in: .whitespacesAndNewlines)
        let mode: CredentialMode
        if secret.lowercased().hasPrefix("sk-") {
            mode = .apiKey
        } else if secret.lowercased().hasPrefix("tp-") {
            mode = .tokenPlanKey
        } else {
            throw UsageProviderError.providerMessage(
                "小米 MiMo：请使用官方 API Key（API 按量 sk-，Token Plan tp-）"
            )
        }

        switch (mode, credential.metadata["usageKind"] ?? "api") {
        case (.apiKey, "api"):
            return try await fetchAPIUsage(credential, apiKey: secret, now: now)
        case (.tokenPlanKey, "plan"):
            return try await fetchPlanUsage(credential, apiKey: secret, now: now)
        case (.apiKey, "plan"):
            throw UsageProviderError.providerMessage(
                "小米 MiMo：Token Plan 请使用 tp- 开头的专用 API Key"
            )
        case (.tokenPlanKey, "api"):
            throw UsageProviderError.providerMessage(
                "小米 MiMo：API 按量请使用 sk- 开头的普通 API Key"
            )
        default:
            throw UsageProviderError.invalidCredential
        }
    }

    /// 官方按量接口：余额 + 最近 31 天日用量。
    private func fetchAPIUsage(
        _ credential: ProviderCredential,
        apiKey: String,
        now: Date
    ) async throws -> UsageSnapshot {
        let balanceURL = Self.officialURL(base: officialBaseURL, path: "platform/balance")
        let usageURL = Self.officialUsageURL(base: officialBaseURL, now: now)
        async let balanceValue = requestJSON(url: balanceURL, apiKey: apiKey)
        async let usageValue = requestJSON(url: usageURL, apiKey: apiKey)
        let (balancePayload, usagePayload) = try await (balanceValue, usageValue)

        let balance = Self.object(from: balancePayload)
        guard let balanceInfo = Self.firstObject(in: balance, key: "balance_infos") else {
            throw UsageProviderError.invalidResponse
        }
        let currency = Self.normalizedCurrency(Self.firstString(in: balanceInfo, keys: ["currency"]))
        let cashBalance = Self.firstNumber(in: balanceInfo, keys: ["cash_balance"]) ?? 0
        let giftBalance = Self.firstNumber(in: balanceInfo, keys: ["gift_balance"]) ?? 0
        let availableBalance = Self.firstNumber(in: balanceInfo, keys: ["total_available"])
            ?? cashBalance + giftBalance + (Self.firstNumber(in: balanceInfo, keys: ["credit_available"]) ?? 0)
        let isAvailable = Self.firstBool(in: balance, keys: ["is_available"]) ?? (availableBalance > 0)
        let warningThreshold = credential.metadata["balanceWarningThreshold"]
            .flatMap { Decimal(string: $0) }
        let health = UsageMetricHealthEvaluator.balanceState(
            balance: availableBalance,
            warningThreshold: warningThreshold,
            isAvailable: isAvailable
        )

        let usageRoot = Self.object(from: usagePayload)
        let records: [GLMJSONValue]
        if case let .array(values)? = usageRoot["data"] {
            records = values
        } else {
            records = []
        }
        var totalCost: Decimal = 0
        var totalTokens: Decimal = 0
        var inputMissTokens: Decimal = 0
        var outputTokens: Decimal = 0
        var cacheTokens: Decimal = 0
        for value in records {
            guard case let .object(record) = value else { continue }
            totalCost += Self.firstNumber(in: record, keys: ["consumed_amount"]) ?? 0
            let recordCache = Self.firstNumber(in: record, keys: ["input_cache_hit_tokens"]) ?? 0
            let recordMiss = Self.firstNumber(in: record, keys: ["input_cache_miss_tokens"]) ?? 0
            let recordOutput = Self.firstNumber(in: record, keys: ["output_tokens"]) ?? 0
            cacheTokens += recordCache
            inputMissTokens += recordMiss
            outputTokens += recordOutput
            let components = recordCache + recordMiss + recordOutput
            totalTokens += max(Self.firstNumber(in: record, keys: ["total_tokens"]) ?? 0, components)
        }

        return UsageSnapshot(
            planName: "API 按量",
            kind: .periodic,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: Self.defaultAllowedModels,
            fetchedAt: now,
            statusText: "可用",
            billingMode: "按量计费",
            providerID: .xiaomi,
            credentialID: credential.credentialID,
            metrics: [
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
                    value: cashBalance,
                    unit: .currency,
                    presentation: .value,
                    semantic: .value,
                    currencyCode: currency,
                    healthState: health
                ),
                NormalizedUsageMetric(
                    id: "gift-balance",
                    label: "赠送余额",
                    value: giftBalance,
                    unit: .currency,
                    presentation: .value,
                    semantic: .value,
                    currencyCode: currency,
                    healthState: health
                ),
                NormalizedUsageMetric(
                    id: "total-consumption",
                    label: "近 31 天消费",
                    value: totalCost,
                    unit: .currency,
                    presentation: .value,
                    semantic: .value,
                    currencyCode: currency
                ),
                NormalizedUsageMetric(
                    id: "total-tokens",
                    label: "近 31 天消耗",
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
        )
    }

    /// 官方 Token Plan 接口：Credits 余量与当前周期，周期结束时间即过期时间。
    private func fetchPlanUsage(
        _ credential: ProviderCredential,
        apiKey: String,
        now: Date
    ) async throws -> UsageSnapshot {
        let url = Self.officialURL(base: tokenPlanBaseURL, path: "platform/token_plan")
        let payload = try await requestJSON(url: url, apiKey: apiKey)
        let root = Self.object(from: payload)
        guard let quota = Self.objectValue(in: root, key: "quota") else {
            throw UsageProviderError.invalidResponse
        }
        let totalCredits = Self.firstNumber(in: quota, keys: ["total_credits"]) ?? 0
        let usedCredits = Self.firstNumber(in: quota, keys: ["used_credits"]) ?? 0
        let remainingCredits = Self.firstNumber(in: quota, keys: ["remaining_credits"])
            ?? max(0, totalCredits - usedCredits)
        let period = Self.objectValue(in: root, key: "period")
        let periodStart = Self.parseDate(Self.firstString(in: period ?? [:], keys: ["start_at", "startAt"]))
        let periodEnd = Self.parseDate(Self.firstString(in: period ?? [:], keys: ["end_at", "endAt"]))
        let expired = periodEnd.map { $0 <= now } ?? false
        let percent = Self.percent(used: usedCredits, limit: totalCredits)
        let autoRenew = Self.firstBool(in: root, keys: ["auto_renew", "autoRenew"])
        let billingMode: String?
        if autoRenew == true {
            billingMode = "自动续费"
        } else if autoRenew == false {
            billingMode = "手动续费"
        } else {
            billingMode = nil
        }

        return UsageSnapshot(
            planName: Self.planName(for: Self.firstString(in: root, keys: ["plan_code", "planCode"])),
            subscriptionId: nil,
            planId: Self.firstString(in: root, keys: ["plan_code", "planCode"]),
            kind: .periodic,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: Self.defaultAllowedModels,
            fetchedAt: now,
            status: expired ? 0 : 1,
            statusText: expired ? "已失效" : "有效",
            billingMode: billingMode,
            subscriptionStartAt: periodStart,
            subscriptionEndAt: periodEnd,
            providerID: .xiaomi,
            credentialID: credential.credentialID,
            metrics: [
                NormalizedUsageMetric(
                    id: "plan-total",
                    label: "套餐总量",
                    used: usedCredits,
                    limit: totalCredits,
                    remaining: remainingCredits,
                    unit: .token,
                    windowStart: periodStart,
                    windowEnd: periodEnd,
                    presentation: .progress,
                    semantic: .usedQuota,
                    healthState: Self.percentHealthState(percent)
                ),
                NormalizedUsageMetric(
                    id: "plan-status",
                    label: "订阅状态",
                    value: expired ? 0 : 1,
                    unit: .boolean,
                    presentation: .status,
                    semantic: .status,
                    healthState: expired ? .unavailable : .normal
                )
            ]
        )
    }

    private func requestJSON(url: URL, apiKey: String) async throws -> GLMJSONValue {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 15
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json, text/plain, */*", forHTTPHeaderField: "Accept")
        request.setValue("zh-CN,zh;q=0.9,en;q=0.8", forHTTPHeaderField: "Accept-Language")

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

    private enum CredentialMode {
        case apiKey
        case tokenPlanKey
    }

    private static func officialURL(base: URL, path: String) -> URL {
        path.split(separator: "/").reduce(base) { url, component in
            url.appendingPathComponent(String(component))
        }
    }

    private static func officialUsageURL(base: URL, now: Date) -> URL {
        let calendar = Calendar(identifier: .gregorian)
        let endDate = utcDateString(now)
        let thirtyDaysAgo = calendar.date(byAdding: .day, value: -30, to: now) ?? now
        let startDate = max(utcDateString(thirtyDaysAgo), officialUsageEarliestDate)
        var components = URLComponents(
            url: officialURL(base: base, path: "platform/usage/daily"),
            resolvingAgainstBaseURL: false
        )!
        components.queryItems = [
            URLQueryItem(name: "start_date", value: min(startDate, endDate)),
            URLQueryItem(name: "end_date", value: endDate)
        ]
        return components.url!
    }

    private static func utcDateString(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyy-MM-dd"
        return formatter.string(from: date)
    }

    private static func firstObject(
        in object: [String: GLMJSONValue],
        key: String
    ) -> [String: GLMJSONValue]? {
        guard case let .array(values)? = object[key],
              case let .object(value)? = values.first
        else {
            return nil
        }
        return value
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
        let normalized = planCode.lowercased()
        for (suffix, label) in [
            ("_year", " 年度套餐"),
            ("_month", " 月度套餐"),
            ("_week", " 周套餐")
        ] where normalized.hasSuffix(suffix) {
            let base = String(normalized.dropLast(suffix.count))
                .replacingOccurrences(of: "_", with: " ")
                .capitalized
            return base + label
        }
        return planCode
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
