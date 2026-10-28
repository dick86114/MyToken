import AppKit
import XCTest
@testable import RoutinUsage

final class StatusBarBalanceAppearanceTests: XCTestCase {
    func test余额圆圈空心且数字使用短码前景色() throws {
        var rules = MenuBarColorRules.standard
        rules.normalColor = .init(red: 0.1, green: 0.9, blue: 0.3)
        let indicator = MenuBarIndicatorModel(
            shortCode: "DS",
            percent: nil,
            healthState: .normal,
            accessibilityLabel: "余额正常",
            content: .balance("99")
        )
        let darkBitmap = try renderedBitmap(
            MenuBarMultiUsageIcon.image(indicators: [indicator], colorRules: rules)
        )
        let lightBitmap = try renderedBitmap(
            MenuBarMultiUsageIcon.image(indicators: [indicator], colorRules: rules),
            appearance: try XCTUnwrap(NSAppearance(named: .aqua))
        )
        let circleCenter = circleCenterInBitmap(darkBitmap)
        let interiorSample = try XCTUnwrap(
            darkBitmap.colorAt(x: circleCenter.x, y: circleCenter.y - 14)
        )

        XCTAssertLessThan(interiorSample.alphaComponent, 0.1)
        XCTAssertTrue(
            containsColor(
                in: darkBitmap,
                within: circleTextBounds(for: darkBitmap),
                matches: { $0.greenComponent > 0.9 && $0.alphaComponent > 0.8 }
            )
        )
        XCTAssertTrue(
            containsColor(
                in: lightBitmap,
                within: circleTextBounds(for: lightBitmap),
                matches: { $0.greenComponent < 0.1 && $0.alphaComponent > 0.8 }
            )
        )
        XCTAssertFalse(
            containsColor(
                in: darkBitmap,
                within: circleTextBounds(for: darkBitmap),
                matches: { color in
                    colorMatches(color, expected: rules.normalColor, tolerance: 0.05)
                }
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

    func test余额圆圈与进度条共享显式前景色且数字使用短码前景色() throws {
        let sourceURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/MenuBarLabelView.swift")
        let source = try String(contentsOf: sourceURL, encoding: .utf8)
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

        XCTAssertTrue(progress.contains("foregroundColor.setStroke()"))
        XCTAssertTrue(balance.contains("foregroundColor.setStroke()"))
        XCTAssertFalse(progress.contains("NSColor.labelColor"))
        XCTAssertFalse(balance.contains("NSColor.labelColor"))
        XCTAssertFalse(balance.contains("statusColor.setStroke()"))
        XCTAssertFalse(balance.contains("balanceColor("))
        XCTAssertTrue(balance.contains("balanceTextAttributes("))
        XCTAssertTrue(balance.contains("color: foregroundColor"))
        XCTAssertTrue(balance.contains(".foregroundColor: color"))
        XCTAssertFalse(balance.contains(".strokeColor"))
        XCTAssertFalse(balance.contains(".strokeWidth"))
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

    private func renderedBitmap(
        _ image: NSImage,
        appearance: NSAppearance
    ) throws -> NSBitmapImageRep {
        var result: NSBitmapImageRep?
        var renderError: Error?
        appearance.performAsCurrentDrawingAppearance {
            do {
                result = try renderedBitmap(image)
            } catch {
                renderError = error
            }
        }
        if let renderError {
            throw renderError
        }
        return try XCTUnwrap(result)
    }

    private func containsColor(
        in bitmap: NSBitmapImageRep,
        within bounds: (xRange: ClosedRange<Int>, yRange: ClosedRange<Int>),
        matches predicate: (NSColor) -> Bool
    ) -> Bool {
        for x in bounds.xRange {
            for y in bounds.yRange {
                guard
                    let color = bitmap.colorAt(x: x, y: y),
                    let rgb = color.usingColorSpace(.sRGB)
                else { continue }

                if predicate(rgb) {
                    return true
                }
            }
        }
        return false
    }

    private func colorMatches(
        _ color: NSColor,
        expected: MenuBarColorComponents,
        tolerance: Double
    ) -> Bool {
        guard let rgb = color.usingColorSpace(.sRGB) else { return false }
        return abs(rgb.redComponent - expected.red) < tolerance
            && abs(rgb.greenComponent - expected.green) < tolerance
            && abs(rgb.blueComponent - expected.blue) < tolerance
            && rgb.alphaComponent > 0.9
    }
}
