import Foundation

protocol CodexGroupProbing: Sendable {
    func probe(apiKey: String, marker: CodexGroupProbeRequestMarker) async throws
}

struct CodexGroupProbeRequestMarker: Equatable, Sendable {
    let id: UUID
    let startedAt: Date

    init(id: UUID = UUID(), startedAt: Date = .now) {
        self.id = id
        self.startedAt = startedAt
    }

    var userAgent: String {
        "MyToken-Group-Probe/\(id.uuidString)"
    }
}

enum CodexGroupProbeError: Error, Equatable, Sendable {
    case invalidKey
    case modelUnavailable
    case timeout
    case secureConnection
    case network
    case invalidResponse
    case server(statusCode: Int)
}

struct CodexGroupProbeClient: CodexGroupProbing {
    static let endpoint = URL(string: "https://api.routin.ai/plan/v1/responses")!
    static let model = "gpt-5.6-luna"
    static let timeout: TimeInterval = 45

    let session: URLSession
    let logWriter: any AppLogWriting

    init(
        session: URLSession,
        logWriter: any AppLogWriting = NoopAppLogWriter()
    ) {
        self.session = session
        self.logWriter = logWriter
    }

    func probe(apiKey: String, marker: CodexGroupProbeRequestMarker) async throws {
        var request = URLRequest(url: Self.endpoint)
        request.httpMethod = "POST"
        request.timeoutInterval = Self.timeout
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue(marker.userAgent, forHTTPHeaderField: "User-Agent")
        request.setValue("application/json", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.httpBody = try JSONSerialization.data(withJSONObject: [
            "model": Self.model,
            "input": "返回 OK",
            "max_output_tokens": 16
        ])

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            if error is CancellationError || Task.isCancelled {
                throw CancellationError()
            }
            let foundationError = error as NSError
            await logWriter.log(
                level: .warning,
                event: "codex_group_probe_transport_failed",
                details: "domain=\(foundationError.domain) code=\(foundationError.code)"
            )
            if let error = error as? URLError, error.code == .timedOut {
                throw CodexGroupProbeError.timeout
            }
            if let error = error as? URLError, error.code == .secureConnectionFailed {
                throw CodexGroupProbeError.secureConnection
            }
            throw CodexGroupProbeError.network
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw CodexGroupProbeError.invalidResponse
        }
        guard (200..<300).contains(httpResponse.statusCode) else {
            if httpResponse.statusCode == 401 {
                throw CodexGroupProbeError.invalidKey
            }
            if httpResponse.statusCode == 404, isModelUnavailable(data) {
                throw CodexGroupProbeError.modelUnavailable
            }
            throw CodexGroupProbeError.server(statusCode: httpResponse.statusCode)
        }
    }

    private func isModelUnavailable(_ data: Data) -> Bool {
        guard let text = String(data: data, encoding: .utf8)?.lowercased() else {
            return false
        }
        return text.contains("model_not_found") || text.contains("model unavailable")
    }
}
