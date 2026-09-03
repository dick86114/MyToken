import AppKit
import XCTest
@testable import RoutinUsage

final class StatusBarIconRenderTests: XCTestCase {
    func test多指标图标保留用量风险颜色() throws {
        let indicators = [
            MenuBarIndicatorModel(
                shortCode: "GLM",
                percent: 100,
                healthState: .normal,
                accessibilityLabel: "GLM"
            )
        ]

        let image = MenuBarMultiUsageIcon.image(indicators: indicators)

        XCTAssertFalse(image.isTemplate)

        let tiff = try XCTUnwrap(image.tiffRepresentation)
        let bitmap = try XCTUnwrap(NSBitmapImageRep(data: tiff))
        let colors = try (0..<bitmap.pixelsWide).flatMap { x in
            try (0..<bitmap.pixelsHigh).map { y in
                try XCTUnwrap(bitmap.colorAt(x: x, y: y))
            }
        }
        let greenPixels = colors.filter {
            $0.greenComponent > $0.redComponent && $0.greenComponent > $0.blueComponent
        }
        XCTAssertFalse(greenPixels.isEmpty)
    }

    func test多指标图标生成有效图片和像素数据() throws {
        let indicators = [
            MenuBarIndicatorModel(shortCode: "ROU", percent: 40, healthState: .normal, accessibilityLabel: "Routin"),
            MenuBarIndicatorModel(shortCode: "DS", percent: nil, healthState: .warning, accessibilityLabel: "DeepSeek")
        ]
        let image = MenuBarMultiUsageIcon.image(indicators: indicators)

        XCTAssertFalse(image.isTemplate)
        XCTAssertGreaterThan(image.size.height, 0)
        let tiff = try XCTUnwrap(image.tiffRepresentation)
        let bitmap = try XCTUnwrap(NSBitmapImageRep(data: tiff))
        let byteCount = bitmap.bytesPerRow * bitmap.pixelsHigh
        XCTAssertGreaterThan(byteCount, 0)
        let hasPixel = bitmap.bitmapData.map { pointer in
            (0..<byteCount).contains { pointer[$0] != 0 }
        } ?? false
        XCTAssertTrue(hasPixel)
        XCTAssertGreaterThan(image.size.width, 0)
    }

    func test多指标图标最多绘制五个() {
        let indicators = (0..<6).map { index in
            MenuBarIndicatorModel(
                shortCode: "K\(index)",
                percent: Double(index * 15),
                healthState: .normal,
                accessibilityLabel: "指标\(index)"
            )
        }

        let image = MenuBarMultiUsageIcon.image(indicators: indicators)
        let expectedWidth = MenuBarMultiUsageIcon.unitWidth * 5
            + MenuBarMultiUsageIcon.gap * 4

        XCTAssertFalse(image.isTemplate)
        XCTAssertEqual(image.size.width, expectedWidth)
    }
}
