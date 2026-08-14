import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarLabelViewTests: XCTestCase {
    private func components(of color: NSColor) -> (Double, Double, Double) {
        let converted = color.usingColorSpace(NSColorSpace.deviceRGB)!
        return (converted.redComponent, converted.greenComponent, converted.blueComponent)
    }

    func test浅色外观使用黑色Logo轮廓() {
        let color = MenuBarLogoAppearance.outlineColor(
            for: NSAppearance(named: .aqua)!
        )
        let (red, green, blue) = components(of: color)

        XCTAssertEqual(red, 0, accuracy: 0.001)
        XCTAssertEqual(green, 0, accuracy: 0.001)
        XCTAssertEqual(blue, 0, accuracy: 0.001)
    }

    func test深色外观使用白色Logo轮廓() {
        let color = MenuBarLogoAppearance.outlineColor(
            for: NSAppearance(named: .darkAqua)!
        )
        let (red, green, blue) = components(of: color)

        XCTAssertEqual(red, 1, accuracy: 0.001)
        XCTAssertEqual(green, 1, accuracy: 0.001)
        XCTAssertEqual(blue, 1, accuracy: 0.001)
    }
}
