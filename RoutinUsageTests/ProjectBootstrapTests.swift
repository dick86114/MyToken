import AppKit
import XCTest
@testable import RoutinUsage

final class ProjectBootstrapTests: XCTestCase {
    func test应用标识稳定() {
        XCTAssertEqual(RoutinUsageApp.applicationName, "MyToken")
    }

    func test正式应用使用新的状态栏身份并保留旧偏好迁移() throws {
        let project = try sourceText(at: "project.yml")
        let environment = try sourceText(at: "RoutinUsage/App/AppEnvironment.swift")

        XCTAssertTrue(project.contains("PRODUCT_BUNDLE_IDENTIFIER: cc.idickies.mytoken"))
        XCTAssertTrue(environment.contains("UserDefaultsMigration.migrateCompatiblePreferences()"))
    }

    func test应用提供版本号与官网和GitHub地址() throws {
        XCTAssertFalse(RoutinUsageApp.currentVersion.isEmpty)
        XCTAssertEqual(RoutinUsageApp.websiteURL.absoluteString, "https://mytoken.idickies.cc")
        XCTAssertEqual(
            RoutinUsageApp.githubURL.absoluteString,
            "https://github.com/dick86114/MyToken"
        )
        XCTAssertEqual(
            RoutinUsageApp.releasesURL.absoluteString,
            "https://github.com/dick86114/MyToken/releases"
        )
    }

