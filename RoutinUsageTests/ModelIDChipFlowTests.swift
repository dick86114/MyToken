import XCTest

final class ModelIDChipFlowTests: XCTestCase {
    func test模型标签使用中性表面和供应商强调色点缀() throws {
        let source = try TestSourceReader.read([
            "RoutinUsage",
            "Views",
            "Settings",
            "Components",
            "ModelIDChipFlow.swift"
        ])

        XCTAssertTrue(source.contains("accentColor: Color = .accentColor"))
        XCTAssertTrue(source.contains("Color.primary.opacity"))
        XCTAssertTrue(source.contains("accentColor.opacity"))
        XCTAssertFalse(source.contains("ModelIDChipPalette"))
        XCTAssertFalse(source.contains("colorCount"))
    }
}
