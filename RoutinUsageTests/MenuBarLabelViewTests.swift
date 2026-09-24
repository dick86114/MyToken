import AppKit
import XCTest
import Foundation
@testable import RoutinUsage

final class MenuBarLabelViewTests: XCTestCase {
    func test余额圆圈按整数部分缩写并省略小数() {
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(Decimal(string: "12.36")), "12")
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(Decimal(string: "999.95")), "999")
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(Decimal(string: "1500.95")), "1k")
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(Decimal(string: "20999")), "20k")
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(Decimal(string: "200999")), "200k")
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(Decimal(string: "4200000")), "4M")
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(Decimal(string: "-1500")), "-1k")
        XCTAssertEqual(MenuBarBalanceFormatter.compactText(nil), "--")
    }
    func test多指标悬停提示按凭证换行() {
        let indicators = [
            MenuBarIndicatorModel(
                shortCode: "GLM",
                percent: 27,
                healthState: .normal,
                accessibilityLabel: "GLM，武，已使用 27%"
            ),
            MenuBarIndicatorModel(
                shortCode: "DS",
                percent: nil,
                healthState: .normal,
                accessibilityLabel: "DeepSeek，亮，余额 33.19"
            )
        ]

        XCTAssertEqual(
            MenuBarIndicatorModel.hoverSummary(for: indicators),
            "GLM，武，已使用 27%\nDeepSeek，亮，余额 33.19"
        )
    }

    @MainActor
    func test多指标图标不使用系统模板() {
        XCTAssertFalse(MenuBarMultiUsageIcon.image(indicators: []).isTemplate)
    }

    func test状态和无数据指标使用独立内容类型() throws {
       let configuration = KeyConfiguration(
           id: UUID(),
           name: "测试供应商",
           keySuffix: "",
           sortOrder: 0,
           providerID: .glm,
           credentialKind: .apiKey
       )
       let descriptor = try XCTUnwrap(
           ProviderRegistry.builtInDescriptors.first(where: { $0.id == .glm })
       )
       let statusMetric = NormalizedUsageMetric(
           id: "availability",
           label: "账户状态",
           value: nil,
           unit: .boolean,
           presentation: .status,
           semantic: .status,
           healthState: .normal
       )
       let statusState = KeyUsageState(
           configuration: configuration,
           snapshot: UsageSnapshot(
               planName: "测试",
               kind: .periodic,
               fiveHour: nil,
               weekly: nil,
               token: nil,
               allowedModels: [],
               fetchedAt: .now,
               metrics: [statusMetric]
           ),
           lastSuccessAt: .now,
           isRefreshing: false,
           isStale: false,
           error: nil
       )

       XCTAssertEqual(
           MenuBarIndicatorModel.make(state: statusState, descriptor: descriptor, metric: statusMetric).content,
           .status
       )
       XCTAssertEqual(
           MenuBarIndicatorModel.make(state: statusState, descriptor: descriptor, metric: nil).content,
           .none
       )
   }
}
