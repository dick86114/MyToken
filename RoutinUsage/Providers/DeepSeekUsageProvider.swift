import Foundation

struct DeepSeekUsageProvider: UsageProvider {
    static let endpoint = URL(string: "https://api.deepseek.com/user/balance")!
    static let modelsEndpoint = URL(string: "https://api.deepseek.com/models")!

    let descriptor: ProviderDescriptor
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
        self.descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == .deepseek })!
    }

    func metricCapabilities(for configuration: KeyConfiguration) -> [UsageMetricCapability] {
        guard configuration.credentialKind == .apiKey else { return [] }
        let warningThreshold = configuration.metadata["balanceWarningThreshold"]
            .flatMap { Decimal(string: $0) }
        return [
            UsageMetricCapability(
                metricID: "balance",
                label: "余额",
                presentation: .balance,
                semantic: .balance,
                isMenuBarSelectable: true,
                menuBarPriority: 0,
                defaultAlertEnabled: true,
                defaultAbsoluteAlertThreshold: warningThreshold
            ),
            UsageMetricCapability(
                metricID: "availability",
                label: "账户状态",
                presentation: .status,
                semantic: .status,
                isMenuBarSelectable: false,
                menuBarPriority: nil,
                defaultAlertEnabled: false,
                defaultAbsoluteAlertThreshold: nil
            )
        ]
    }

    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        try await fetchUsage(credential, now: now)
    }

    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        guard credential.providerID == .deepseek, credential.kind == .apiKey, !credential.secret.isEmpty else {
            throw UsageProviderError.invalidCredential
        }

        var request = URLRequest(url: Self.endpoint)
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

        let allowedModels: [String]
        do {
            allowedModels = try await fetchAllowedModels(credential)
        } catch is CancellationError {
            throw CancellationError()
        } catch {
            allowedModels = []
        }

        do {
            let payload = try JSONDecoder().decode(DeepSeekBalanceResponse.self, from: data)
            guard !payload.balanceInfos.isEmpty else {
                throw UsageProviderError.invalidResponse
            }
            let warningThreshold: Decimal?
            if let rawThreshold = credential.metadata["balanceWarningThreshold"] {
                warningThreshold = Decimal(string: rawThreshold)
            } else {
                warningThreshold = nil
            }

            // 接口可能同时返回多种币种余额（例如 CNY 与 USD），逐币种生成一组指标，
            // 避免只展示第一个币种而丢失其余账户余额。
            let hasMultipleCurrencies = payload.balanceInfos.count > 1
            var metrics: [NormalizedUsageMetric] = []
            for (index, balance) in payload.balanceInfos.enumerated() {
                let currency = balance.currency.isEmpty ? "CNY" : balance.currency
                let total = Decimal(string: balance.totalBalance) ?? 0
                let granted = Decimal(string: balance.grantedBalance) ?? 0
                let toppedUp = Decimal(string: balance.toppedUpBalance) ?? 0
                let health = UsageMetricHealthEvaluator.balanceState(
                    balance: total,
                    warningThreshold: warningThreshold,
                    isAvailable: payload.isAvailable
                )
                let suffix = hasMultipleCurrencies ? "-\(currency.uppercased())" : ""
                let currencyLabel = hasMultipleCurrencies ? "（\(currency.uppercased())）" : ""

                metrics.append(contentsOf: [
                    NormalizedUsageMetric(
                        id: "balance\(suffix)",
                        label: "余额\(currencyLabel)",
                        value: total,
                        unit: .currency,
                        presentation: .balance,
                        semantic: .balance,
                        currencyCode: currency,
                        healthState: health
                    ),
                    NormalizedUsageMetric(
                        id: "grantedBalance\(suffix)",
                        label: "赠金余额\(currencyLabel)",
                        value: granted,
                        unit: .currency,
                        presentation: .value,
                        semantic: .value,
                        currencyCode: currency,
                        healthState: health
                    ),
                    NormalizedUsageMetric(
                        id: "toppedUpBalance\(suffix)",
                        label: "充值余额\(currencyLabel)",
                        value: toppedUp,
                        unit: .currency,
                        presentation: .value,
                        semantic: .value,
                        currencyCode: currency,
                        healthState: health
                    )
                ])

                // 账户可用状态属于整个账户，仅在首组指标中输出一次。
                if index == 0 {
                    metrics.append(
                        NormalizedUsageMetric(
                            id: "availability",
                            label: "账户状态",
                            value: payload.isAvailable ? 1 : 0,
                            unit: .boolean,
                            presentation: .status,
                            semantic: .status,
                            healthState: payload.isAvailable ? health : .unavailable
                        )
                    )
                }
            }

            return UsageSnapshot(
                planName: "API 余额",
                kind: .periodic,
                fiveHour: nil,
                weekly: nil,
                token: nil,
                allowedModels: allowedModels,
                fetchedAt: now,
                providerID: .deepseek,
                credentialID: credential.credentialID,
                metrics: metrics
            )
        } catch let error as UsageProviderError {
            throw error
        } catch {
            throw UsageProviderError.invalidResponse
        }
    }

    private func fetchAllowedModels(_ credential: ProviderCredential) async throws -> [String] {
        var request = URLRequest(url: Self.modelsEndpoint)
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
            throw UsageProviderError.invalidResponse
        }
        let payload = try JSONDecoder().decode(DeepSeekModelsResponse.self, from: data)
        return payload.models
            .compactMap(\.id)
            .filter { !$0.isEmpty }
    }
}

private struct DeepSeekBalanceResponse: Decodable {
    let isAvailable: Bool
    let balanceInfos: [DeepSeekBalanceInfo]

    private enum CodingKeys: String, CodingKey {
        case isAvailable = "is_available"
        case balanceInfos = "balance_infos"
    }
}

private struct DeepSeekBalanceInfo: Decodable {
    let currency: String
    let totalBalance: String
    let grantedBalance: String
    let toppedUpBalance: String

    private enum CodingKeys: String, CodingKey {
        case currency
        case totalBalance = "total_balance"
        case grantedBalance = "granted_balance"
        case toppedUpBalance = "topped_up_balance"
    }
}

private struct DeepSeekModelsResponse: Decodable {
    let models: [DeepSeekModelIdentifier]

    private enum CodingKeys: String, CodingKey {
        case models = "data"
    }
}

private struct DeepSeekModelIdentifier: Decodable {
    let id: String?
}
