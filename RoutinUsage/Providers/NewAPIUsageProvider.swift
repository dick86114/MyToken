import Foundation

struct NewAPIUsageProvider: UsageProvider {
    let descriptor: ProviderDescriptor
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
        self.descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == .newAPI })!
    }

    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        try await fetchUsage(credential, now: now)
    }

    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        guard credential.providerID == .newAPI,
              credential.kind == .bearerAPIKey,
              !credential.secret.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
              let rawUserID = credential.metadata["userID"],
              let userID = Int(rawUserID.trimmingCharacters(in: .whitespacesAndNewlines)),
              userID > 0,
              let root = Self.baseURL(from: credential.metadata["baseURL"])
        else { throw UsageProviderError.invalidCredential }

        let user: NewAPIUser
        do {
            let envelope = try await request(
                root.appendingPathComponent("api/user/self"),
                credential: credential,
                as: NewAPIEnvelope<NewAPIUser>.self
            )
            guard envelope.success != false, let data = envelope.data else {
                throw UsageProviderError.providerMessage("New API：\(envelope.message ?? "认证失败")")
            }
            user = data
        } catch let error as UsageProviderError {
            throw error
        } catch {
            throw UsageProviderError.invalidResponse
        }

        let calendar = Calendar.current
        let todayStart = calendar.startOfDay(for: now)
        let sevenDayStart = calendar.date(byAdding: .day, value: -7, to: todayStart) ?? todayStart
        let thirtyDayStart = calendar.date(byAdding: .day, value: -30, to: todayStart) ?? todayStart
        async let today = requestStat(root: root, start: todayStart, end: now, credential: credential)
        async let sevenDays = requestStat(root: root, start: sevenDayStart, end: now, credential: credential)
        async let thirtyDays = requestStat(root: root, start: thirtyDayStart, end: now, credential: credential)
        let (todayStat, sevenDayStat, thirtyDayStat) = try await (today, sevenDays, thirtyDays)

        let total = user.quota
        let used = user.usedQuota
        let remaining = max(0, total - used)
        let threshold = credential.metadata["balanceWarningThreshold"].flatMap { Decimal(string: $0) }
        let health = UsageMetricHealthEvaluator.balanceState(
            balance: remaining,
            warningThreshold: threshold,
            isAvailable: true
        )
        let group = user.group?.trimmingCharacters(in: .whitespacesAndNewlines)
        let planName = group.map { "New API · \($0)" } ?? "New API"

        return UsageSnapshot(
            planName: planName,
            kind: .periodic,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: [],
            fetchedAt: now,
            providerID: .newAPI,
            credentialID: credential.credentialID,
            metrics: [
                NormalizedUsageMetric(
                    id: "remaining-quota",
                    label: "剩余额度",
                    value: remaining,
                    unit: .currency,
                    presentation: .balance,
                    currencyCode: "额度",
                    healthState: health
                ),
                Self.valueMetric(id: "total-quota", label: "总额度", value: total, health: health),
                Self.valueMetric(id: "used-quota", label: "已用额度", value: used, health: health),
                Self.valueMetric(id: "today-quota", label: "今日消费", value: todayStat.quota, health: .normal),
                Self.valueMetric(id: "seven-day-quota", label: "近 7 天消费", value: sevenDayStat.quota, health: .normal),
                Self.valueMetric(id: "thirty-day-quota", label: "近 30 天消费", value: thirtyDayStat.quota, health: .normal),
                NormalizedUsageMetric(
                    id: "rpm",
                    label: "近 60 秒请求",
                    value: Decimal(todayStat.rpm),
                    unit: .request,
                    presentation: .value,
                    healthState: .normal
                ),
                NormalizedUsageMetric(
                    id: "tpm",
                    label: "近 60 秒 Token",
                    value: Decimal(todayStat.tpm),
                    unit: .token,
                    presentation: .value,
                    healthState: .normal
                ),
                NormalizedUsageMetric(
                    id: "request-count",
                    label: "累计请求",
                    value: Decimal(user.requestCount),
                    unit: .request,
                    presentation: .value,
                    healthState: .normal
                )
            ]
        )
    }

    private func requestStat(
        root: URL,
        start: Date,
        end: Date,
        credential: ProviderCredential
    ) async throws -> NewAPIStat {
        var components = URLComponents(url: root.appendingPathComponent("api/log/self/stat"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "start_timestamp", value: String(Int64(start.timeIntervalSince1970))),
            URLQueryItem(name: "end_timestamp", value: String(Int64(end.timeIntervalSince1970))),
            URLQueryItem(name: "type", value: "2")
        ]
        guard let url = components?.url else { throw UsageProviderError.invalidCredential }
        let envelope = try await request(url, credential: credential, as: NewAPIEnvelope<NewAPIStat>.self)
        guard envelope.success != false, let data = envelope.data else {
            throw UsageProviderError.providerMessage("New API：\(envelope.message ?? "统计接口返回失败")")
        }
        return data
    }

    private func request<T: Decodable>(
        _ url: URL,
        credential: ProviderCredential,
        as type: T.Type
    ) async throws -> T {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 15
        request.setValue("Bearer \(credential.secret)", forHTTPHeaderField: "Authorization")
        request.setValue(credential.metadata["userID"], forHTTPHeaderField: "New-Api-User")
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
            switch httpResponse.statusCode {
            case 401, 403: throw UsageProviderError.unauthorized
            case 429: throw UsageProviderError.rateLimited
            case 500...599: throw UsageProviderError.providerUnavailable
            default: throw UsageProviderError.invalidResponse
            }
        }
        do {
            return try JSONDecoder().decode(type, from: data)
        } catch {
            throw UsageProviderError.invalidResponse
        }
    }

    private static func baseURL(from rawValue: String?) -> URL? {
        guard let rawValue,
              var components = URLComponents(string: rawValue.trimmingCharacters(in: .whitespacesAndNewlines)),
              components.scheme != nil,
              components.host != nil
        else { return nil }
        while components.path.hasSuffix("/") { components.path.removeLast() }
        if components.path == "/api" { components.path = "" }
        return components.url
    }

    private static func valueMetric(
        id: String,
        label: String,
        value: Decimal,
        health: UsageMetricHealthState
    ) -> NormalizedUsageMetric {
        NormalizedUsageMetric(
            id: id,
            label: label,
            value: value,
            unit: .currency,
            presentation: .value,
            currencyCode: "额度",
            healthState: health
        )
    }
}

private struct NewAPIEnvelope<Value: Decodable>: Decodable {
    let success: Bool?
    let message: String?
    let data: Value?
}

private struct NewAPIUser: Decodable {
    let quota: Decimal
    let usedQuota: Decimal
    let requestCount: Int
    let group: String?

    private enum CodingKeys: String, CodingKey {
        case quota
        case usedQuota = "used_quota"
        case requestCount = "request_count"
        case group
    }
}

private struct NewAPIStat: Decodable {
    let quota: Decimal
    let rpm: Int
    let tpm: Int
}
