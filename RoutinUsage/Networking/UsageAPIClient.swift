import Foundation

protocol UsageFetching: Sendable {
    func fetchUsage(apiKey: String, now: Date) async throws -> UsageSnapshot?
}

enum UsageAPIError: Error, Equatable, Sendable {
    case invalidKey
    case transport
    case invalidResponse
    case server(statusCode: Int)
}

struct UsageAPIClient: UsageFetching {
    static let endpoint = URL(string: "https://api.routin.ai/plan/v1/usage")!

    let session: URLSession
    let mapper: UsageMapper

    func fetchUsage(apiKey: String, now: Date) async throws -> UsageSnapshot? {
        var request = URLRequest(url: Self.endpoint)
        request.httpMethod = "GET"
        request.timeoutInterval = 15
        request.setValue("Bearer \(apiKey)", forHTTPHeaderField: "Authorization")
        request.setValue("application/json", forHTTPHeaderField: "Accept")

        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            let foundationError = error as NSError
            if error is CancellationError
                || Task.isCancelled
                || foundationError.domain == "Swift.CancellationError" {
                throw CancellationError()
            }
            throw UsageAPIError.transport
        }

        guard let httpResponse = response as? HTTPURLResponse else {
            throw UsageAPIError.invalidResponse
        }

        guard (200..<300).contains(httpResponse.statusCode) else {
            if httpResponse.statusCode == 401, isInvalidKeyResponse(data) {
                throw UsageAPIError.invalidKey
            }
            throw UsageAPIError.server(statusCode: httpResponse.statusCode)
        }

        if String(data: data, encoding: .utf8)?
            .trimmingCharacters(in: .whitespacesAndNewlines) == "null" {
            return nil
        }

        do {
            let dto = try JSONDecoder().decode(UsageResponseDTO.self, from: data)
            return try mapper.map(dto, fetchedAt: now)
        } catch {
            throw UsageAPIError.invalidResponse
        }
    }

    private func isInvalidKeyResponse(_ data: Data) -> Bool {
        guard let response = try? JSONDecoder().decode(ErrorResponse.self, from: data) else {
            return false
        }
        return response.error == "invalid_api_key"
    }
}

private struct ErrorResponse: Decodable {
    let error: String
}
