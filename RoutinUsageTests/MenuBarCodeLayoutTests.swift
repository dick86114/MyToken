import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarCodeLayoutTests: XCTestCase {
    func test短码与进度条视觉中心对齐() throws {
        let indicator = MenuBarIndicatorModel(
            shortCode: "GLM",
            percent: 50,
            healthState: .normal,
            accessibilityLabel: "GLM，已使用 50%",
            content: .progress(50)
        )
        let image = MenuBarMultiUsageIcon.image(indicators: [indicator])
        let bitmap = try XCTUnwrap(
            NSBitmapImageRep(data: XCTUnwrap(image.tiffRepresentation))
        )
        let scale = Double(bitmap.pixelsWide)
            / MenuBarMultiUsageIcon.imageWidth(for: [indicator])
        let textXRange = 0..<Int(((MenuBarMultiUsageIcon.outerPadding + 8) * scale).rounded())
        let trackStart = Int(((MenuBarMultiUsageIcon.outerPadding + 9) * scale).rounded())
        let trackXRange = trackStart..<bitmap.pixelsWide
        let textCenter = try verticalCenter(
            in: bitmap,
            xRange: textXRange,
            scale: scale
        )
        let trackCenter = try verticalCenter(
            in: bitmap,
            xRange: trackXRange,
            scale: scale
        )

        XCTAssertEqual(
            textCenter,
            trackCenter,
            accuracy: 0.5,
            "短码中心 \(textCenter)，轨道中心 \(trackCenter)"
        )
    }

    private func verticalCenter(
        in bitmap: NSBitmapImageRep,
        xRange: Range<Int>,
        scale: CGFloat
    ) throws -> CGFloat {
        var minimumY: Int?
        var maximumY: Int?
        for x in xRange {
            for y in 0..<bitmap.pixelsHigh {
                guard let color = bitmap.colorAt(x: x, y: y),
                    color.alphaComponent > 0.05
                else { continue }
                minimumY = min(minimumY ?? y, y)
                maximumY = max(maximumY ?? y, y)
            }
        }
        let lower = try XCTUnwrap(minimumY)
        let upper = try XCTUnwrap(maximumY)
        return (CGFloat(lower + upper + 1) / 2) / scale
    }

    func test短码字号和行距按轨道高度重新计算() {
        XCTAssertEqual(
            MenuBarMultiUsageIcon.codeFont(for: 2).pointSize,
            8.4,
            accuracy: 0.01
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.codeFont(for: 3).pointSize,
            7,
            accuracy: 0.01
        )

        let trackHeight = MenuBarMultiUsageIcon.size.height - 8
        for characterCount in 2...3 {
            let font = MenuBarMultiUsageIcon.codeFont(for: characterCount)
            let expected = (trackHeight - font.capHeight)
                / CGFloat(characterCount - 1)
            XCTAssertEqual(
                MenuBarMultiUsageIcon.codeSlotHeight(for: characterCount),
                expected,
                accuracy: 0.01
            )
            let letterGap = expected - font.capHeight
            XCTAssertGreaterThan(letterGap, characterCount == 3 ? 0.75 : 0)
        }
        XCTAssertEqual(MenuBarMultiUsageIcon.codeSlotHeight(for: 1), 0, accuracy: 0.01)
    }

    func test短码字形高度与进度轨道对齐() {
        let trackHeight = MenuBarMultiUsageIcon.size.height - 8
        let trackTop = (MenuBarMultiUsageIcon.size.height - trackHeight) / 2
        let opticalOffset: CGFloat = 2

        for characters in [Array("D"), Array("DS"), Array("GLM")] {
            let font = MenuBarMultiUsageIcon.codeFont(for: characters.count)
            let baselines = characters.indices.map { index in
                MenuBarMultiUsageIcon.codeBaselineY(
                    characterCount: characters.count,
                    index: index,
                    font: font
                )
            }

            if characters.count == 1 {
                let expectedBaseline = trackTop + trackHeight / 2
                    + font.capHeight / 2
                    - opticalOffset
                XCTAssertEqual(baselines[0], expectedBaseline, accuracy: 0.01)
            } else {
                XCTAssertEqual(
                    baselines[0] + font.capHeight,
                    trackTop + trackHeight - opticalOffset,
                    accuracy: 0.01
                )
                XCTAssertEqual(
                    baselines[baselines.count - 1],
                    trackTop - opticalOffset,
                    accuracy: 0.01
                )
            }

            for baseline in baselines {
                XCTAssertGreaterThanOrEqual(baseline, 0)
                XCTAssertLessThanOrEqual(
                    baseline + font.capHeight,
                    MenuBarMultiUsageIcon.size.height - 2
                )
            }
        }
    }

    func test上下样式使用放大的粗体数值和固定宽度() {
        let indicator = MenuBarIndicatorModel(
            shortCode: "GLM",
            percent: 68,
            healthState: .warning,
            accessibilityLabel: "GLM，已使用 68%",
            content: .progress(68),
            style: .stacked
        )

        XCTAssertEqual(
            MenuBarMultiUsageIcon.codeFont(for: 3).pointSize,
            7,
            accuracy: 0.01
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.stackedValueFont.pointSize,
            13,
            accuracy: 0.01
        )
        XCTAssertTrue(
            NSFontManager.shared.traits(of: MenuBarMultiUsageIcon.stackedValueFont)
                .contains(.boldFontMask)
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.unitWidth(for: indicator),
            MenuBarMultiUsageIcon.stackedProgressUnitWidth,
            accuracy: 0.01
        )
    }

    func test余额数字去掉描边并恢复粗体() {
        let font = NSFont.monospacedSystemFont(ofSize: 8, weight: .bold)
        let color = NSColor.systemOrange
        let attributes = MenuBarMultiUsageIcon.balanceTextAttributes(
            font: font,
            color: color
        )
        let balanceFont = MenuBarMultiUsageIcon.balanceFont(for: "99")

        XCTAssertNil(attributes[.strokeColor])
        XCTAssertNil(attributes[.strokeWidth])
        XCTAssertEqual(attributes[.foregroundColor] as? NSColor, color)
        XCTAssertTrue(
            NSFontManager.shared.traits(of: balanceFont)
                .contains(.boldFontMask)
        )
        XCTAssertEqual(
            MenuBarMultiUsageIcon.balanceTextMaximumWidth,
            MenuBarMultiUsageIcon.balanceDiameter
                - MenuBarMultiUsageIcon.indicatorStrokeWidth
                - 4,
            accuracy: 0.01
        )
    }

    func test上下样式真实绘制为上下两行() throws {
        let indicator = MenuBarIndicatorModel(
            shortCode: "GLM",
            percent: 68,
            healthState: .warning,
            accessibilityLabel: "GLM，已使用 68%",
            content: .progress(68),
            style: .stacked
        )
        let image = MenuBarMultiUsageIcon.image(indicators: [indicator])
        let bitmap = try XCTUnwrap(
            NSBitmapImageRep(data: XCTUnwrap(image.tiffRepresentation))
        )
        let scale = CGFloat(bitmap.pixelsWide)
            / MenuBarMultiUsageIcon.imageWidth(for: [indicator])
        let rowSegments = textRowSegments(in: bitmap)

        XCTAssertEqual(rowSegments.count, 2)
        let topHeight = rowSegments[0].upperBound - rowSegments[0].lowerBound
        let bottomHeight = rowSegments[1].upperBound - rowSegments[1].lowerBound
        let gap = rowSegments[1].lowerBound - rowSegments[0].upperBound

        XCTAssertGreaterThan(
            CGFloat(bottomHeight) / scale,
            CGFloat(topHeight) / scale
        )
        XCTAssertEqual(
            CGFloat(gap) / scale,
            3,
            accuracy: 1
        )
    }

    private func textRowSegments(in bitmap: NSBitmapImageRep) -> [Range<Int>] {
        var segments: [Range<Int>] = []
        var start: Int?

        for y in 0..<bitmap.pixelsHigh {
            let hasContent = (0..<bitmap.pixelsWide).contains { x in
                guard let color = bitmap.colorAt(x: x, y: y) else { return false }
                return color.alphaComponent > 0.05
            }
            if hasContent, start == nil {
                start = y
            } else if !hasContent, let segmentStart = start {
                segments.append(segmentStart..<y)
                start = nil
            }
        }
        if let start {
            segments.append(start..<bitmap.pixelsHigh)
        }
        return segments
    }

    func test余额上下样式按货币文本宽度紧凑排布() throws {
        let indicator = MenuBarIndicatorModel(
            shortCode: "DS",
            percent: nil,
            healthState: .normal,
            accessibilityLabel: "DeepSeek，余额 12.36 元",
            balanceCurrencyText: "¥12",
            content: .balance("12"),
            style: .stacked
        )
        let image = MenuBarMultiUsageIcon.image(indicators: [indicator])
        let bitmap = try XCTUnwrap(
            NSBitmapImageRep(data: try XCTUnwrap(image.tiffRepresentation))
        )

        XCTAssertEqual(
            MenuBarMultiUsageIcon.unitWidth(for: indicator),
            MenuBarMultiUsageIcon.stackedBalanceUnitWidth(for: indicator),
            accuracy: 0.01
        )
        XCTAssertLessThan(
            MenuBarMultiUsageIcon.unitWidth(for: indicator),
            MenuBarMultiUsageIcon.stackedProgressUnitWidth
        )
        XCTAssertEqual(textRowSegments(in: bitmap).count, 2)
    }
}
