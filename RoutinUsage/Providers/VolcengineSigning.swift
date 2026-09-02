import CryptoKit
import Foundation

struct VolcengineCredential: Sendable, Equatable {
    let accessKeyID: String
    let secretAccessKey: String
    let region: String
}

struct VolcengineRequestSigner: Sendable {
    let service: String
    let requestType: String

    init(service: String = "ark", requestType: String = "request") {
        self.service = service
        self.requestType = requestType
    }

    func sign(
        method: String,
        url: URL,
        headers: [String: String],
        body: Data,
        credential: VolcengineCredential,
        date: Date
    ) -> [String: String] {
        let timestamp = Self.timestampFormatter.string(from: date)
        let dateStamp = String(timestamp.prefix(8))
        var signedHeaders = headers
        signedHeaders["Host"] = url.host ?? ""
        signedHeaders["X-Date"] = timestamp
        signedHeaders["X-Content-Sha256"] = Self.hexDigest(body)

        let canonicalHeaders = signedHeaders
            .map { ($0.key.lowercased(), $0.value.trimmingCharacters(in: .whitespacesAndNewlines)) }
            .sorted { $0.0 < $1.0 }
            .map { "\($0.0):\($0.1)\n" }
            .joined()
        let signedHeaderNames = signedHeaders.keys.map { $0.lowercased() }.sorted().joined(separator: ";")
        let canonicalQuery = Self.canonicalQuery(url: url)
        let canonicalRequest = [
            method.uppercased(),
            url.path.isEmpty ? "/" : url.path,
            canonicalQuery,
            canonicalHeaders,
            signedHeaderNames,
            signedHeaders["X-Content-Sha256"]!
        ].joined(separator: "\n")

        let scope = "\(dateStamp)/\(credential.region)/\(service)/\(requestType)"
        let stringToSign = [
            "HMAC-SHA256",
            timestamp,
            scope,
            Self.hexDigest(Data(canonicalRequest.utf8))
        ].joined(separator: "\n")

        let kDate = Self.hmac(key: Data(("VOLC" + credential.secretAccessKey).utf8), message: dateStamp)
        let kRegion = Self.hmac(key: kDate, message: credential.region)
        let kService = Self.hmac(key: kRegion, message: service)
        let kSigning = Self.hmac(key: kService, message: requestType)
        let signature = Self.hex(Self.hmac(key: kSigning, message: stringToSign))

        signedHeaders["Authorization"] = "HMAC-SHA256 Credential=\(credential.accessKeyID)/\(scope), SignedHeaders=\(signedHeaderNames), Signature=\(signature)"
        return signedHeaders
    }

    private static let timestampFormatter: DateFormatter = {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.timeZone = TimeZone(secondsFromGMT: 0)
        formatter.dateFormat = "yyyyMMdd'T'HHmmss'Z'"
        return formatter
    }()

    private static func hmac(key: Data, message: String) -> Data {
        Data(HMAC<SHA256>.authenticationCode(for: Data(message.utf8), using: SymmetricKey(data: key)))
    }

    private static func hexDigest(_ data: Data) -> String {
        hex(Data(SHA256.hash(data: data)))
    }

    private static func hex(_ data: Data) -> String {
        data.map { String(format: "%02x", $0) }.joined()
    }

    private static func canonicalQuery(url: URL) -> String {
        guard let components = URLComponents(url: url, resolvingAgainstBaseURL: false),
              let items = components.queryItems
        else { return "" }
        let encodedItems: [(String, String)] = items.map { item in
            (Self.percentEncode(item.name), Self.percentEncode(item.value ?? ""))
        }
        let sortedItems = encodedItems.sorted { left, right in
            if left.0 == right.0 { return left.1 < right.1 }
            return left.0 < right.0
        }
        return sortedItems.map { item in
            item.0 + "=" + item.1
        }.joined(separator: "&")
    }

    private static func percentEncode(_ value: String) -> String {
        var allowed = CharacterSet.alphanumerics
        allowed.insert(charactersIn: "-_.~")
        return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
    }
}
