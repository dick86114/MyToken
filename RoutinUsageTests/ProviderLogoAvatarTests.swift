import AppKit
import SwiftUI
import XCTest
@testable import RoutinUsage

final class ProviderLogoAvatarTests: XCTestCase {
    private let fixedDate = Date(timeIntervalSince1970: 1_790_000_000)
    private let fixedTimeZone = TimeZone(identifier: "Asia/Shanghai")!

    func test供应商Logo映射固定到各自品牌资源() {
        let expected: [(ProviderID, String)] = [
            (.routin, "ProviderRoutinLogo"),
            (.deepseek, "ProviderDeepseekLogo"),
            (.glm, "ProviderGLMLogo"),
            (.volcengine, "ProviderVolcengineLogo"),
            (.newAPI, "ProviderNewAPILogo"),
            (.commandCode, "ProviderCommandCodeLogo"),
            (.xiaomi, "ProviderXiaomiLogo"),
            (.opencode, "ProviderOpenCodeLogo"),
        ]

        XCTAssertEqual(Set(expected.map(\.0)), Set(ProviderID.allCases))
        XCTAssertEqual(Set(expected.map(\.1)).count, ProviderID.allCases.count)
        for (providerID, assetName) in expected {
            XCTAssertEqual(providerID.logoAssetName, assetName)
            XCTAssertNotNil(NSImage(named: assetName), "缺少 \(assetName) 资源")
        }
    }

    @MainActor
    func test简洁卡片头像按供应商Logo绘制() throws {
        for scheme in [ColorScheme.light, .dark] {
            let glm = try compactAvatarSignature(
                providerID: .glm,
                colorScheme: scheme
            )
            let openCode = try compactAvatarSignature(
                providerID: .opencode,
                colorScheme: scheme
            )
            XCTAssertGreaterThan(
                meanPixelDifference(glm, openCode),
                12,
                "不同供应商应绘制不同 Logo"
            )
        }
    }

    @MainActor
    func test分享卡浅色和深色头像由供应商Logo决定而非别名首字() throws {
        for template in [UsageShareTemplate.light, .dark] {
            let aliasA = try shareAvatarPixels(
                name: "甲",
                providerID: .glm,
                template: template
            )
            let aliasB = try shareAvatarPixels(
                name: "乙",
                providerID: .glm,
                template: template
            )
            XCTAssertLessThan(
                meanPixelDifference(aliasA, aliasB),
                2,
                "同一供应商更换别名后分享头像不应变化"
            )

            let otherProvider = try shareAvatarPixels(
                name: "甲",
                providerID: .opencode,
                template: template
            )
            XCTAssertGreaterThan(
                meanPixelDifference(aliasA, otherProvider),
                12,
                "不同供应商应显示不同 Logo"
            )
        }
    }

    @MainActor
    private func compactAvatarSignature(
        providerID: ProviderID,
        colorScheme: ColorScheme
    ) throws -> Data {
        let content = CompactAccountAvatar(providerID: providerID)
            .environment(\.colorScheme, colorScheme)
            .padding(16)
            .frame(width: 60, height: 60)
            .background(
                colorScheme == .dark
                    ? Color(red: 0.043, green: 0.059, blue: 0.078)
                    : Color(red: 0.957, green: 0.961, blue: 0.969)
            )
        let renderer = ImageRenderer(content: content)
        renderer.scale = 2
        renderer.proposedSize = ProposedViewSize(width: 60, height: 60)
        let image = try XCTUnwrap(renderer.nsImage)
        return try normalizedSignature(
            from: image,
            origin: CGPoint(x: 16, y: 16),
            size: CGSize(
                width: CompactPopoverMetrics.avatarSize,
                height: CompactPopoverMetrics.avatarSize
            )
        )
    }

    @MainActor
    private func shareAvatarPixels(
        name: String,
        providerID: ProviderID,
        template: UsageShareTemplate
    ) throws -> Data {
        let state = makeState(name: name, providerID: providerID)
        let content = try XCTUnwrap(
            UsageShareContentBuilder.build(
                state: state,
                now: fixedDate,
                timeZone: fixedTimeZone
            )
        )
        var draft = UsageShareDraft.make(from: content)
        draft.template = template
        let card = UsageShareContentBuilder.render(content: content, draft: draft)
        let image = try XCTUnwrap(UsageShareExport.image(for: card))
        return try normalizedSignature(
            from: image,
            origin: CGPoint(x: 16, y: 16),
            size: CGSize(width: 32, height: 32)
        )
    }

    private func makeState(name: String, providerID: ProviderID) -> KeyUsageState {
        let snapshot = UsageSnapshot(
            planName: "Coding Plan",
            kind: .periodic,
            fiveHour: UsageMetric(
                used: 14,
                limit: 100,
                remaining: 86,
                percent: 14,
                unit: .usd,
                windowEnd: fixedDate.addingTimeInterval(3_600)
            ),
            weekly: nil,
            token: nil,
            allowedModels: [],
            fetchedAt: fixedDate,
            providerID: providerID
        )
        return KeyUsageState(
            configuration: KeyConfiguration(
                id: UUID(),
                name: name,
                keySuffix: "abcd",
                sortOrder: 0,
                providerID: providerID
            ),
            snapshot: snapshot,
            lastSuccessAt: fixedDate,
            isRefreshing: false,
            isStale: false,
            error: nil
        )
    }

    private func meanPixelDifference(_ lhs: Data, _ rhs: Data) -> Double {
        guard lhs.count == rhs.count, !lhs.isEmpty else {
            return .infinity
        }

        var difference = 0
        lhs.withUnsafeBytes { leftBytes in
            rhs.withUnsafeBytes { rightBytes in
                for index in 0..<lhs.count {
                    difference += abs(Int(leftBytes[index]) - Int(rightBytes[index]))
                }
            }
        }
        return Double(difference) / Double(lhs.count)
    }

    private func normalizedSignature(
        from image: NSImage,
        origin: CGPoint,
        size: CGSize
    ) throws -> Data {
        let cgImage = try XCTUnwrap(
            image.cgImage(forProposedRect: nil, context: nil, hints: nil)
        )
        let scaleX = CGFloat(cgImage.width) / image.size.width
        let scaleY = CGFloat(cgImage.height) / image.size.height
        let cropRect = CGRect(
            x: origin.x * scaleX,
            y: origin.y * scaleY,
            width: size.width * scaleX,
            height: size.height * scaleY
        ).integral
        let cropped = try XCTUnwrap(cgImage.cropping(to: cropRect))
        let width = 8
        let height = 8
        var normalizedPixels = Data(count: width * height * 4)
        let colorSpace = CGColorSpaceCreateDeviceRGB()
        normalizedPixels.withUnsafeMutableBytes { bytes in
            guard let context = CGContext(
                data: bytes.baseAddress,
                width: width,
                height: height,
                bitsPerComponent: 8,
                bytesPerRow: width * 4,
                space: colorSpace,
                bitmapInfo: CGImageAlphaInfo.premultipliedLast.rawValue
            ) else {
                return
            }
            context.interpolationQuality = .high
            context.draw(
                cropped,
                in: CGRect(x: 0, y: 0, width: width, height: height)
            )
        }
        return normalizedPixels
    }
}
