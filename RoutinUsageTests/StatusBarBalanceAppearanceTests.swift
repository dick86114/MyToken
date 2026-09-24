import AppKit
import XCTest
@testable import RoutinUsage

final class StatusBarBalanceAppearanceTests: XCTestCase {
    func test余额圆圈空心且数字使用状态色() throws {
        var rules = MenuBarColorRules.standard
        rules.normalColor = .init(red: 0.1, green: 0.9, blue: 0.3)
        let indicator = MenuBarIndicatorModel(
            shortCode: "DS",
            percent: nil,
            healthState: .normal,
            accessibilityLabel: "余额正常",
            content: .balance("99")
        )
        let bitmap = try renderedBitmap(
            MenuBarMultiUsageIcon.image(indicators: [indicator], colorRules: rules)
        )
        let circleCenter = circleCenterInBitmap(bitmap)
        let interiorSample = try XCTUnwrap(
            bitmap.colorAt(x: circleCenter.x, y: circleCenter.y - 14)
        )

        XCTAssertLessThan(interiorSample.alphaComponent, 0.1)
        XCTAssertTrue(
            containsColor(
                in: bitmap,
                within: circleTextBounds(for: bitmap),
                contains: rules.normalColor,
                tolerance: 0.05
            )
        )
    }

    func test余额圆圈与进度条统一为一像素描边() throws {
        let sourceURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/MenuBarLabelView.swift")
        let source = try String(contentsOf: sourceURL, encoding: .utf8)

        XCTAssertTrue(source.contains("indicatorStrokeWidth: CGFloat = 1"))
        XCTAssertTrue(source.contains("track.lineWidth = Self.indicatorStrokeWidth"))
        XCTAssertTrue(source.contains("circle.lineWidth = Self.indicatorStrokeWidth"))
    }

    func test余额圆圈与进度条共享边框颜色且数字保留状态色() throws {
        let sourceURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/MenuBarLabelView.swift")
        let source = try String(contentsOf: sourceURL, encoding: .utf8)
        let borderStart = try XCTUnwrap(
            source.range(of: "static var indicatorBorderColor: NSColor")
        )
        let borderEnd = try XCTUnwrap(
            source.range(of: "static let balanceUnitWidth")
        )
        let border = source[borderStart.lowerBound..<borderEnd.lowerBound]

        XCTAssertTrue(border.contains("NSColor.labelColor"))
        XCTAssertFalse(border.contains("withAlphaComponent"))
        let progressStart = try XCTUnwrap(
            source.range(of: "private static func drawProgress(")
        )
        let balanceStart = try XCTUnwrap(
            source.range(of: "private static func drawBalance(")
        )
        let balanceEnd = try XCTUnwrap(
            source.range(of: "static func highContrastTextColor")
        )
        let progress = source[progressStart.lowerBound..<balanceStart.lowerBound]
        let balance = source[balanceStart.lowerBound..<balanceEnd.lowerBound]

        XCTAssertTrue(source.contains("static var indicatorBorderColor: NSColor"))
        XCTAssertTrue(progress.contains("indicatorBorderColor.setStroke()"))
        XCTAssertTrue(balance.contains("indicatorBorderColor.setStroke()"))
        XCTAssertFalse(balance.contains("statusColor.setStroke()"))
        XCTAssertTrue(balance.contains(".foregroundColor: statusColor"))
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

    private func circleCenterInBitmap(
        _ bitmap: NSBitmapImageRep
    ) -> (x: Int, y: Int) {
        let pixelScale = Double(bitmap.pixelsWide)
            / MenuBarMultiUsageIcon.imageWidth(for: 1)
        let circleX = MenuBarMultiUsageIcon.outerPadding
            + MenuBarMultiUsageIcon.balanceUnitWidth
            - MenuBarMultiUsageIcon.balanceDiameter
            - 1.5
            + MenuBarMultiUsageIcon.balanceDiameter / 2
        let circleY = MenuBarMultiUsageIcon.size.height / 2
        return (
            Int((circleX * pixelScale).rounded()),
            Int((circleY * pixelScale).rounded())
        )
    }

    private func circleTextBounds(
        for bitmap: NSBitmapImageRep
    ) -> (xRange: ClosedRange<Int>, yRange: ClosedRange<Int>) {
        let pixelScale = Double(bitmap.pixelsWide)
            / MenuBarMultiUsageIcon.imageWidth(for: 1)
        let circleX = MenuBarMultiUsageIcon.outerPadding
            + MenuBarMultiUsageIcon.balanceUnitWidth
            - MenuBarMultiUsageIcon.balanceDiameter
            - 1.5
        let circleY = (MenuBarMultiUsageIcon.size.height
            - MenuBarMultiUsageIcon.balanceDiameter) / 2
        let inset = 4.0
        let xLower = Int(((circleX + inset) * pixelScale).rounded())
        let xUpper = Int(((circleX + MenuBarMultiUsageIcon.balanceDiameter - inset) * pixelScale).rounded())
        let yLower = Int(((circleY + inset) * pixelScale).rounded())
        let yUpper = Int(((circleY + MenuBarMultiUsageIcon.balanceDiameter - inset) * pixelScale).rounded())
        return (xLower...xUpper, yLower...yUpper)
    }

    private func containsColor(
        in bitmap: NSBitmapImageRep,
        within bounds: (xRange: ClosedRange<Int>, yRange: ClosedRange<Int>),
        contains expected: MenuBarColorComponents,
        tolerance: Double
    ) -> Bool {
        for x in bounds.xRange {
            for y in bounds.yRange {
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
