import Foundation

enum UsageCardDensity: String, Codable, Equatable, Sendable, CaseIterable {
    case compact
    case full

    var title: String {
        switch self {
        case .compact: return "简洁"
        case .full: return "完整"
        }
    }
}
