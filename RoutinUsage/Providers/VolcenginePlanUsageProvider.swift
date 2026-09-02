import Foundation

struct VolcenginePlanUsageProvider: UsageProvider {
    let descriptor: ProviderDescriptor
    private let session: URLSession
    private let signer: VolcengineRequestSigner
    private let endpoint: URL

    init(
        session: URLSession = .shared,
        signer: VolcengineRequestSigner = VolcengineRequestSigner(),
        endpoint: URL = URL(string: "https://ark.cn-beijing.volcengineapi.com/")!
    ) {
        self.session = session
        self.signer = signer
        self.endpoint = endpoint
        self.descriptor = ProviderRegistry.builtInDescriptors.first(where: { $0.id == .volcengine })!
    }

    func validate(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        try await fetchUsage(credential, now: now)
    }

    func fetchUsage(_ credential: ProviderCredential, now: Date) async throws -> UsageSnapshot? {
        guard credential.providerID == .volcengine,
              credential.kind == .accessKeyPair,
              let accessKeyID = credential.metadata["accessKeyID"],
              !accessKeyID.isEmpty,
              let region = credential.metadata["region"],
              !region.isEmpty,
              !credential.secret.isEmpty,
              let planURL = Self.makeURL(
                endpoint: endpoint,
                action: credential.metadata["planType"] == "coding"
                    ? "GetCodingPlanUsage"
                    : "GetAFPUsage"
              )
        else {
            throw UsageProviderError.invalidCredential
        }

        let body = Data("{}".utf8)
        let volcengineCredential = VolcengineCredential(
            accessKeyID: accessKeyID,
            secretAccessKey: credential.secret,
            region: region
        )
        var request = URLRequest(url: planURL)
        request.httpMethod = "POST"
        request.timeoutInterval = 15
        request.httpBody = body
        var headers = [
            "Content-Type": "application/json",
            "Host": planURL.host ?? "ark.cn-beijing.volcengineapi.com"
        ]
        headers = signer.sign(
            method: "POST",
            url: planURL,
            headers: headers,
            body: body,
            credential: volcengineCredential,
            date: now
        )
        for (name, value) in headers {
            request.setValue(value, forHTTPHeaderField: name)
        }

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
            let payload = try JSONDecoder().decode(VolcengineUsageResponse.self, from: data)
            guard let result = payload.result else { throw UsageProviderError.invalidResponse }
            let windows: [(String, String, VolcengineUsageWindow?)] = [
                ("fiveHour", "5 小时 AFP", result.afpFiveHour),
                ("daily", "近 1 天 AFP", result.afpDaily),
                ("weekly", "周 AFP", result.afpWeekly),
                ("monthly", "月 AFP", result.afpMonthly)
            ]
            let metrics = windows.compactMap { id, label, window -> NormalizedUsageMetric? in
                guard let window, window.quota > 0 else { return nil }
                let percent = (window.used / window.quota) * 100
                return NormalizedUsageMetric(
                    id: id,
                    label: label,
                    used: window.used,
                    limit: window.quota,
                    remaining: max(0, window.quota - window.used),
                    unit: .request,
                    windowEnd: window.resetTime,
                    presentation: .progress,
                    healthState: percent >= 80 ? .critical : (percent >= 50 ? .warning : .normal)
                )
            }
            guard !metrics.isEmpty else { throw UsageProviderError.invalidResponse }
            let configuredPlan = credential.metadata["planType"] == "coding" ? "Coding Plan" : "Agent Plan"
            return UsageSnapshot(
                planName: result.planType.map { "\($0) Plan" } ?? configuredPlan,
                kind: .periodic,
                fiveHour: nil,
                weekly: nil,
                token: nil,
                allowedModels: [],
                fetchedAt: now,
                providerID: .volcengine,
                credentialID: credential.credentialID,
                metrics: metrics
            )
        } catch let error as UsageProviderError {
            throw error
        } catch {
            throw UsageProviderError.invalidResponse
        }
    }

    private static func makeURL(endpoint: URL, action: String) -> URL? {
        URL(string: endpoint.absoluteString + "?Action=\(action)&Version=2024-01-01")
    }
}
