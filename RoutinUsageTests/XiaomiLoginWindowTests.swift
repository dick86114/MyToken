import XCTest
@testable import RoutinUsage

final class XiaomiLoginWindowTests: XCTestCase {
    func test小米登录使用可聚焦和可拖动的标准窗口() throws {
        let loginSource = try TestSourceReader.read([
            "RoutinUsage", "Views", "XiaomiLoginWindow.swift",
        ])

        XCTAssertTrue(loginSource.contains("final class XiaomiLoginPanelWindow: NSWindow"))
        XCTAssertTrue(loginSource.contains("styleMask: [.titled, .closable, .resizable]"))
        XCTAssertTrue(loginSource.contains("window.makeKeyAndOrderFront(nil)"))
        XCTAssertTrue(loginSource.contains("window.makeFirstResponder(window.contentView)"))
    }

    func test小米登录入口不再使用sheet承载网页() throws {
        let environment = try TestSourceReader.read([
            "RoutinUsage", "App", "AppEnvironment.swift",
        ])
        let popover = try TestSourceReader.read([
            "RoutinUsage", "App", "StatusBarController.swift",
        ])
        let settings = try TestSourceReader.read([
            "RoutinUsage", "Views", "Settings", "CredentialManagementView.swift",
        ])
        let editor = try TestSourceReader.read([
            "RoutinUsage", "Views", "CredentialEditorView.swift",
        ])

        XCTAssertTrue(environment.contains("XiaomiLoginPanelController.shared.present"))
        XCTAssertFalse(popover.contains(".sheet(item: $environment.xiaomiLoginRequest)"))
        XCTAssertFalse(settings.contains(".sheet(item: $environment.xiaomiLoginRequest)"))
        XCTAssertFalse(editor.contains(".sheet(isPresented: $showsXiaomiLogin)"))
    }
}
