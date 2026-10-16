import Foundation
import XCTest
@testable import RoutinUsage

@MainActor
final class XiaomiWebSessionProviderTests: XCTestCase {
    func test初始化时不创建WebKit会话() {
        var factoryCalls = 0
        let provider = XiaomiWebSessionProvider {
            factoryCalls += 1
            return XiaomiWebSession()
        }

        XCTAssertFalse(provider.hasSession)
        XCTAssertEqual(factoryCalls, 0)
    }

    func test登录和续期复用同一会话且可释放() {
        var factoryCalls = 0
        let provider = XiaomiWebSessionProvider {
            factoryCalls += 1
            return XiaomiWebSession()
        }

        let loginSession = provider.sessionForLogin()
        XCTAssertEqual(factoryCalls, 1)
        XCTAssertTrue(provider.hasSession)
        XCTAssertTrue(provider.existingSession === loginSession)

        let renewSession = provider.existingSession ?? provider.sessionForLogin()
        XCTAssertTrue(renewSession === loginSession)
        XCTAssertEqual(factoryCalls, 1)

        provider.releaseSession(loginSession)
        XCTAssertFalse(provider.hasSession)
        XCTAssertNil(provider.existingSession)
    }
}
