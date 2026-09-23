import XCTest
@testable import RoutinUsage

final class PopoverVisualPolicyTests: XCTestCase {
    func test正常仪表使用品牌色而警示状态使用状态色() {
        XCTAssertEqual(
            PopoverVisualPolicy.gaugeAccentRole(for: .normal),
            .brand
        )
        XCTAssertEqual(
            PopoverVisualPolicy.gaugeAccentRole(for: .warning),
            .warning
        )
        XCTAssertEqual(
            PopoverVisualPolicy.gaugeAccentRole(for: .critical),
            .critical
        )
    }

    func test余额徽章按健康状态映射语义色() {
        XCTAssertEqual(
            PopoverVisualPolicy.balanceAccentRole(for: .normal),
            .positive
        )
        XCTAssertEqual(
            PopoverVisualPolicy.balanceAccentRole(for: .warning),
            .critical
        )
        XCTAssertEqual(
            PopoverVisualPolicy.balanceAccentRole(for: .critical),
            .critical
        )
        XCTAssertEqual(
            PopoverVisualPolicy.balanceAccentRole(for: .unavailable),
            .critical
        )
        XCTAssertEqual(
            PopoverVisualPolicy.balanceAccentRole(for: .stale),
            .critical
        )
        XCTAssertEqual(
            PopoverVisualPolicy.balanceAccentRole(for: .unknown),
            .secondary
        )
    }

    func test健康状态只映射到四种语义色() {
        XCTAssertEqual(
            PopoverVisualPolicy.healthAccentRole(for: .normal),
            .brand
        )
        XCTAssertEqual(
            PopoverVisualPolicy.healthAccentRole(for: .warning),
            .warning
        )
        XCTAssertEqual(
            PopoverVisualPolicy.healthAccentRole(for: .critical),
            .critical
        )
        XCTAssertEqual(
            PopoverVisualPolicy.healthAccentRole(for: .unavailable),
            .critical
        )
        XCTAssertEqual(
            PopoverVisualPolicy.healthAccentRole(for: .stale),
            .secondary
        )
        XCTAssertEqual(
            PopoverVisualPolicy.healthAccentRole(for: .unknown),
            .secondary
        )
    }

    func test材质只在窗口和模态使用玻璃() {
        XCTAssertEqual(
            PopoverVisualPolicy.material(for: .window),
            .windowGlass
        )
        XCTAssertEqual(
            PopoverVisualPolicy.material(for: .card),
            .solid
        )
        XCTAssertEqual(
            PopoverVisualPolicy.material(for: .metric),
            .solid
        )
        XCTAssertEqual(
            PopoverVisualPolicy.material(for: .modal),
            .modalGlass
        )
    }

    func test只有模态表面允许独立阴影() {
        XCTAssertFalse(PopoverVisualPolicy.allowsShadow(for: .window))
        XCTAssertFalse(PopoverVisualPolicy.allowsShadow(for: .card))
        XCTAssertFalse(PopoverVisualPolicy.allowsShadow(for: .metric))
        XCTAssertFalse(PopoverVisualPolicy.allowsShadow(for: .control))
        XCTAssertTrue(PopoverVisualPolicy.allowsShadow(for: .modal))
    }

    func test圆角层级保持外层内层和按钮三级() {
        XCTAssertEqual(PopoverVisualPolicy.cornerRadius(for: .outer), 14)
        XCTAssertEqual(PopoverVisualPolicy.cornerRadius(for: .inner), 10)
        XCTAssertEqual(PopoverVisualPolicy.cornerRadius(for: .button), 10)
    }
}