    func test菜单栏弹窗左侧版本号可点击并链接到Releases() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let app = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertTrue(app.contains("nonisolated static var currentVersion"))
        XCTAssertTrue(app.contains("nonisolated static let githubURL"))
        XCTAssertTrue(app.contains("nonisolated static let releasesURL"))
        XCTAssertTrue(popover.contains("Link(destination: RoutinUsageApp.releasesURL)"))
        XCTAssertTrue(popover.contains("RoutinUsageApp.currentVersion"))
        XCTAssertTrue(popover.contains("打开 Releases 页面"))
        XCTAssertFalse(popover.contains(".underline()"))
    }

    func test菜单栏弹窗顶部使用左版本中彩色透明Logo右刷新布局() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let app = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let assetURL = projectRoot.appendingPathComponent(
            "RoutinUsage/Assets.xcassets/PopoverColorBrandLogo.imageset/Contents.json"
        )
        let asset = try JSONSerialization.jsonObject(with: Data(contentsOf: assetURL))
        let assetObject = try XCTUnwrap(asset as? [String: Any])
        let images = try XCTUnwrap(assetObject["images"] as? [[String: Any]])

        XCTAssertTrue(app.contains("nonisolated static let websiteURL"))
        XCTAssertTrue(popover.contains("Link(destination: RoutinUsageApp.websiteURL)"))
        XCTAssertTrue(popover.contains("Image(nsImage: NSImage(named: \"PopoverColorBrandLogo\")"))
        let logoStart = try XCTUnwrap(popover.range(of: "private var popoverBrandLogo"))
        let logoEnd = try XCTUnwrap(
            popover.range(
                of: "var toolbar",
                range: logoStart.lowerBound..<popover.endIndex
            )
        )
        let logoSource = String(popover[logoStart.lowerBound..<logoEnd.lowerBound])
        XCTAssertTrue(logoSource.contains("frame(width: 36, height: 36)"))
        XCTAssertFalse(logoSource.contains("frame(width: 28, height: 28)"))
        XCTAssertFalse(logoSource.contains("clipShape"))
        XCTAssertFalse(logoSource.contains("logoBacking"))
        XCTAssertFalse(logoSource.contains("strokeBorder"))

        func appearanceValue(_ image: [String: Any]) -> String? {
            let appearances = image["appearances"] as? [[String: Any]]
            return appearances?.first?["value"] as? String
        }
        let lightScales = images
            .filter { appearanceValue($0) == "light" }
            .compactMap { $0["scale"] as? String }
        let darkScales = images
            .filter { appearanceValue($0) == "dark" }
            .compactMap { $0["scale"] as? String }
        XCTAssertEqual(Set(lightScales), ["1x", "2x"])
        XCTAssertEqual(Set(darkScales), ["1x", "2x"])
        for filename in [
            "popover-color-brand-logo-light.png",
            "popover-color-brand-logo-light@2x.png",
            "popover-color-brand-logo-dark.png",
            "popover-color-brand-logo-dark@2x.png"
        ] {
            XCTAssertTrue(FileManager.default.fileExists(
                atPath: projectRoot
                    .appendingPathComponent("RoutinUsage/Assets.xcassets/PopoverColorBrandLogo.imageset")
                    .appendingPathComponent(filename)
                    .path
            ))
        }

        func centerColor(_ filename: String) throws -> NSColor {
            let url = projectRoot
                .appendingPathComponent("RoutinUsage/Assets.xcassets/PopoverColorBrandLogo.imageset")
                .appendingPathComponent(filename)
            let rep = try XCTUnwrap(NSBitmapImageRep(data: try Data(contentsOf: url)))
            return try XCTUnwrap(rep.colorAt(x: 42, y: 42))
        }

        let lightCenter = try centerColor("popover-color-brand-logo-light@2x.png")
        let darkCenter = try centerColor("popover-color-brand-logo-dark@2x.png")
        XCTAssertGreaterThan(lightCenter.alphaComponent, 0.9)
        XCTAssertLessThan((lightCenter.redComponent + lightCenter.greenComponent + lightCenter.blueComponent) / 3, 0.25)
        XCTAssertGreaterThan(darkCenter.alphaComponent, 0.9)
        XCTAssertGreaterThan((darkCenter.redComponent + darkCenter.greenComponent + darkCenter.blueComponent) / 3, 0.75)

        XCTAssertFalse(popover.contains(".shadow(color: Color.black.opacity(0.25)"))
        XCTAssertTrue(popover.contains("repeatForever"))
        XCTAssertTrue(popover.contains("打开 MyToken 官网"))
        XCTAssertTrue(popover.contains(".overlay(alignment: .center)"))
        XCTAssertTrue(popover.contains("arrow.triangle.2.circlepath"))
        XCTAssertTrue(popover.contains("arrow.clockwise"))
        XCTAssertTrue(popover.contains("showRefreshSuccess"))
        XCTAssertTrue(popover.contains("刷新全部 Key"))
        XCTAssertFalse(popover.contains("if hasRoutinAccount"))
        XCTAssertFalse(popover.contains("checkInHelpText"))
        XCTAssertFalse(popover.contains("Picker(\"用量周期\""))
    }

    func test应用图标使用已归档的MyToken品牌原图生成完整尺寸集() throws {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let brandLogo = projectRoot
            .appendingPathComponent("docs/brand-assets/mytoken-brand-logo.png")
        let appIconDirectory = projectRoot
            .appendingPathComponent("RoutinUsage/Assets.xcassets/AppIcon.appiconset")
        let expectedIconNames = [
            "icon_16x16.png",
            "icon_16x16@2x.png",
            "icon_32x32.png",
            "icon_32x32@2x.png",
            "icon_128x128.png",
            "icon_128x128@2x.png",
            "icon_256x256.png",
            "icon_256x256@2x.png",
            "icon_512x512.png",
            "icon_512x512@2x.png"
        ]

        XCTAssertTrue(FileManager.default.fileExists(atPath: brandLogo.path))
        for iconName in expectedIconNames {
            XCTAssertTrue(
                FileManager.default.fileExists(
                    atPath: appIconDirectory.appendingPathComponent(iconName).path
                )
            )
        }
    }

    func test菜单栏Logo原图使用最新品牌资源() throws {
        let menuBarLogo = try XCTUnwrap(
            Bundle(for: ProjectBootstrapTests.self).url(
                forResource: "routin-menu-bar-logo-source",
                withExtension: "png"
            )
        )
        let image = try XCTUnwrap(NSImage(contentsOf: menuBarLogo))

        XCTAssertEqual(image.size, NSSize(width: 1675, height: 1782))
    }

    func test菜单栏弹窗同屏展示五小时与周用量() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let row = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertFalse(popover.contains("dimension: settings.displayDimension"))
        XCTAssertTrue(row.contains("periodicContent"))
        XCTAssertTrue(row.contains("title: \"5 小时\""))
        XCTAssertTrue(row.contains("title: \"周\""))
    }

    func test菜单栏使用原生状态栏按钮承载左右键交互() throws {
        let source = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertTrue(source.contains("StatusBarController(environment: environment)"))
        XCTAssertTrue(source.contains("@State private var statusBarController"))
        XCTAssertFalse(source.contains("MenuBarExtra"))
    }

    func test真实菜单栏和弹窗读取独立展示顺序() throws {
        let controller = try TestSourceReader.read([
            "RoutinUsage", "App", "StatusBarController.swift"
        ])
        let popover = try TestSourceReader.read([
            "RoutinUsage", "Views", "UsagePopoverView.swift"
        ])

        XCTAssertTrue(controller.contains("displayOrder.visible(enabledIDs:"))
        XCTAssertTrue(controller.contains("visibility.menuBarIDs"))
        XCTAssertTrue(popover.contains("displayOrder.visible(enabledIDs:"))
        XCTAssertTrue(popover.contains("visibility.popoverIDs"))
        XCTAssertFalse(popover.contains("LegacyCredentialDisplayOrder.popoverIDs"))
        XCTAssertFalse(popover.contains("settings.availableCredentialIDs"))
    }

    func test凭证管理页复用弹窗卡片并支持详情() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/CredentialManagementView.swift")

        XCTAssertTrue(settings.contains("UsageRowView("))
        XCTAssertTrue(settings.contains("LazyVGrid"))
        XCTAssertTrue(settings.contains("CredentialDetailsView"))
    }

    func test状态栏控制器在应用启动完成后异步安装() throws {
        let source = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertTrue(source.contains("applicationDidFinishLaunching"))
        XCTAssertTrue(source.contains("NSApp.setActivationPolicy(.accessory)"))
        XCTAssertTrue(source.contains("SettingsWindowActivationPolicy.refresh()"))
        XCTAssertTrue(source.contains("let controller = StatusBarController(environment: environment)"))
        XCTAssertTrue(source.contains("retainedStatusBarController = controller"))
        XCTAssertTrue(source.contains("controller.start()"))

        let installRange = try XCTUnwrap(
            source.range(of: "controller.start()")
        )
        let refreshRange = try XCTUnwrap(
            source.range(of: "SettingsWindowActivationPolicy.refresh()")
        )
        XCTAssertLessThan(refreshRange.lowerBound, installRange.lowerBound)
    }

    func testDebug使用独立BundleID避免复用系统菜单栏状态() throws {
        let project = try sourceText(at: "project.yml")
        let testScript = try sourceText(at: "scripts/test.sh")

        XCTAssertTrue(project.contains("PRODUCT_BUNDLE_IDENTIFIER: ai.routin.mytoken.debug.v3"))
        XCTAssertTrue(testScript.contains("PRODUCT_BUNDLE_IDENTIFIER=ai.routin.mytoken.tests"))
    }

    func test关闭最后一个设置窗口后应用仍驻留菜单栏() throws {
        let source = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertTrue(source.contains("@NSApplicationDelegateAdaptor(RoutinUsageAppDelegate.self)"))
        XCTAssertTrue(source.contains("applicationShouldTerminateAfterLastWindowClosed"))
        XCTAssertTrue(source.contains("return false"))
    }

    func test启动阶段会关闭旧登录和签到窗口() throws {
        let app = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")
        let environment = try sourceText(at: "RoutinUsage/App/AppEnvironment.swift")

        XCTAssertTrue(app.contains("closeLegacySuppressedLaunchWindows()"))
        XCTAssertTrue(app.contains("登录小米 MiMo"))
        XCTAssertTrue(environment.contains("isStartupRefreshActive"))
        XCTAssertTrue(environment.contains("guard !isStartupRefreshActive else { return }"))
    }

    func test菜单栏标签提供应用操作菜单但不提供账号选择() throws {
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(statusBarController.contains("button.sendAction(on: [.leftMouseUp, .rightMouseUp])"))
        XCTAssertTrue(statusBarController.contains("button.imagePosition = .imageRight"))
        XCTAssertTrue(statusBarController.contains("NSApp.currentEvent?.type == .rightMouseUp"))
        XCTAssertFalse(statusBarController.contains("切换账号"))
        XCTAssertFalse(statusBarController.contains("environment.store.selectKey(id)"))
        XCTAssertFalse(statusBarController.contains("selectedKeyID"))
        XCTAssertTrue(statusBarController.contains("设置"))
        XCTAssertTrue(statusBarController.contains("private var settingsWindow: NSWindow?"))
        XCTAssertTrue(statusBarController.contains("检查更新"))
        XCTAssertTrue(statusBarController.contains("NSApplication.shared.terminate(nil)"))
    }

    func test菜单栏右键菜单交由系统状态项定位并使用常规展示样式() throws {
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(statusBarController.contains("menu.presentationStyle = .regular"))
        XCTAssertTrue(statusBarController.contains("statusItem.menu = menu"))
        XCTAssertTrue(statusBarController.contains("button.performClick(nil)"))
        XCTAssertTrue(statusBarController.contains("statusItem.menu = nil"))
        XCTAssertFalse(statusBarController.contains("menu.popUp(positioning: nil"))
    }

    func test菜单栏和设置页提供问题提交入口() throws {
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/HelpUpdateView.swift")

        XCTAssertTrue(statusBarController.contains("提交问题"))
        XCTAssertTrue(statusBarController.contains("openIssueReport"))
        XCTAssertTrue(settings.contains("提交问题"))
        XCTAssertTrue(settings.contains("openIssueReport"))
    }

    func test菜单栏弹窗显示时会激活应用并获取焦点() throws {
        let source = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(source.contains("NSApp.activate(ignoringOtherApps: true)"))
        XCTAssertTrue(source.contains("window.makeKeyAndOrderFront(nil)"))
    }

    func test空配置点击菜单栏直接打开设置且不嵌套引导() throws {
        let source = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(source.contains("guard !environment.store.orderedKeyIDs.isEmpty else {"))
        XCTAssertTrue(source.contains("openSettingsWindow()"))
        XCTAssertTrue(source.contains("window.makeKeyAndOrderFront(nil)"))
        XCTAssertFalse(source.contains(".sheet(isPresented: $environment.showsOnboarding)"))
    }

    @MainActor
    func test未配置菜单栏使用黑白图标并跟随菜单栏反色() throws {
        let source = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")
        let menuBarLabelURL = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
            .appendingPathComponent("RoutinUsage/Views/MenuBarLabelView.swift")
        let menuBarLabel = try String(contentsOf: menuBarLabelURL, encoding: .utf8)
        let logoStart = try XCTUnwrap(menuBarLabel.range(of: "enum MenuBarMonoBrandLogo"))
        let logoEnd = try XCTUnwrap(
            menuBarLabel.range(
                of: "enum MenuBarLogoUsageIcon",
                range: logoStart.lowerBound..<menuBarLabel.endIndex
            )
        )
        let logoSource = String(menuBarLabel[logoStart.lowerBound..<logoEnd.lowerBound])

        XCTAssertTrue(source.contains("button.image = MenuBarMonoBrandLogo.image()"))
        XCTAssertFalse(source.contains("MenuBarMonoBrandLogo.image(appearance:"))
        XCTAssertFalse(source.contains("observeStatusBarAppearance()"))
        XCTAssertFalse(source.contains("scheduleStatusButtonUpdate()"))
        XCTAssertTrue(logoSource.contains("NSImage(size: size, flipped: false)"))
        XCTAssertTrue(logoSource.contains("NSColor.labelColor.setFill()"))
        XCTAssertFalse(logoSource.contains("bestMatch(from: [.aqua, .darkAqua])"))
        XCTAssertFalse(logoSource.contains("performAsCurrentDrawingAppearance"))
        XCTAssertFalse(source.contains("button.image?.isTemplate = true"))
    }

    func test未配置菜单栏黑白图标资源包含明暗外观() throws {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let contentsURL = projectRoot.appendingPathComponent(
            "RoutinUsage/Assets.xcassets/MenuBarMonoBrandLogo.imageset/Contents.json"
        )
        let contents = try JSONSerialization.jsonObject(with: Data(contentsOf: contentsURL))
        let contentsObject = try XCTUnwrap(contents as? [String: Any])
        let images = try XCTUnwrap(contentsObject["images"] as? [[String: Any]])

        func appearanceValue(_ image: [String: Any]) -> String? {
            let appearances = image["appearances"] as? [[String: Any]]
            return appearances?.first?["value"] as? String
        }
        let lightScales = images
            .filter { appearanceValue($0) == "light" }
            .compactMap { $0["scale"] as? String }
        let darkScales = images
            .filter { appearanceValue($0) == "dark" }
            .compactMap { $0["scale"] as? String }
        XCTAssertEqual(Set(lightScales), ["1x", "2x"])
        XCTAssertEqual(Set(darkScales), ["1x", "2x"])
        for filename in [
            "menu-bar-mono-brand-logo-light.png",
            "menu-bar-mono-brand-logo-light@2x.png",
            "menu-bar-mono-brand-logo-dark.png",
            "menu-bar-mono-brand-logo-dark@2x.png"
        ] {
            XCTAssertTrue(FileManager.default.fileExists(
                atPath: projectRoot
                    .appendingPathComponent("RoutinUsage/Assets.xcassets/MenuBarMonoBrandLogo.imageset")
                    .appendingPathComponent(filename)
                    .path
            ))
        }
    }

    func test更新完成提示不会阻塞首次启动检查() throws {
        let source = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")
        let start = try XCTUnwrap(source.range(of: "await self.environment.start()"))
        let notice = try XCTUnwrap(source.range(of: "self.environment.presentUpdateCompletionNoticeIfNeeded()"))

        XCTAssertLessThan(start.lowerBound, notice.lowerBound)
    }

    func test弹窗进度条复用共享用量风险呈现() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(usageRowView.contains("UsageMetricProgressBar(metric: metric)"))
        XCTAssertTrue(usageRowView.contains("rules: menuBarColorRules"))
    }

    func test凭证刷新使用边框动效且失败改用悬浮标识() throws {
        let row = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(row.contains("RefreshingCardBorder"))
        XCTAssertTrue(row.contains("TimelineView(.animation"))
        XCTAssertTrue(row.contains("refreshFailureIndicator"))
        XCTAssertTrue(row.contains("avatarWithStatus"))
        XCTAssertTrue(row.contains("Image(systemName: \"exclamationmark\")"))
        XCTAssertTrue(row.contains("RefreshFailurePopover"))
        XCTAssertTrue(row.contains("UsageFormatter.refreshFailureTooltip"))
        XCTAssertFalse(row.contains("clock.badge.exclamationmark"))
    }

    func test菜单栏弹窗不再提供Key选中交互() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let row = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(popover.contains("usageListHeader"))
        XCTAssertTrue(popover.contains("账户用量"))
        XCTAssertFalse(popover.contains("当前账户"))
        XCTAssertFalse(popover.contains("isSelectedRoutin"))
        XCTAssertFalse(row.contains("store.selectedKeyID"))
        XCTAssertFalse(row.contains("store.selectKey"))
        XCTAssertFalse(row.contains("当前账户"))
    }

    func test设置使用状态栏控制器直管的可缩放窗口() throws {
        let source = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(source.contains("private var settingsWindow: NSWindow?"))
        XCTAssertTrue(source.contains("styleMask: [.titled, .closable, .miniaturizable, .resizable]"))
        XCTAssertTrue(source.contains("window.styleMask.remove(.fullSizeContentView)"))
        XCTAssertTrue(source.contains("WindowFramePersistence.loadSize()"))
        XCTAssertTrue(source.contains("window.minSize = WindowFramePersistence.minimumSize"))
    }

    func test引导页统一使用五小时产品文案且设置页不再硬编码维度() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/GeneralSettingsView.swift")
        let onboarding = try sourceText(at: "RoutinUsage/Views/OnboardingView.swift")

        XCTAssertFalse(settings.contains("五小时"))
        XCTAssertFalse(onboarding.contains("五小时"))
        XCTAssertTrue(onboarding.contains("5 小时"))
    }

    func test设置窗口使用窗口代理且编辑器保留查看状态() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/SettingsWindowView.swift")
        let keyEditor = try sourceText(at: "RoutinUsage/Views/KeyEditorView.swift")

        XCTAssertTrue(settings.contains("WindowFramePersistence"))
        XCTAssertFalse(settings.contains("@State private var revealedKeyIDs"))
        XCTAssertTrue(keyEditor.contains("@State private var isSecretVisible"))
        XCTAssertTrue(keyEditor.contains("TextField(\"plan-…\""))
        XCTAssertTrue(keyEditor.contains("CredentialVisibility.iconName(isVisible: isSecretVisible)"))
    }

    func test设置页查看状态不从UserDefaults读取() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/SettingsWindowView.swift")
        let credentials = try sourceText(at: "RoutinUsage/Views/Settings/CredentialManagementView.swift")

        XCTAssertFalse(settings.contains("UserDefaults"))
        XCTAssertFalse(credentials.contains("UserDefaults"))
    }

    func test首次引导使用MyToken品牌图并聚焦添加首个Key() throws {
        let onboarding = try sourceText(at: "RoutinUsage/Views/OnboardingView.swift")

        XCTAssertTrue(onboarding.contains("PopoverColorBrandLogo"))
        XCTAssertTrue(onboarding.contains("欢迎使用 MyToken"))
        XCTAssertFalse(onboarding.contains("chart.bar.xaxis"))
        XCTAssertTrue(onboarding.contains("添加第一个 Key"))
    }

    func test菜单栏视图真实接入每凭证指标解析() throws {
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(statusBarController.contains("usagePreferences(for: id)"))
        XCTAssertTrue(statusBarController.contains("MenuBarMetricResolver.resolve"))
        XCTAssertTrue(statusBarController.contains("metric: resolution.metric"))
        XCTAssertFalse(statusBarController.contains("settings.displayDimension"))
    }

    func test弹窗详情保留两个周期并显示完整重置时间() throws {
        let row = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")
        let metricGrid = try sourceText(at: "RoutinUsage/Views/NormalizedUsageMetricGrid.swift")

        XCTAssertTrue(row.contains("title: \"5 小时\""))
        XCTAssertTrue(row.contains("title: \"周\""))
        XCTAssertTrue(metricGrid.contains("UsageFormatter.fullDateTime(windowEnd)"))
    }

    func test统一玻璃辅助层使用系统玻璃并保留旧系统材质回退() throws {
        guard let surface = try optionalSourceText(at: "RoutinUsage/Views/LiquidGlassSurface.swift") else {
            XCTFail("缺少统一玻璃辅助层")
            return
        }
        let projectSpec = try sourceText(at: "project.yml")

        XCTAssertTrue(surface.contains("if #available(macOS 26.0, *)"))
        XCTAssertTrue(surface.contains(".glassEffect("))
        XCTAssertTrue(surface.contains(".buttonStyle(.glass)"))
        XCTAssertTrue(surface.contains(".buttonStyle(.glassProminent)"))
        XCTAssertTrue(surface.contains(".regularMaterial"))
        XCTAssertTrue(surface.contains("shape.stroke(Color.white.opacity("))
        XCTAssertTrue(surface.contains(".shadow("))
        XCTAssertTrue(surface.contains("func liquidGlassSurface("))
        XCTAssertTrue(surface.contains("PopoverVisualPolicy.cornerRadius(for: .outer)"))
        XCTAssertTrue(surface.contains("func liquidGlassModalSurface("))
        XCTAssertTrue(surface.contains("func liquidGlassWindowBackground()"))
        XCTAssertTrue(projectSpec.contains("macOS: \"14.0\""))
    }

    func test设置页和用量弹窗使用统一玻璃窗口背景() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/SettingsWindowView.swift")
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertTrue(settings.contains(".liquidGlassWindowBackground()"))
        XCTAssertTrue(popover.contains(".liquidGlassWindowBackground()"))
    }

    func test设置页使用原生侧栏并避免嵌套玻璃容器() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/SettingsWindowView.swift")
        let credentials = try sourceText(at: "RoutinUsage/Views/Settings/CredentialManagementView.swift")

        XCTAssertTrue(settings.contains("HStack(spacing: 0)"))
        XCTAssertFalse(settings.contains("NavigationSplitView"))
        XCTAssertTrue(settings.contains("SettingsSection"))
        XCTAssertTrue(settings.contains(".liquidGlassWindowBackground()"))
        XCTAssertTrue(settings.contains("List(SettingsSection.allCases"))
        XCTAssertFalse(settings.contains(".liquidGlassSurface(cornerRadius:"))
        XCTAssertTrue(credentials.contains("UsageRowView("))
        XCTAssertTrue(credentials.contains("LazyVGrid"))
    }

    func test弹窗筛选器使用单行供应商菜单并保留固定底栏() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let row = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")
        let onboarding = try sourceText(at: "RoutinUsage/Views/OnboardingView.swift")

        XCTAssertTrue(popover.contains("ScrollView(.vertical, showsIndicators: false)"))
        XCTAssertTrue(popover.contains("ThinVerticalScrollIndicator"))
        XCTAssertTrue(popover.contains("账户用量"))
        XCTAssertTrue(popover.contains("ProviderFilterMenu("))
        XCTAssertTrue(popover.contains("providerCounts"))
        XCTAssertTrue(popover.contains("ProviderFilterMenuModel.options"))
        XCTAssertFalse(popover.contains("WrappingFilterChips"))
        XCTAssertTrue(popover.contains("visibleProviderIDs"))
        XCTAssertTrue(popover.contains("updateReleaseOverlay"))
        XCTAssertTrue(popover.contains("UpdateReleasePopup"))
        XCTAssertTrue(popover.contains("var bottomBar: some View"))
        XCTAssertFalse(popover.contains(".liquidGlassButton()"))
        XCTAssertTrue(popover.contains(".liquidGlassWindowBackground()"))
        XCTAssertFalse(popover.contains(".liquidGlassSurface(cornerRadius:"))
        XCTAssertFalse(popover.contains(".liquidGlassControlSurface()"))
        XCTAssertFalse(popover.contains(".liquidGlassProgressSurface()"))
        XCTAssertFalse(row.contains(".liquidGlassSurface(cornerRadius:"))
        XCTAssertFalse(row.contains(".liquidGlassProgressSurface()"))
        XCTAssertTrue(onboarding.contains(".liquidGlassSurface()"))
        XCTAssertTrue(onboarding.contains(".liquidGlassButton(prominent: true)"))
        XCTAssertTrue(onboarding.contains(".liquidGlassWindowBackground()"))
    }

    func test菜单栏真实图标保留辅助功能描述() throws {
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(statusBarController.contains("MenuBarMultiUsageIcon.image("))
        XCTAssertTrue(statusBarController.contains("indicators: selectedIndicators"))
        XCTAssertTrue(statusBarController.contains("MenuBarMonoBrandLogo.image()"))
        XCTAssertTrue(statusBarController.contains("button.setAccessibilityLabel"))
    }

    func test弹窗倒计时每分钟刷新并将分组倍率合并为一行() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(usageRowView.contains("TimelineView(.periodic(from: .now, by: 60))"))
        XCTAssertTrue(usageRowView.contains("now: timeline.date"))
        XCTAssertFalse(usageRowView.contains("ForEach(Array(groupMultipliers.enumerated())"))
        XCTAssertFalse(usageRowView.contains("details.append(UsageFormatter.groupMultiplierText"))
    }

    func test弹窗将分组倍率置于百分比下方并右对齐() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        let headerStart = try XCTUnwrap(
            usageRowView.range(of: "func headerView(now: Date) -> some View")
        )
        let progressStart = try XCTUnwrap(usageRowView.range(of: "UsageMetricProgressBar"))
        let header = usageRowView[headerStart.lowerBound..<progressStart.lowerBound]

        XCTAssertTrue(header.contains("VStack(alignment: .trailing"))
        XCTAssertFalse(header.contains("groupMultiplierText(currentGroupMultiplier)"))
        XCTAssertTrue(header.contains("Spacer(minLength: 8)"))
        XCTAssertFalse(usageRowView.contains("HStack {\n                    Spacer()\n                    Text(UsageFormatter.groupMultiplierText(groupMultipliers))"))
    }

    func test本地Key相关文案不再声称使用系统钥匙串() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/CredentialManagementView.swift")
        let onboarding = try sourceText(at: "RoutinUsage/Views/OnboardingView.swift")

        XCTAssertFalse(settings.contains("将同时删除系统钥匙串中的 Key"))
        XCTAssertFalse(onboarding.contains("Key 仅保存在这台 Mac 的系统钥匙串中"))
    }

    func test工程规格锁定为Xcode15兼容格式() throws {
        let projectSpec = try sourceText(at: "project.yml")

        XCTAssertTrue(projectSpec.contains("projectFormat: xcode15_3"))
    }

    func test工程锁定Xcode15兼容的Swift版本() throws {
        let projectSpec = try sourceText(at: "project.yml")

        XCTAssertTrue(projectSpec.contains("SWIFT_VERSION: \"5.0\""))
    }

    func test发布工作流使用MyToken作为版本展示名称() throws {
        let releaseWorkflow = try sourceText(at: ".github/workflows/release-macos.yml")

        XCTAssertTrue(releaseWorkflow.contains("name: MyToken v${{ inputs.version }}"))
        XCTAssertTrue(releaseWorkflow.contains("RELEASE_VERSION: ${{ inputs.version }}"))
        XCTAssertTrue(releaseWorkflow.contains("cp \"build/dist/MyToken.dmg\" \"build/dist/${dmg_name}\""))
        XCTAssertTrue(releaseWorkflow.contains("uname -m"))
        XCTAssertTrue(releaseWorkflow.contains("tag_name: v${{ inputs.version }}"))
        XCTAssertTrue(releaseWorkflow.contains("make_latest: true"))
        XCTAssertFalse(releaseWorkflow.contains("macos-v${{ inputs.version }}"))
        XCTAssertFalse(releaseWorkflow.contains("MyRoutin.dmg"))
        XCTAssertFalse(releaseWorkflow.contains("Routin Usage"))
    }

    func test发布工作流要求手动Markdown更新日志() throws {
        let workflow = try sourceText(at: ".github/workflows/release-macos.yml")
        let androidWorkflow = try sourceText(at: ".github/workflows/release-android.yml")
        let releaseNotesInput = """
              release_notes:
                description: 'macOS 发布说明（Markdown，首行用于工作流名称）'
                required: true
                type: string
        """

        XCTAssertTrue(workflow.contains(releaseNotesInput))
        XCTAssertTrue(workflow.contains("run-name: 发布 macOS v${{ inputs.version }} · ${{ inputs.release_notes }}"))
        XCTAssertTrue(androidWorkflow.contains("run-name: 发布 Android v${{ inputs.version }} · ${{ inputs.release_notes }}"))
        XCTAssertTrue(workflow.contains("release_notes:"))
        XCTAssertTrue(workflow.contains("body: ${{ inputs.release_notes }}"))
        XCTAssertTrue(workflow.contains("generate_release_notes: false"))
        XCTAssertTrue(workflow.contains("GITHUB_STEP_SUMMARY"))
        XCTAssertTrue(androidWorkflow.contains("GITHUB_STEP_SUMMARY"))
        XCTAssertTrue(androidWorkflow.contains("body: ${{ inputs.release_notes }}"))
    }

    func test发布工作流显式选择并校验Xcode() throws {
        let workflow = try sourceText(at: ".github/workflows/release-macos.yml")

        XCTAssertTrue(
            workflow.contains("DEVELOPER_DIR: /Applications/Xcode_26.3.app/Contents/Developer")
        )
        XCTAssertTrue(workflow.contains("scripts/verify-xcode.sh"))
    }

    func test持续集成显式选择并校验Xcode() throws {
        let workflow = try sourceText(at: ".github/workflows/ci.yml")

        XCTAssertTrue(
            workflow.contains("DEVELOPER_DIR: /Applications/Xcode_26.3.app/Contents/Developer")
        )
        XCTAssertTrue(workflow.contains("scripts/verify-xcode.sh"))
    }

    func test单元测试运行时不初始化菜单栏控制器() throws {
        let app = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertTrue(app.contains("XCTestConfigurationFilePath"))
        XCTAssertTrue(app.contains("guard !Self.isRunningUnitTests"))
        XCTAssertTrue(app.contains("@State private var statusBarController: StatusBarController?"))
    }

    func test弹窗详情显示剩余时长与配对分组倍率且不显示允许模型() throws {
        let usageRowView = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertTrue(usageRowView.contains("title: \"5 小时\""))
        XCTAssertTrue(usageRowView.contains("title: \"周\""))
        XCTAssertTrue(usageRowView.contains("UsageFormatter.remainingDurationText"))
        XCTAssertFalse(usageRowView.contains("groupMultipliers"))
        XCTAssertFalse(usageRowView.contains("allowedModels"))
        XCTAssertFalse(usageRowView.contains("允许模型"))
    }

    func test菜单栏弹窗失焦关闭并保持状态栏层级() throws {
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(statusBarController.contains("NSWindow.didResignKeyNotification"))
        XCTAssertTrue(statusBarController.contains("popover.performClose(nil)"))
        XCTAssertTrue(statusBarController.contains("window.level = .statusBar"))
    }

    func test弹窗详情不再拼接配对分组倍率() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")

        XCTAssertFalse(settings.contains("UsageFormatter.groupMultiplierText(snapshot.groupMultipliers)"))
    }

    func test凭证行不切换菜单栏当前Key并提供启用开关() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/CredentialManagementView.swift")

        XCTAssertTrue(settings.contains("Toggle"))
        XCTAssertTrue(settings.contains("model.setEnabled"))
        XCTAssertTrue(settings.contains("isEnabled"))
        XCTAssertFalse(settings.contains("store.selectKey"))
        XCTAssertFalse(settings.contains("store.selectedKeyID"))
    }

    func test菜单栏弹窗只使用启用Key() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertTrue(popover.contains("store.visibleKeyIDs"))
        XCTAssertTrue(popover.contains("displayOrder.visible(enabledIDs:"))
    }

    func test菜单栏管理页提供常驻拖拽和无障碍动作() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/MenuBarManagementView.swift")

        XCTAssertFalse(settings.contains("isReorderingMenuBarIndicators"))
        XCTAssertFalse(settings.contains("private struct CredentialSortInteraction"))
        XCTAssertTrue(settings.contains("ReorderableCredentialCardList"))
        XCTAssertTrue(settings.contains("reorderingDisplay"))
        XCTAssertTrue(settings.contains("accessibilityAction"))
    }

    func test设置页显示当前版本与完整更新日志() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/HelpUpdateView.swift")
        let updateNotes = try sourceText(at: "RoutinUsage/Views/UpdateNotesView.swift")

        XCTAssertTrue(settings.contains("当前版本"))
        XCTAssertTrue(settings.contains("UpdateNotesView(notes: update.notes)"))
        XCTAssertFalse(updateNotes.contains(".lineLimit("))
    }

    func test更新日志视图使用Markdown并为无日志版本提供提示() throws {
        let updateNotes = try sourceText(at: "RoutinUsage/Views/UpdateNotesView.swift")

        XCTAssertTrue(updateNotes.contains("AttributedString(markdown:"))
        XCTAssertTrue(updateNotes.contains("此版本未提供更新日志"))
    }

    func test更新流程展示下载进度和完成提示并自动重启() throws {
        let environment = try sourceText(at: "RoutinUsage/App/AppEnvironment.swift")
        let service = try sourceText(at: "RoutinUsage/Updates/GitHubUpdateService.swift")
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/HelpUpdateView.swift")

        XCTAssertTrue(environment.contains("case downloading(progress: Double?)"))
        XCTAssertTrue(environment.contains("case completed(String)"))
        XCTAssertTrue(service.contains("createsNewApplicationInstance"))
        XCTAssertTrue(service.contains("UpdateCompletionNotice"))
        XCTAssertTrue(service.contains("if error == nil"))
        XCTAssertTrue(service.contains("新版本已安装到“应用程序”文件夹"))
        XCTAssertTrue(popover.contains("ProgressView(value: progress"))
        XCTAssertTrue(popover.contains("onBackground"))
        XCTAssertTrue(popover.contains("后台更新"))
        XCTAssertTrue(popover.contains("guard selectedUpdate == nil else"))
        XCTAssertTrue(popover.contains("shouldShowFooterUpdateProgress"))
        XCTAssertTrue(popover.contains("更新完成"))
        XCTAssertTrue(settings.contains("ProgressView(value: progress"))
        XCTAssertTrue(settings.contains("更新完成"))
    }

    func test小米重试登录清理旧Web会话并绕过缓存() throws {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let session = try String(
            contentsOf: projectRoot
                .appendingPathComponent("RoutinUsage/Providers/XiaomiWebSession.swift"),
            encoding: .utf8
        )
        let loginWindow = try String(
            contentsOf: projectRoot
                .appendingPathComponent("RoutinUsage/Views/XiaomiLoginWindow.swift"),
            encoding: .utf8
        )

        XCTAssertTrue(session.contains("await clearWebsiteData()"))
        XCTAssertTrue(session.contains(".reloadIgnoringLocalAndRemoteCacheData"))
        XCTAssertTrue(loginWindow.contains("resetSession: request.resetsWebSession"))
    }

    func test更新弹窗操作按钮整块可点击() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let phase = try XCTUnwrap(popover.range(of: "ghostButton(title: \"稍后更新\""))
        let end = try XCTUnwrap(popover.range(of: "private var publishedText"))
        let actionButtons = popover[phase.lowerBound..<end.lowerBound]

        XCTAssertEqual(actionButtons.components(separatedBy: ".contentShape(Rectangle())").count - 1, 2)
    }

    func test更新弹窗使用版本徽章并随系统深浅色() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertTrue(popover.contains("发现新版本"))
        XCTAssertFalse(popover.contains("更新日志 & 优化项目"))
        XCTAssertTrue(popover.contains("UpdateNotesView(notes: update.notes)"))
        XCTAssertTrue(popover.contains("稍后更新"))
        XCTAssertTrue(popover.contains("立即更新"))
        XCTAssertFalse(popover.contains(".environment(\\.colorScheme, .light)"))
    }

    func testAndroid卡片失败标识与刷新按钮同处标题区() throws {
        let projectRoot = URL(fileURLWithPath: #filePath)
            .deletingLastPathComponent()
            .deletingLastPathComponent()
        let card = try String(
            contentsOf: projectRoot
                .appendingPathComponent("android/feature-home/src/main/kotlin/ai/routin/mytoken/feature/home/CredentialUsageCard.kt"),
            encoding: .utf8
        )
        let dialog = try String(
            contentsOf: projectRoot
                .appendingPathComponent("android/feature-credentials/src/main/kotlin/ai/routin/mytoken/feature/credentials/XiaomiLoginActivity.kt"),
            encoding: .utf8
        )
        // 失败徽章改挂头像后，标题区改为 CredentialCardHeader：头像徽章在左，刷新按钮在右。
        let headerStart = try XCTUnwrap(card.range(of: "CredentialCardHeader("))
        let avatarCallIndex = try XCTUnwrap(card.range(of: "AvatarWithFailureBadge(card = card"))
        let refreshTagIndex = try XCTUnwrap(card.range(of: "credential_refresh_${card.credential.id}"))
        let header = card[headerStart.lowerBound..<refreshTagIndex.lowerBound]

        XCTAssertTrue(header.contains("AvatarWithFailureBadge(card = card"))
        XCTAssertTrue(card.contains("credential_failure_${card.credential.id}"))
        XCTAssertTrue(avatarCallIndex.lowerBound < refreshTagIndex.lowerBound)
        XCTAssertFalse(card.contains("widthIn(max = 96.dp)"))
        XCTAssertFalse(card.contains("overflow = TextOverflow.Ellipsis"))
        XCTAssertTrue(dialog.contains("WebSettings.LOAD_NO_CACHE"))
        XCTAssertTrue(dialog.contains("removeAllCookies"))
        XCTAssertTrue(dialog.contains("onReceivedHttpError"))
        XCTAssertTrue(dialog.contains("/api/v1/userProfile"))
    }

    func test弹窗设置入口复用右键菜单设置逻辑() throws {
        let usagePopoverView = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertTrue(usagePopoverView.contains("openSettings()"))
        XCTAssertFalse(usagePopoverView.contains("NSApp.setActivationPolicy(.regular)"))
        XCTAssertTrue(statusBarController.contains("openSettings: { [weak self] in"))
        XCTAssertTrue(statusBarController.contains("self?.openSettingsWindow()"))
        XCTAssertTrue(usagePopoverView.contains("设置"))
        XCTAssertFalse(usagePopoverView.contains("SettingsLink"))
    }

    func testRoutin登录和签到功能已移除() throws {
        let app = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")
        let environment = try sourceText(at: "RoutinUsage/App/AppEnvironment.swift")
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertFalse(app.contains("RoutinCheckInWindow"))
        XCTAssertFalse(app.contains("id: \"routin-check-in\""))
        XCTAssertFalse(environment.contains("RoutinCheckInService"))
        XCTAssertFalse(environment.contains("RoutinWebSession"))
        XCTAssertFalse(statusBarController.contains("routinCheckIn"))
        XCTAssertFalse(popover.contains("Routin 签到："))
    }

    func test启动时不会创建Routin签到场景() throws {
        let app = try sourceText(at: "RoutinUsage/App/RoutinUsageApp.swift")

        XCTAssertFalse(app.contains("Window(\"Routin 签到\", id: \"routin-check-in\")"))
        XCTAssertTrue(app.contains("closeLegacySuppressedLaunchWindows()"))
    }

    func testCodex分组检测功能已移除() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let environment = try sourceText(at: "RoutinUsage/App/AppEnvironment.swift")
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertFalse(popover.contains("startCodexGroupDetection"))
        XCTAssertFalse(popover.contains("获取 Codex 当前分组？"))
        XCTAssertFalse(environment.contains("CodexGroupDetectionService"))
        XCTAssertFalse(environment.contains("startCodexGroupDetection"))
        XCTAssertFalse(statusBarController.contains("CodexGroupDetection"))
    }

    func test新设置页面不保存Routin账号密码或Cookie() throws {
        let settings = try sourceText(at: "RoutinUsage/Views/Settings/SettingsWindowView.swift")
        let credentials = try sourceText(at: "RoutinUsage/Views/Settings/CredentialManagementView.swift")
        let help = try sourceText(at: "RoutinUsage/Views/Settings/HelpUpdateView.swift")

        XCTAssertFalse(settings.contains("SecureField(\"Routin"))
        XCTAssertFalse(settings.contains("TextField(\"账号"))
        XCTAssertFalse(settings.contains("TextField(\"密码"))
        XCTAssertFalse(settings.contains("Cookie"))
        XCTAssertFalse(credentials.contains("SecureField(\"Routin"))
        XCTAssertFalse(credentials.contains("TextField(\"账号"))
        XCTAssertFalse(credentials.contains("TextField(\"密码"))
        XCTAssertFalse(credentials.contains("Cookie"))
        XCTAssertFalse(help.contains("SecureField(\"Routin"))
        XCTAssertFalse(help.contains("TextField(\"账号"))
        XCTAssertFalse(help.contains("TextField(\"密码"))
        XCTAssertFalse(help.contains("Cookie"))
    }

    func test菜单栏弹窗不提供签到状态和分组检测状态() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")

        XCTAssertFalse(popover.contains("openWindow(id: \"routin-check-in\")\n                    Task { await startRoutinCheckIn() }"))
        XCTAssertFalse(popover.contains("if hasRoutinAccount"))
        XCTAssertFalse(popover.contains("checkInHelpText"))
        XCTAssertFalse(popover.contains("checkInState.statusText"))
        XCTAssertFalse(popover.contains("Routin 签到："))
    }

    func testCodex分组检测展示层已移除() throws {
        let popover = try sourceText(at: "RoutinUsage/Views/UsagePopoverView.swift")
        let row = try sourceText(at: "RoutinUsage/Views/UsageRowView.swift")
        let environment = try sourceText(at: "RoutinUsage/App/AppEnvironment.swift")
        let statusBarController = try sourceText(at: "RoutinUsage/App/StatusBarController.swift")

        XCTAssertFalse(popover.contains("startCodexGroupDetection"))
        XCTAssertFalse(popover.contains("codexGroupDetectionStatus"))
        XCTAssertFalse(row.contains("detectionRecord"))
        XCTAssertFalse(row.contains("Codex 分组检测"))
        XCTAssertFalse(environment.contains("codexGroupDetection"))
        XCTAssertFalse(statusBarController.contains("codexGroupDetection"))
    }

    private func sourceText(at relativePath: String) throws -> String {
        guard let source = try optionalSourceText(at: relativePath) else {
            throw CocoaError(.fileNoSuchFile)
        }
        return source
    }

    private func optionalSourceText(at relativePath: String) throws -> String? {
        let resource: (name: String, extension: String?)
        switch relativePath {
        case "project.yml":
            resource = ("project", "yml")
        case "scripts/test.sh":
            resource = ("test", "sh")
        case ".github/workflows/release-macos.yml":
            resource = ("release-macos", "yml")
        case ".github/workflows/release-android.yml":
            resource = ("release-android", "yml")
        case ".github/workflows/ci.yml":
            resource = ("ci", "yml")
        case "RoutinUsage/App/RoutinUsageApp.swift":
            resource = ("RoutinUsageApp.swift", "txt")
        case "RoutinUsage/App/StatusBarController.swift":
            resource = ("StatusBarController.swift", "txt")
        case "RoutinUsage/App/AppEnvironment.swift":
            resource = ("AppEnvironment.swift", "txt")
        case "RoutinUsage/Updates/GitHubUpdateService.swift":
            resource = ("GitHubUpdateService.swift", "txt")
        case "RoutinUsage/Views/Settings/SettingsWindowView.swift":
            resource = ("SettingsWindowView.swift", "txt")
        case "RoutinUsage/Views/Settings/CredentialManagementView.swift":
            resource = ("CredentialManagementView.swift", "txt")
        case "RoutinUsage/Views/Settings/MenuBarManagementView.swift":
            resource = ("MenuBarManagementView.swift", "txt")
        case "RoutinUsage/Views/Settings/GeneralSettingsView.swift":
            resource = ("GeneralSettingsView.swift", "txt")
        case "RoutinUsage/Views/Settings/HelpUpdateView.swift":
            resource = ("HelpUpdateView.swift", "txt")
        case "RoutinUsage/Views/UpdateNotesView.swift":
            resource = ("UpdateNotesView.swift", "txt")
        case "RoutinUsage/Views/KeyEditorView.swift":
            resource = ("KeyEditorView.swift", "txt")
        case "RoutinUsage/Views/OnboardingView.swift":
            resource = ("OnboardingView.swift", "txt")
        case "RoutinUsage/Views/UsageRowView.swift":
            resource = ("UsageRowView.swift", "txt")
        case "RoutinUsage/Views/NormalizedUsageMetricGrid.swift":
            resource = ("NormalizedUsageMetricGrid.swift", "txt")
        case "RoutinUsage/Models/MenuBarStyle.swift":
            resource = ("MenuBarStyle.swift", "txt")
        case "RoutinUsage/Views/UsagePopoverView.swift":
            resource = ("UsagePopoverView.swift", "txt")
        case "RoutinUsage/Views/LiquidGlassSurface.swift":
            resource = ("LiquidGlassSurface.swift", "txt")
        default:
            throw CocoaError(.fileNoSuchFile)
        }
        guard let sourceURL = Bundle(for: ProjectBootstrapTests.self)
            .url(forResource: resource.name, withExtension: resource.extension) else { return nil }
        return try String(contentsOf: sourceURL, encoding: .utf8)
    }

    @MainActor
    private func renderedBitmap(_ image: NSImage) throws -> NSBitmapImageRep {
        let pixelScale = 2
        let bitmap = try XCTUnwrap(
            NSBitmapImageRep(
                bitmapDataPlanes: nil,
                pixelsWide: Int(image.size.width) * pixelScale,
                pixelsHigh: Int(image.size.height) * pixelScale,
                bitsPerSample: 8,
                samplesPerPixel: 4,
                hasAlpha: true,
                isPlanar: false,
                colorSpaceName: .calibratedRGB,
                bytesPerRow: 0,
                bitsPerPixel: 0
            )
        )
        bitmap.size = image.size
        let context = try XCTUnwrap(NSGraphicsContext(bitmapImageRep: bitmap))

        NSGraphicsContext.saveGraphicsState()
        NSGraphicsContext.current = context
        image.draw(
            in: NSRect(origin: .zero, size: image.size),
            from: NSRect(origin: .zero, size: image.size),
            operation: .sourceOver,
            fraction: 1
        )
        NSGraphicsContext.restoreGraphicsState()

        return bitmap
    }
}
