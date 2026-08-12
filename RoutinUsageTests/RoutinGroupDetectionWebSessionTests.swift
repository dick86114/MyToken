import Foundation
import XCTest
@testable import RoutinUsage

final class RoutinGroupDetectionWebSessionTests: XCTestCase {
    func test账户页面结果使用邮箱和昵称生成账号摘要() throws {
        let identity = try XCTUnwrap(
            RoutinGroupDetectionPageParser.accountIdentity(
                from: #"{"email":"  MEMBER@EXAMPLE.COM ","displayName":"测试账号"}"#
            )
        )

        XCTAssertEqual(identity, RoutinAccountIdentity.make(
            email: "member@example.com",
            displayName: "测试账号"
        ))
    }

    func test账户页面缺少邮箱不会退化使用昵称() {
        XCTAssertNil(
            RoutinGroupDetectionPageParser.accountIdentity(
                from: #"{"email":"","displayName":"测试账号"}"#
            )
        )
    }

    func test账户页面缺少昵称时仍使用邮箱创建关联摘要() throws {
        let identity = try XCTUnwrap(
            RoutinGroupDetectionPageParser.accountIdentity(
                from: #"{"email":"member@example.com","displayName":""}"#
            )
        )

        XCTAssertEqual(identity.displayName, "Routin 账号")
        XCTAssertEqual(
            identity.fingerprint,
            RoutinAccountIdentity.make(
                email: "member@example.com",
                displayName: "任意显示名"
            ).fingerprint
        )
    }

    func test日志结果接受包含唯一标识的UserAgent单元格并提取完整分组名() throws {
        let marker = CodexGroupProbeRequestMarker(
            id: UUID(uuidString: "00000000-0000-0000-0000-000000000123")!,
            startedAt: Date(timeIntervalSince1970: 1_786_400_000)
        )
        let rows = """
        [
          {"userAgent":"Codex Desktop","groupName":"Codex"},
          {"userAgent":"C MyRoutin-Group-Probe/00000000-0000-0000-0000-000000000123 Client","groupName":"Codex Pro"}
        ]
        """

        XCTAssertEqual(
            try RoutinGroupDetectionPageParser.groupName(from: rows, marker: marker),
            "Codex Pro"
        )
    }

    func test日志匹配不到标识时返回未找到() {
        let marker = CodexGroupProbeRequestMarker(id: UUID())

        XCTAssertThrowsError(
            try RoutinGroupDetectionPageParser.groupName(
                from: #"[{"userAgent":"Codex Desktop","groupName":"Codex"}]"#,
                marker: marker
            )
        ) { error in
            XCTAssertEqual(error as? RoutinGroupDetectionWebError, .logNotFound)
        }
    }

    func test日志有多个相同标识时拒绝猜测() {
        let marker = CodexGroupProbeRequestMarker(id: UUID())
        let rows = """
        [
          {"userAgent":"\(marker.userAgent)","groupName":"Codex"},
          {"userAgent":"\(marker.userAgent)","groupName":"Codex Pro"}
        ]
        """

        XCTAssertThrowsError(
            try RoutinGroupDetectionPageParser.groupName(from: rows, marker: marker)
        ) { error in
            XCTAssertEqual(error as? RoutinGroupDetectionWebError, .ambiguousLog)
        }
    }

    func test日志页面表格已出现时不受页面其他Loading文案影响() {
        XCTAssertTrue(
            RoutinGroupDetectionPageParser.isLogPageReady(
                bodyText: "模型请求日志\nLoading...\nHelp Center",
                hasTable: true
            )
        )
    }

    func test日志页面没有表格时仍等待加载() {
        XCTAssertFalse(
            RoutinGroupDetectionPageParser.isLogPageReady(
                bodyText: "模型请求日志\nLoading...",
                hasTable: false
            )
        )
    }
}
