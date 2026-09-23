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
    func test余额单元比进度单元更宽且混合宽度按实际单元累加() {
        let progress = MenuBarIndicatorModel(
            shortCode: "GLM",
            percent: 60,
            healthState: .normal,
            accessibilityLabel: "GLM",
            content: .progress(60)
        )
        let balance = MenuBarIndicatorModel(
            shortCode: "DS",
            percent: nil,
            healthState: .normal,
            accessibilityLabel: "DeepSeek",
            content: .balance("12")
        )

        XCTAssertGreaterThan(
            MenuBarMultiUsageIcon.imageWidth(for: [balance]),
            MenuBarMultiUsageIcon.imageWidth(for: [progress])
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.imageWidth(for: [progress, balance]),
            MenuBarMultiUsageIcon.outerPadding * 2
                + MenuBarMultiUsageIcon.unitWidth
                + MenuBarMultiUsageIcon.balanceUnitWidth
                + MenuBarMultiUsageIcon.gap
        )
    }

    func test余额圆圈按健康状态使用绿红规则() throws {
        var rules = MenuBarColorRules.standard
        rules.normalColor = .init(red: 0.1, green: 0.9, blue: 0.3)
        rules.criticalColor = .init(red: 0.95, green: 0.1, blue: 0.2)
        let normal = MenuBarIndicatorModel(
            shortCode: "DS",
            percent: nil,
            healthState: .normal,
            accessibilityLabel: "余额正常",
            content: .balance("99")
        )
        let warning = MenuBarIndicatorModel(
            shortCode: "DS",
            percent: nil,
            healthState: .warning,
            accessibilityLabel: "余额偏低",
            content: .balance("1")
        )

        let normalBitmap = try renderedBitmap(
            MenuBarMultiUsageIcon.image(indicators: [normal], colorRules: rules)
        )
        let warningBitmap = try renderedBitmap(
            MenuBarMultiUsageIcon.image(indicators: [warning], colorRules: rules)
        )

        XCTAssertTrue(
            try containsColor(
                in: normalBitmap,
                contains: rules.normalColor,
                tolerance: 0.05
            )
        )
        XCTAssertTrue(
            try containsColor(
                in: warningBitmap,
                contains: rules.criticalColor,
                tolerance: 0.05
            )
        )
    }

    func test圆圈文字按背景亮度选择黑白() {
        XCTAssertEqual(
            MenuBarMultiUsageIcon.highContrastTextColor(for: .black).usingColorSpace(.sRGB),
            NSColor.white.usingColorSpace(.sRGB)
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.highContrastTextColor(for: .white).usingColorSpace(.sRGB),
            NSColor.black.usingColorSpace(.sRGB)
        )
    }
}
