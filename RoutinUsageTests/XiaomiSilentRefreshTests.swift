import XCTest
@testable import RoutinUsage

/// 静默续期的判定逻辑：小米账号 SSO 中转页可能仍能自动换票，不能提前放弃；
/// 只有确认停在平台登录页或正常控制台时才应结束等待。
final class XiaomiSilentRefreshTests: XCTestCase {
    func test小米账号SSO中转页不提前终止静默续期() {
        XCTAssertFalse(XiaomiWebSession.requiresInteractiveLogin(
            url: URL(string: "https://account.xiaomi.com/pass/serviceLogin?callback=https%3A%2F%2Fplatform.xiaomimimo.com")
        ))
    }

    func test静默续期使用与控制台浏览器相同的SSO入口() throws {
        let components = try XCTUnwrap(URLComponents(
            url: XiaomiWebSession.silentRenewURL,
            resolvingAgainstBaseURL: false
        ))
        XCTAssertEqual(components.scheme, "https")
        XCTAssertEqual(components.host, "platform.xiaomimimo.com")
        XCTAssertEqual(components.path, "/api/v1/genLoginUrl")
        XCTAssertEqual(
            components.queryItems?.first(where: { $0.name == "currentPath" })?.value,
            "/console/balance"
        )
    }

    func test平台登录页仍会终止静默续期() {
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
