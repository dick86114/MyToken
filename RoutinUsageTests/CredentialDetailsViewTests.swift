import XCTest
@testable import RoutinUsage

final class CredentialDetailsViewTests: XCTestCase {
    func testRoutin详情显示套餐订阅和模型信息() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "CredentialDetailsView.swift"
        ])

        XCTAssertTrue(source.contains("套餐与订阅"))
        XCTAssertTrue(source.contains("凭证与模型"))
        XCTAssertTrue(source.contains("5 小时结束"))
        XCTAssertTrue(source.contains("分组倍率"))
        XCTAssertTrue(source.contains("allowedModels"))
        XCTAssertTrue(source.contains("subscriptionStatus(snapshot.status)"))
        XCTAssertTrue(source.contains("关闭凭证详情"))
        XCTAssertTrue(source.contains("var onClose: (() -> Void)? = nil"))
        XCTAssertTrue(source.contains("case \"planType\": \"计划类型\""))
        XCTAssertTrue(source.contains("case \"websiteURL\": \"官网地址\""))
        XCTAssertTrue(source.contains("configurationRows"))
        XCTAssertTrue(source.contains("Link(destination: url)"))
        XCTAssertTrue(source.contains("ScrollView(.vertical, showsIndicators: false)"))
        XCTAssertTrue(source.contains("CredentialDetailsLayout.minWidth"))
        XCTAssertTrue(source.contains("CredentialDetailsLayout.maxWidth"))
        XCTAssertFalse(source.contains("minWidth: 720"))
        XCTAssertTrue(source.contains("statusOverview"))
        XCTAssertTrue(source.contains("usageSection"))
        XCTAssertTrue(source.contains("detailsPanel"))
        XCTAssertLessThanOrEqual(CredentialDetailsLayout.minWidth, 500)
        XCTAssertGreaterThan(
            CredentialDetailsLayout.maxWidth,
            CredentialDetailsLayout.minWidth
        )
    }

    func test详情按状态用量再套餐模型配置排序() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "CredentialDetailsView.swift"
        ])
        let status = try XCTUnwrap(source.range(of: "statusOverview"))
        let usage = try XCTUnwrap(source.range(of: "usageSection"))
        let details = try XCTUnwrap(source.range(of: "detailsPanel"))

        XCTAssertLessThan(status.lowerBound, usage.lowerBound)
        XCTAssertLessThan(usage.lowerBound, details.lowerBound)
    }

    func test火山详情显示动态订阅信息且官网链接可打开() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "CredentialDetailsView.swift"
        ])

        XCTAssertTrue(source.contains("case .routin, .volcengine, .commandCode, .xiaomi:"))
        XCTAssertTrue(source.contains("套餐与订阅"))
        XCTAssertTrue(source.contains("subscriptionStartAt"))
        XCTAssertTrue(source.contains("subscriptionEndAt"))
        XCTAssertTrue(source.contains("statusText"))
        XCTAssertTrue(source.contains("billingMode"))
        XCTAssertTrue(source.contains("接口未返回"))
        XCTAssertTrue(source.contains("truncationMode(.middle)"))
        XCTAssertTrue(source.contains("arrow.up.right"))
        XCTAssertTrue(source.contains("在浏览器中打开"))
    }

    func testGLM和DeepSeek详情显示动态模型清单() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "CredentialDetailsView.swift"
        ])

        XCTAssertTrue(source.contains("hasAccountDetails"))
        XCTAssertTrue(source.contains("accountModelsBlock"))
        XCTAssertTrue(source.contains("allowedModels"))
        XCTAssertTrue(source.contains("title: \"凭证与模型\""))
    }

    func test详情按能力隐藏不支持的旧低余额元数据() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "CredentialDetailsView.swift"
        ])

        XCTAssertTrue(
            source.contains("CredentialEditorValidation.supportsBalanceWarningThreshold(")
        )
        XCTAssertTrue(source.contains("isHiddenLegacyBalanceThreshold"))
    }

    func test详情状态使用语义色区分正常刷新失败和过期() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "CredentialDetailsView.swift"
        ])

        XCTAssertTrue(source.contains("\"数据正常\""))
        XCTAssertTrue(source.contains("\"刷新失败\""))
        XCTAssertTrue(source.contains("\"数据过期\""))
        XCTAssertTrue(source.contains("\"正在刷新\""))
        XCTAssertTrue(source.contains("return .error"))
        XCTAssertTrue(source.contains("return .warning"))
        XCTAssertTrue(source.contains(".success"))
    }

    func test模型清单使用中性胶囊和供应商强调色并支持点击复制() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "CredentialDetailsView.swift"
        ])
        let chipSource = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "Components",
            "ModelIDChipFlow.swift"
        ])

        XCTAssertTrue(source.contains("ModelIDChipFlow("))
        XCTAssertTrue(source.contains("ProviderTheme.accentColor("))
        XCTAssertTrue(source.contains("copyModelID"))
        XCTAssertFalse(source.contains("allowedModels.joined(separator: \"、\")"))
        XCTAssertTrue(chipSource.contains("WrappingModelChips"))
        XCTAssertTrue(chipSource.contains("Capsule()"))
        XCTAssertTrue(chipSource.contains("Color.primary.opacity"))
        XCTAssertTrue(chipSource.contains("NSPasteboard.general.setString(modelID, forType: .string)"))
        XCTAssertTrue(chipSource.contains("已复制模型 ID"))
        XCTAssertTrue(chipSource.contains("复制模型 ID"))
    }
}
