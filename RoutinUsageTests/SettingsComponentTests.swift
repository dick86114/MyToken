import AppKit
import XCTest
@testable import RoutinUsage

final class SettingsComponentTests: XCTestCase {
    func test菜单栏预览复用真实图标渲染器() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "MenuBarIndicatorPreview.swift"
        ])

        XCTAssertTrue(source.contains("MenuBarIndicatorModel.make("))
        XCTAssertTrue(source.contains("MenuBarMultiUsageIcon.image("))
        XCTAssertFalse(source.contains("appearance: NSApp.effectiveAppearance"))
        XCTAssertTrue(source.contains("let metric: NormalizedUsageMetric?"))
        XCTAssertTrue(source.contains("static func =="))
        XCTAssertFalse(source.contains("@Bindable var environment"))
    }

    func test排序控件使用本地拖拽手势() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "Components", "ReorderableCredentialCardList.swift"
        ])

        XCTAssertTrue(source.contains("struct ReorderableCredentialCardList"))
        XCTAssertTrue(source.contains("DragGesture(minimumDistance: 5"))
        XCTAssertTrue(source.contains("interactiveSpring"))
        XCTAssertTrue(source.contains("let move: (ID, Int) -> Bool"))
        XCTAssertTrue(source.contains("@State private var workingIDs"))
        XCTAssertTrue(source.contains("commitMove"))
        XCTAssertTrue(source.contains(".coordinateSpace(name: reorderableCardCoordinateSpace)"))
        XCTAssertTrue(source.contains("DragGesture(minimumDistance: 5, coordinateSpace: .named(reorderableCardCoordinateSpace))"))
        XCTAssertTrue(source.contains(".transaction { transaction in"))
        XCTAssertTrue(source.contains("transaction.animation = nil"))
        XCTAssertFalse(source.contains("CardFramePreferenceKey"))
        XCTAssertFalse(source.contains("onPreferenceChange"))
    }

    func test排序拖动上下相邻目标与补偿位移对称() {
        XCTAssertEqual(
            ReorderableCardGeometry.targetIndex(
                startIndex: 0,
                translation: 60,
                step: 100,
                count: 4
            ),
            1
        )
        XCTAssertEqual(
            ReorderableCardGeometry.targetIndex(
                startIndex: 2,
                translation: -60,
                step: 100,
                count: 4
            ),
            1
        )

        XCTAssertEqual(
            ReorderableCardGeometry.reorderedIDs([
                "A", "B", "C", "D"
            ], moving: "A", to: 1),
            ["B", "A", "C", "D"]
        )
        XCTAssertEqual(
            ReorderableCardGeometry.reorderedIDs([
                "A", "B", "C", "D"
            ], moving: "C", to: 1),
            ["A", "C", "B", "D"]
        )

        XCTAssertEqual(
            ReorderableCardGeometry.offset(
                index: 1,
                startIndex: 0,
                targetIndex: 1,
                step: 100,
                activeTranslation: nil
            ),
            -100
        )
        XCTAssertEqual(
            ReorderableCardGeometry.offset(
                index: 1,
                startIndex: 2,
                targetIndex: 1,
                step: 100,
                activeTranslation: nil
            ),
            100
        )
    }
}
