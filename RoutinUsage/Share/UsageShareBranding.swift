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
        guard let websiteURL else { return nil }

        return ProviderShareBranding(
            logoAssetName: logoAssetName(for: providerID),
            websiteURL: websiteURL,
            websiteDisplay: websiteURL.absoluteString.hasSuffix("/")
                ? String(websiteURL.absoluteString.dropLast())
                : websiteURL.absoluteString
        )
    }

    static func logoAssetName(for providerID: ProviderID) -> String {
        providerID.logoAssetName
    }
}
