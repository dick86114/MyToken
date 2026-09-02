import XCTest
@testable import RoutinUsage

final class ProviderModelsTests: XCTestCase {
    func test首期供应商描述包含简称凭证类型和能力() {
        let descriptors = ProviderRegistry.builtInDescriptors

        XCTAssertEqual(descriptors.map(\.id), [.routin, .deepseek, .glm, .volcengine])
        XCTAssertEqual(descriptors.first(where: { $0.id == .deepseek })?.shortCode, "DS")
        XCTAssertEqual(descriptors.first(where: { $0.id == .glm })?.shortCode, "GLM")
        XCTAssertEqual(descriptors.first(where: { $0.id == .volcengine })?.shortCode, "VOL")
        XCTAssertTrue(descriptors.first(where: { $0.id == .deepseek })?.capabilities.contains(.balance) == true)
        XCTAssertTrue(descriptors.first(where: { $0.id == .routin })?.capabilities.contains(.quotaWindow) == true)
    }

    func test旧KeyConfiguration默认映射为Routin凭证() throws {
        let configuration = KeyConfiguration(
            id: UUID(uuidString: "AAAAAAAA-BBBB-CCCC-DDDD-EEEEEEEEEEEE")!,
            name: "主账号",
            keySuffix: "1234",
            sortOrder: 0
        )

        let data = try JSONEncoder().encode(configuration)
        let decoded = try JSONDecoder().decode(KeyConfiguration.self, from: data)

        XCTAssertEqual(decoded.providerID, .routin)
        XCTAssertEqual(decoded.credentialKind, .bearerAPIKey)
        XCTAssertEqual(decoded.metadata, [:])
    }

    func test凭证配置编码保留供应商和非秘密元数据() throws {
        let configuration = KeyConfiguration(
            id: UUID(),
            name: "火山账号",
            keySuffix: "ABCD",
            sortOrder: 1,
            providerID: .volcengine,
            credentialKind: .accessKeyPair,
            metadata: ["region": "cn-beijing", "planType": "coding"]
        )

        let decoded = try JSONDecoder().decode(
            KeyConfiguration.self,
            from: JSONEncoder().encode(configuration)
        )

        XCTAssertEqual(decoded.providerID, .volcengine)
        XCTAssertEqual(decoded.credentialKind, .accessKeyPair)
        XCTAssertEqual(decoded.metadata["region"], "cn-beijing")
        XCTAssertEqual(decoded.metadata["planType"], "coding")
    }
}
