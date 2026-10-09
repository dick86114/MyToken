import XCTest
@testable import RoutinUsage

final class UsageShareBrandingTests: XCTestCase {
    func test分享模板默认浅色票根且浅色在前() {
        XCTAssertEqual(UsageShareTemplate.allCases, [
            .ticketLight,
            .ticket,
            .light,
            .dark,
        ])
        XCTAssertEqual(
            UsageShareDraft.make(
                from: UsageShareContent(
                    displayName: "Main",
                    providerName: "GLM",
                    planName: "Coding Plan",
                    subtitle: "GLM · Coding Plan",
                    subscriptionStartText: nil,
                    subscriptionEndText: nil,
                    expiryText: nil,
                    cycleRemainingText: nil,
                    tokenPercentText: nil,
                    groupMultiplierText: nil,
                    metrics: [],
                    capturedAt: Date(),
                    capturedAtText: "2026.10.09 14:00",
                    providerID: .glm,
                    websiteURL: URL(string: "https://open.bigmodel.cn"),
                    passCode: "PASS",
                    avatarLetter: "M",
                    isAvailable: true
                )
            ).template,
            .ticketLight
        )
    }

    func test配置官网时使用供应商品牌否则回退MyToken() {
        let configured = ProviderShareBranding.make(
            providerID: .glm,
            websiteURL: URL(string: "https://open.bigmodel.cn/")
        )
        XCTAssertEqual(configured?.logoAssetName, "ProviderGLMLogo")
        XCTAssertEqual(configured?.websiteDisplay, "https://open.bigmodel.cn")

        let fallback = ProviderShareBranding.make(providerID: .glm, websiteURL: nil)
        XCTAssertNil(fallback)
        XCTAssertEqual(ProviderShareBranding.myTokenLogoAssetName, "PopoverColorBrandLogo")
        XCTAssertEqual(
            ProviderShareBranding.myTokenWebsiteDisplay,
            "https://mytoken.idickies.cc"
        )
    }
}
