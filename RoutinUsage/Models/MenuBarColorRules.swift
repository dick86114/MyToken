import AppKit
import Foundation

struct MenuBarColorComponents: Codable, Equatable, Sendable {
    var red: Double
    var green: Double
    var blue: Double
    var alpha: Double

    init(red: Double, green: Double, blue: Double, alpha: Double = 1) {
        self.red = Self.clamp(red)
        self.green = Self.clamp(green)
        self.blue = Self.clamp(blue)
        self.alpha = Self.clamp(alpha)
    }

    static let standardGreen = MenuBarColorComponents(red: 0.20, green: 0.78, blue: 0.35)
    static let standardOrange = MenuBarColorComponents(red: 1.00, green: 0.62, blue: 0.04)
    static let standardRed = MenuBarColorComponents(red: 1.00, green: 0.27, blue: 0.23)

    var nsColor: NSColor {
        NSColor(srgbRed: red, green: green, blue: blue, alpha: alpha)
    }

    private static func clamp(_ value: Double) -> Double {
        guard value.isFinite else { return 0 }
        return min(max(value, 0), 1)
    }
}

struct MenuBarColorRules: Codable, Equatable, Sendable {
    var warningThreshold: Int
    var criticalThreshold: Int
    var normalColor: MenuBarColorComponents
    var warningColor: MenuBarColorComponents
    var criticalColor: MenuBarColorComponents

    init(
        warningThreshold: Int = 50,
        criticalThreshold: Int = 80,
        normalColor: MenuBarColorComponents = .standardGreen,
        warningColor: MenuBarColorComponents = .standardOrange,
        criticalColor: MenuBarColorComponents = .standardRed
    ) {
        self.warningThreshold = warningThreshold
        self.criticalThreshold = criticalThreshold
        self.normalColor = normalColor
        self.warningColor = warningColor
        self.criticalColor = criticalColor
    }

    static let standard = MenuBarColorRules()

    static func isValid(warningThreshold: Int, criticalThreshold: Int) -> Bool {
        (1...99).contains(warningThreshold)
            && (1...99).contains(criticalThreshold)
            && warningThreshold < criticalThreshold
    }

    var isValid: Bool {
        Self.isValid(warningThreshold: warningThreshold, criticalThreshold: criticalThreshold)
    }

    func color(for level: MenuBarUsageRisk) -> NSColor {
        switch level {
        case .normal: normalColor.nsColor
        case .warning: warningColor.nsColor
        case .critical: criticalColor.nsColor
        }
    }
}
