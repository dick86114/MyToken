import XCTest
@testable import RoutinUsage

final class VolcengineSigningTests: XCTestCase {
    func test固定输入生成稳定的Authorization签名() {
        let signer = VolcengineRequestSigner()
        let url = URL(string: "https://ark.test/?Action=GetAFPUsage&Version=2024-01-01")!
        let credential = VolcengineCredential(
            accessKeyID: "access-key",
            secretAccessKey: "secret-key",
            region: "cn-beijing"
        )

        let first = signer.sign(
            method: "POST",
            url: url,
            headers: ["Content-Type": "application/json"],
            body: Data("{}".utf8),
            credential: credential,
            date: Date(timeIntervalSince1970: 1_700_000_000)
        )
        let second = signer.sign(
            method: "POST",
            url: url,
            headers: ["Content-Type": "application/json"],
            body: Data("{}".utf8),
            credential: credential,
            date: Date(timeIntervalSince1970: 1_700_000_000)
        )

        XCTAssertEqual(first["Authorization"], second["Authorization"])
        XCTAssertTrue(first["Authorization"]?.contains("Credential=access-key/") == true)
        XCTAssertEqual(first["X-Content-Sha256"], second["X-Content-Sha256"])
    }
}
