import XCTest
@testable import RoutinUsage

/// 静默续期的判定逻辑：落到交互式登录页时要立刻放弃，正常控制台页面不能误判。
final class XiaomiSilentRefreshTests: XCTestCase {
    func test交互式登录页被识别() {
        XCTAssertTrue(XiaomiWebSession.requiresInteractiveLogin(
            url: URL(string: "https://account.xiaomi.com/pass/serviceLogin?callback=https%3A%2F%2Fplatform.xiaomimimo.com")
        ))
        XCTAssertTrue(XiaomiWebSession.requiresInteractiveLogin(
            url: URL(string: "https://platform.xiaomimimo.com/login?redirect=/console/balance")
        ))
        XCTAssertTrue(XiaomiWebSession.requiresInteractiveLogin(
            url: URL(string: "https://platform.xiaomimimo.com/#/login")
        ))
    }

    func test正常控制台页面不误判() {
        XCTAssertFalse(XiaomiWebSession.requiresInteractiveLogin(
            url: URL(string: "https://platform.xiaomimimo.com/console/balance")
        ))
        XCTAssertFalse(XiaomiWebSession.requiresInteractiveLogin(
            url: URL(string: "https://platform.xiaomimimo.com/api/v1/balance")
        ))
        XCTAssertFalse(XiaomiWebSession.requiresInteractiveLogin(url: nil))
    }
}
