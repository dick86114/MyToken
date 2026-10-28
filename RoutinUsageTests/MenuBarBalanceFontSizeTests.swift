import AppKit
import XCTest
@testable import RoutinUsage

final class MenuBarBalanceFontSizeTests: XCTestCase {
    func test余额数值按位数动态缩放且不超出圆圈() {
        let texts = ["9", "12", "100", "1000"]
        let fontSizes = texts.map { text -> CGFloat in
            let font = MenuBarMultiUsageIcon.balanceFont(for: text)
            let attributes = MenuBarMultiUsageIcon.balanceTextAttributes(
                font: font,
                color: .black
            )
            let textSize = NSString(string: text).size(withAttributes: attributes)

            XCTAssertLessThanOrEqual(
                textSize.width,
                MenuBarMultiUsageIcon.balanceTextMaximumWidth + 0.01,
                "\(text) 的数值宽度超出圆圈"
            )
            return font.pointSize
        }

        for index in 0..<(fontSizes.count - 1) {
            XCTAssertGreaterThan(
                fontSizes[index],
                fontSizes[index + 1],
                "字号未随位数动态缩小：\(fontSizes)"
            )
        }
    }
}
