import XCTest
@testable import RoutinUsage

final class RoutinWebSessionTests: XCTestCase {
    func testRoutin官方页面允许在内置网页中打开() {
        XCTAssertEqual(
            RoutinWebSession.navigationDecision(
                for: URL(string: "https://routin.ai/dashboard/lottery")!,
                approvedIdentityHosts: []
            ),
            .allowInWebView
        )
        XCTAssertEqual(
            RoutinWebSession.navigationDecision(
                for: URL(string: "https://app.routin.ai/login")!,
                approvedIdentityHosts: []
            ),
            .allowInWebView
        )
    }

    func test已验证的身份域名允许在内置网页中打开() {
        XCTAssertEqual(
            RoutinWebSession.navigationDecision(
                for: URL(string: "https://accounts.example.test/oauth")!,
                approvedIdentityHosts: ["accounts.example.test"]
            ),
            .allowInWebView
        )
    }

    func test未知外部链接交给默认浏览器() {
        XCTAssertEqual(
            RoutinWebSession.navigationDecision(
                for: URL(string: "https://example.invalid/help")!,
                approvedIdentityHosts: []
            ),
            .openExternally
        )
    }

    func test非HTTPS和伪造Routin域名会被取消() {
        XCTAssertEqual(
            RoutinWebSession.navigationDecision(
                for: URL(string: "http://routin.ai/dashboard/lottery")!,
                approvedIdentityHosts: []
            ),
            .cancel
        )
        XCTAssertEqual(
            RoutinWebSession.navigationDecision(
                for: URL(string: "https://routin.ai.evil.example/dashboard/lottery")!,
                approvedIdentityHosts: []
            ),
            .openExternally
        )
        XCTAssertEqual(
            RoutinWebSession.navigationDecision(
                for: URL(string: "file:///tmp/page.html")!,
                approvedIdentityHosts: []
            ),
            .cancel
        )
    }

    func test今日已抽页面解析为已签到() {
        XCTAssertEqual(
            RoutinWebSession.pageOutcome(
                from: "签到抽奖",
                bodyText: "每日签到抽奖\n今日已抽\n明天再来"
            ),
            .alreadyCheckedIn
        )
    }

    func test成功提示页面解析为签到成功() {
        XCTAssertEqual(
            RoutinWebSession.pageOutcome(
                from: "Daily Lottery",
                bodyText: "签到抽奖\n抽奖成功，优惠券已进入可用券列表"
            ),
            .succeeded
        )
    }

    func test未知页面不伪造结果() {
        XCTAssertNil(
            RoutinWebSession.pageOutcome(
                from: "Routin",
                bodyText: "Loading..."
            )
        )
    }
}
