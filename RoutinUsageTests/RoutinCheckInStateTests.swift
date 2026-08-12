import XCTest
@testable import RoutinUsage

final class RoutinCheckInStateTests: XCTestCase {
    func test签到状态提供明确的中文文案() {
        XCTAssertEqual(RoutinCheckInState.idle.statusText, "尚未登录 Routin")
        XCTAssertEqual(RoutinCheckInState.needsLogin.statusText, "请先登录 Routin")
        XCTAssertEqual(RoutinCheckInState.succeeded.statusText, "签到成功")
        XCTAssertEqual(RoutinCheckInState.alreadyCheckedIn.statusText, "今天已签到")
        XCTAssertEqual(
            RoutinCheckInState.failed(.pageChanged).statusText,
            "无法确认签到结果，请在 Routin 页面中检查"
        )
    }

    func test进行中的状态会禁用重复操作() {
        XCTAssertTrue(RoutinCheckInState.loggingIn.isBusy)
        XCTAssertTrue(RoutinCheckInState.checkingIn.isBusy)
        XCTAssertFalse(RoutinCheckInState.needsLogin.isBusy)
        XCTAssertFalse(RoutinCheckInState.succeeded.isBusy)
    }

    func test终态结果可被界面保留显示() {
        XCTAssertTrue(RoutinCheckInState.succeeded.isTerminalResult)
        XCTAssertTrue(RoutinCheckInState.alreadyCheckedIn.isTerminalResult)
        XCTAssertTrue(RoutinCheckInState.failed(.network).isTerminalResult)
        XCTAssertFalse(RoutinCheckInState.checkingIn.isTerminalResult)
    }
}
