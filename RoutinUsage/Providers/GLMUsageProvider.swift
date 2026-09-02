import Foundation

struct GLMUsageProvider: UsageProvider {
    let descriptor: ProviderDescriptor
    private let session: URLSession
    private let baseURL: URL

    init(session: URLSession = .shared, baseURL: URL = URL(string: "https://api.z.ai")!) {
        self.session = session
        self.baseURL = baseURL
        self.descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == .glm })!
    }

    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        try await fetchUsage(credential, now: now)
    }

    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        guard credential.providerID == .glm, credential.kind == .apiKey, !credential.secret.isEmpty else {
            throw UsageProviderError.invalidCredential
        }

        let root = credential.metadata["baseURL"]
            .flatMap(URL.init(string:)) ?? baseURL
        let start = Self.format(Self.windowStart(from: now))
        let end = Self.format(Self.windowEnd(from: now))
        let query = "?startTime=\(Self.encode(start))&endTime=\(Self.encode(end))"

        async let model = request(
            url: root.appendingPathComponent("/api/monitor/usage/model-usage").appending(query: query),
            credential: credential
        )
        async let tool = request(
            url: root.appendingPathComponent("/api/monitor/usage/tool-usage").appending(query: query),
            credential: credential
        )
        async let quota = request(
            url: root.appendingPathComponent("/api/monitor/usage/quota/limit"),
            credential: credential
        )

        let (modelValue, toolValue, quotaValue) = try await (model, tool, quota)
        var metrics: [NormalizedUsageMetric] = []
        metrics.append(contentsOf: Self.quotaMetrics(from: quotaValue))
        metrics.append(contentsOf: Self.breakdownMetrics(from: modelValue, prefix: "model", label: "模型"))
        metrics.append(contentsOf: Self.breakdownMetrics(from: toolValue, prefix: "tool", label: "工具"))
        guard !metrics.isEmpty else {
            throw UsageProviderError.invalidResponse
        }

        return UsageSnapshot(
            planName: "Coding Plan",
            kind: .periodic,
            fiveHour: nil,
            weekly: nil,
            token: nil,
            allowedModels: [],
            fetchedAt: now,
            providerID: .glm,
            credentialID: credential.credentialID,
            metrics: metrics
        )
    }

    private func request(url: URL, credential: ProviderCredential) async throws -> GLMJSONValue {
        var request = URLRequest(url: url)
        request.httpMethod = "GET"
        request.timeoutInterval = 15
        request.setValue(credential.secret, forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("en-US,en", forHTTPHeaderField: "Accept-Language")

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
            return try JSONDecoder().decode(GLMJSONValue.self, from: data)
        } catch {
            throw UsageProviderError.invalidResponse
        }
    }

    private static func quotaMetrics(from value: GLMJSONValue) -> [NormalizedUsageMetric] {
        let limits: [GLMJSONValue]
        switch value {
        case let .object(object):
            if case let .object(data)? = object["data"], case let .array(items)? = data["limits"] {
                limits = items
            } else if case let .array(items)? = object["limits"] {
                limits = items
            } else {
                limits = []
            }
        default:
            limits = []
        }

        return limits.enumerated().compactMap { index, item in
            guard case let .object(object) = item,
                  let percentage = object["percentage"]?.numberValue()
            else { return nil }
            let type = object["type"]?.stringValue() ?? "quota"
            let label: String
            if type == "TOKENS_LIMIT" {
                label = "Token 配额（5 小时）"
            } else if type == "TIME_LIMIT" {
                label = "MCP 配额（1 个月）"
            } else {
                label = type
            }
            return NormalizedUsageMetric(
                id: "quota-\(index)-\(type)",
                label: label,
                used: percentage,
                limit: 100,
                remaining: 100 - percentage,
                unit: .token,
                presentation: .progress,
                healthState: percentage >= 80 ? .critical : (percentage >= 50 ? .warning : .normal)
            )
        }
    }

    private static func breakdownMetrics(
        from value: GLMJSONValue,
        prefix: String,
        label: String
    ) -> [NormalizedUsageMetric] {
        let values: [GLMJSONValue]
        switch value {
        case let .object(object):
            if case let .object(data)? = object["data"], case let .array(items)? = data["items"] {
                values = items
            } else if case let .array(items)? = object["data"] {
                values = items
            } else if case let .array(items)? = object["items"] {
                values = items
            } else {
                values = []
            }
        case let .array(items):
            values = items
        default:
            values = []
        }

        return values.enumerated().compactMap { index, item in
            guard case let .object(object) = item else { return nil }
            let name = ["model", "modelName", "tool", "toolName", "name", "id"]
                .compactMap { object[$0]?.stringValue() }
                .first ?? "\(label) \(index + 1)"
            let amount = ["tokens", "tokenCount", "count", "usage", "value", "currentUsage"]
                .compactMap { object[$0]?.numberValue() }
                .first
            guard let amount else { return nil }
            return NormalizedUsageMetric(
                id: "\(prefix)-\(index)",
                label: "\(label)：\(name)",
                value: amount,
                unit: .request,
                presentation: .value,
                healthState: .normal
            )
        }
    }

    private static func windowStart(from date: Date) -> Date {
        Calendar.current.date(byAdding: .day, value: -1, to: date) ?? date
    }

    private static func windowEnd(from date: Date) -> Date {
        Calendar.current.date(bySetting: .minute, value: 59, of: date)
            .flatMap { Calendar.current.date(bySetting: .second, value: 59, of: $0) } ?? date
    }

    private static func format(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.dateFormat = "yyyy-MM-dd HH:mm:ss"
        return formatter.string(from: date)
    }

    private static func encode(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? value
    }
}

private extension URL {
    func appending(query: String) -> URL {
        URL(string: absoluteString + query) ?? self
    }
}
