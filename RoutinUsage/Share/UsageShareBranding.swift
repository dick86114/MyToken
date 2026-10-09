import Foundation

struct ProviderShareBranding: Equatable, Sendable {
    let logoAssetName: String
    let websiteURL: URL
    let websiteDisplay: String

    static let myTokenLogoAssetName = "PopoverColorBrandLogo"
    static let myTokenWebsiteURL = URL(string: "https://mytoken.idickies.cc/")!
    static let myTokenWebsiteDisplay = "https://mytoken.idickies.cc"

    static func make(
        providerID: ProviderID,
        websiteURL: URL?
    ) -> ProviderShareBranding? {
        guard let websiteURL,
              let logoAssetName = logoAssetName(for: providerID)
        else { return nil }

        return ProviderShareBranding(
            logoAssetName: logoAssetName,
            websiteURL: websiteURL,
            websiteDisplay: websiteURL.absoluteString.hasSuffix("/")
                ? String(websiteURL.absoluteString.dropLast())
                : websiteURL.absoluteString
        )
    }

    static func logoAssetName(for providerID: ProviderID) -> String? {
        switch providerID {
        case .routin: return "ProviderRoutinLogo"
        case .deepseek: return "ProviderDeepseekLogo"
        case .glm: return "ProviderGLMLogo"
        case .volcengine: return "ProviderVolcengineLogo"
        case .newAPI: return "ProviderNewAPILogo"
        case .commandCode: return "ProviderCommandCodeLogo"
        case .xiaomi: return "ProviderXiaomiLogo"
        case .opencode: return "ProviderOpenCodeLogo"
        }
    }
}
