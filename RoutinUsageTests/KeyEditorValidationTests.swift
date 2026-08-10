import XCTest
@testable import RoutinUsage

@MainActor
final class KeyEditorValidationTests: XCTestCase {
    func test空名称返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validate(name: " \n ", secret: "plan-valid-1234")) { error in
            XCTAssertEqual(error.localizedDescription, "请输入 Key 名称")
        }
    }

    func test空Key返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validate(name: "主账号", secret: "")) { error in
            XCTAssertEqual(error.localizedDescription, "请输入 plan Key")
        }
    }

    func test非PlanKey返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validate(name: "主账号", secret: "sk-invalid")) { error in
            XCTAssertEqual(error.localizedDescription, "Key 必须以 plan- 开头")
        }
    }

    func test低阈值不小于高阈值返回中文错误() {
        XCTAssertThrowsError(try KeyEditorValidation.validateThresholds(low: 95, high: 80)) { error in
            XCTAssertEqual(error.localizedDescription, "低阈值必须小于高阈值")
        }
    }

    func test合法输入只规范化名称并保持Key原值() throws {
        let input = try KeyEditorValidation.validate(
            name: "  主账号 \n",
            secret: "plan-AbC-8F2A "
        )

        XCTAssertEqual(input.name, "主账号")
        XCTAssertEqual(input.secret, "plan-AbC-8F2A ")
    }

    func test网络验证失败保留名称和Key以便重试() async {
        let model = KeyEditorModel(name: " 主账号 ", secret: "plan-sensitive-8F2A")

        await model.save { _, _ in
            throw UsageStoreError.network
        }

        XCTAssertEqual(model.name, " 主账号 ")
        XCTAssertEqual(model.secret, "plan-sensitive-8F2A")
        XCTAssertEqual(model.errorMessage, "网络连接失败，请检查网络后重试")
        XCTAssertFalse(model.isSaving)
    }

    func testKey列表只显示固定掩码和四位尾号() {
        XCTAssertEqual(KeyDisplayMask.masked(suffix: "8F2A"), "plan-••••8F2A")
    }
}
