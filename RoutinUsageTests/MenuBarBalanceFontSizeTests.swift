import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarBalanceFontSizeTests: XCTestCase {
    func test余额数值按位数动态缩放且不超出圆圈() throws {
        var rules = MenuBarColorRules.standard
        rules.normalColor = .init(red: 0.1, green: 0.9, blue: 0.3)
        let texts = ["9", "12", "100", "1000"]
        var textHeights: [CGFloat] = []

        for text in texts {
            let indicator = MenuBarIndicatorModel(
                shortCode: "DS",
                percent: nil,
                healthState: .normal,
                accessibilityLabel: "余额 \(text)",
                content: .balance(text)
            )
            let image = MenuBarMultiUsageIcon.image(
                indicators: [indicator],
                colorRules: rules
            )
            let bitmap = try renderedBitmap(image)
            let statusBounds = try statusTextBounds(
                in: bitmap,
                expected: rules.normalColor
            )
            textHeights.append(statusBounds.height)

            let scale = Double(bitmap.pixelsWide)
                / MenuBarMultiUsageIcon.imageWidth(for: [indicator])
            let circleX = MenuBarMultiUsageIcon.outerPadding
                + MenuBarMultiUsageIcon.balanceUnitWidth
                - MenuBarMultiUsageIcon.balanceDiameter
                - 1.5
            let safeRect = CGRect(
                x: circleX + 1,
                y: 5,
                width: MenuBarMultiUsageIcon.balanceDiameter - 2,
                height: MenuBarMultiUsageIcon.balanceDiameter - 2
            ).scaleBy(scale)
            XCTAssertTrue(
                safeRect.contains(statusBounds),
                "\(text) 的数值边界 \(statusBounds) 超出安全区 \(safeRect)"
            )
        }

        for index in 0..<(textHeights.count - 1) {
            XCTAssertGreaterThan(
                textHeights[index],
                textHeights[index + 1],
                "字号未随位数动态缩小：\(textHeights)"
            )
        }
    }

    private func statusTextBounds(
        in bitmap: NSBitmapImageRep,
        expected: MenuBarColorComponents
    ) throws -> CGRect {
        // 与其他渲染测试保持一致的 2x 采样路径。
        var minimumX: Int?
        var maximumX: Int?
        var minimumY: Int?
        var maximumY: Int?

        for x in 0..<bitmap.pixelsWide {
            for y in 0..<bitmap.pixelsHigh {
                guard
                    let color = bitmap.colorAt(x: x, y: y),
                    colorMatches(color, expected: expected)
                else { continue }
                minimumX = min(minimumX ?? x, x)
                maximumX = max(maximumX ?? x, x)
                minimumY = min(minimumY ?? y, y)
                maximumY = max(maximumY ?? y, y)
            }
        }

        let lowerX = try XCTUnwrap(minimumX)
        let upperX = try XCTUnwrap(maximumX)
        let lowerY = try XCTUnwrap(minimumY)
        let upperY = try XCTUnwrap(maximumY)
        return CGRect(
            x: CGFloat(lowerX),
            y: CGFloat(lowerY),
            width: CGFloat(upperX - lowerX + 1),
            height: CGFloat(upperY - lowerY + 1)
        )
    }

    private func colorMatches(
        _ color: NSColor,
        expected: MenuBarColorComponents
    ) -> Bool {
        guard let rgb = color.usingColorSpace(.sRGB) else { return false }
        return abs(rgb.redComponent - expected.red) < 0.05
            && abs(rgb.greenComponent - expected.green) < 0.05
            && abs(rgb.blueComponent - expected.blue) < 0.05
            && rgb.alphaComponent > 0.7
    }
}

private extension MenuBarBalanceFontSizeTests {
    func renderedBitmap(_ image: NSImage) throws -> NSBitmapImageRep {
        let bitmap = try XCTUnwrap(
            NSBitmapImageRep(
                bitmapDataPlanes: nil,
                pixelsWide: Int(image.size.width) * 2,
                pixelsHigh: Int(image.size.height) * 2,
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
}
private extension CGRect {
    func scaleBy(_ scale: Double) -> CGRect {
        CGRect(
            x: minX * scale,
            y: minY * scale,
            width: width * scale,
            height: height * scale
        )
    }
}
