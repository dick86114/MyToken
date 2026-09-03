import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarCodeLayoutTests: XCTestCase {
    func test短码字号和行距安全区内的安全边距() {
        XCTAssertEqual(
            MenuBarMultiUsageIcon.codeFont(for: 2).pointSize,
            6,
            accuracy: 0.01
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.codeFont(for: 3).pointSize,
            4.7,
            accuracy: 0.01
        )
        XCTAssertEqual(MenuBarMultiUsageIcon.codeSlotHeight(for: 2), 7, accuracy: 0.01)
        XCTAssertEqual(MenuBarMultiUsageIcon.codeSlotHeight(for: 3), 14.0 / 3.0, accuracy: 0.01)
    }

    func test短码绘制基线保留在图标安全区内() {
        let characters = Array("GLM")
        let font = MenuBarMultiUsageIcon.codeFont(for: characters.count)

        for index in characters.indices {
            let baseline = MenuBarMultiUsageIcon.codeBaselineY(
                characterCount: characters.count,
                index: index,
                font: font
            )
            let glyphTop = baseline + font.capHeight

            XCTAssertGreaterThanOrEqual(baseline, 2)
            XCTAssertLessThanOrEqual(glyphTop, 16)
        }
    }

    func testDS两个短码绘制后不超出图标安全区() throws {
        let indicator = MenuBarIndicatorModel(
            shortCode: "DS",
            percent: 32,
            healthState: .normal,
            accessibilityLabel: "DeepSeek"
        )
        let image = MenuBarMultiUsageIcon.image(
            indicators: [indicator],
            appearance: NSAppearance(named: .darkAqua)
        )
        let tiff = try XCTUnwrap(image.tiffRepresentation)
        let bitmap = try XCTUnwrap(NSBitmapImageRep(data: tiff))

        let labelPixels = try (0..<bitmap.pixelsWide).flatMap { x in
            try (0..<bitmap.pixelsHigh).compactMap { y -> NSColor? in
                let color = try XCTUnwrap(bitmap.colorAt(x: x, y: y))
                let isLabel = color.alphaComponent > 0.9
                    && color.redComponent > 0.85
                    && color.greenComponent > 0.85
                    && color.blueComponent > 0.85
                return isLabel ? color : nil
            }
        }

        XCTAssertGreaterThanOrEqual(labelPixels.count, 8)
    }
}
