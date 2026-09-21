import AppKit
import XCTest
@testable import RoutinUsage

final class StatusBarIconRenderTests: XCTestCase {
    func test多指标图标保留彩色并由系统逐屏适配() throws {
        let indicators = [
            MenuBarIndicatorModel(
                shortCode: "GLM",
                percent: 60,
                healthState: .normal,
                accessibilityLabel: "GLM"
            )
        ]

        let image = MenuBarMultiUsageIcon.image(indicators: indicators)

        XCTAssertFalse(image.isTemplate)
        var rules = MenuBarColorRules.standard
        rules.warningColor = .init(red: 0.9, green: 0.5, blue: 0.1)
        let warningImage = MenuBarMultiUsageIcon.image(
            indicators: indicators,
            colorRules: rules
        )
        let bitmap = try renderedBitmap(warningImage)
        let hasWarningFill = try containsColor(
            in: bitmap,
            contains: MenuBarColorComponents(red: 0.9, green: 0.5, blue: 0.1),
            tolerance: 0.08
        )
        XCTAssertTrue(hasWarningFill)
    }

    func test多指标图标生成有效图片和像素数据() throws {
        let indicators = [
            MenuBarIndicatorModel(shortCode: "ROU", percent: 40, healthState: .normal, accessibilityLabel: "Routin"),
            MenuBarIndicatorModel(shortCode: "DS", percent: nil, healthState: .warning, accessibilityLabel: "DeepSeek")
        ]
        let image = MenuBarMultiUsageIcon.image(indicators: indicators)

        XCTAssertGreaterThan(image.size.height, 0)
        let bitmap = try renderedBitmap(image)
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
        let expectedWidth = MenuBarMultiUsageIcon.imageWidth(for: 5)

        XCTAssertEqual(image.size.width, expectedWidth)
    }

    private func renderedBitmap(_ image: NSImage) throws -> NSBitmapImageRep {
        let pixelScale = 2
        let bitmap = try XCTUnwrap(
            NSBitmapImageRep(
                bitmapDataPlanes: nil,
                pixelsWide: Int(image.size.width) * pixelScale,
                pixelsHigh: Int(image.size.height) * pixelScale,
                bitsPerSample: 8,
                samplesPerPixel: 4,
                hasAlpha: true,
                isPlanar: false,
                colorSpaceName: .calibratedRGB,
                bytesPerRow: 0,
                bitsPerPixel: 0
            )
        )
        bitmap.size = image.size
        let context = try XCTUnwrap(NSGraphicsContext(bitmapImageRep: bitmap))

        NSGraphicsContext.saveGraphicsState()
        NSGraphicsContext.current = context
        image.draw(
            in: NSRect(origin: .zero, size: image.size),
            from: NSRect(origin: .zero, size: image.size),
            operation: .sourceOver,
            fraction: 1
        )
        NSGraphicsContext.restoreGraphicsState()

        return bitmap
    }

    private func containsColor(
        in bitmap: NSBitmapImageRep,
        contains expected: MenuBarColorComponents,
        tolerance: Double
    ) throws -> Bool {
        for x in 0..<bitmap.pixelsWide {
            for y in 0..<bitmap.pixelsHigh {
                guard
                    let color = bitmap.colorAt(x: x, y: y),
                    let rgb = color.usingColorSpace(.sRGB)
                else { continue }

                if abs(rgb.redComponent - expected.red) < tolerance,
                   abs(rgb.greenComponent - expected.green) < tolerance,
                   abs(rgb.blueComponent - expected.blue) < tolerance,
                   rgb.alphaComponent > 0.9 {
                    return true
                }
            }
        }
        return false
    }
}
