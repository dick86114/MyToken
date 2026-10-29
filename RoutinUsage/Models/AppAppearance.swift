import AppKit
import Foundation

/// macOS 端应用外观；system 表示不覆盖系统外观。
enum AppAppearance: String, Codable, CaseIterable, Equatable, Sendable {
    case system
    case light
    case dark

    var title: String {
        switch self {
        case .system: return "跟随系统"
        case .light: return "浅色"
        case .dark: return "深色"
        }
    }

    var nsAppearanceName: NSAppearance.Name? {
        switch self {
        case .system: return nil
        case .light: return .aqua
        case .dark: return .darkAqua
        }
    }
}
