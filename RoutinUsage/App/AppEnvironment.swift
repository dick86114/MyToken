import AppKit
import Foundation
import Observation

@MainActor
protocol RefreshScheduling: AnyObject {
    func start(minutes: Int, onTick: @escaping @Sendable () -> Void)
    func reschedule(minutes: Int)
    func stop()
}

enum AppUpdateStatus: Equatable {
    case idle
    case checking
    case available(AppUpdate)
    case downloading(progress: Double?)
    case completed(String)
    case failed(String)
}

extension RefreshScheduler: RefreshScheduling {}

actor AuthorizationCachingNotificationSender: NotificationSending {
    private let sender: any NotificationSending
    private var authorizationTask: Task<Bool, Error>?

    init(sender: any NotificationSending) {
        self.sender = sender
    }

    func requestAuthorization() async throws -> Bool {
        if let authorizationTask {
            return try await authorizationTask.value
        }

        let sender = sender
        let task = Task {
            try await sender.requestAuthorization()
        }
        authorizationTask = task
        do {
            let result = try await task.value
            authorizationTask = nil
            return result
        } catch {
            authorizationTask = nil
            throw error
        }
    }

    func send(_ alert: UsageAlert) async throws {
        try await sender.send(alert)
    }
}

@MainActor
@Observable
final class AppEnvironment {
    let settings: AppSettings
    let store: UsageStore
    let loginItemManager: any LoginItemManaging
    let routinCheckIn: RoutinCheckInService
    let routinWebSession: RoutinWebSession?
    var showsOnboarding = false
    private(set) var updateStatus: AppUpdateStatus = .idle
    private(set) var updateCompletionNotice: String?

    @ObservationIgnored private let refreshScheduler: any RefreshScheduling
    @ObservationIgnored private let keyRepository: KeyRepository
    @ObservationIgnored private let apiClient: any UsageFetching
    @ObservationIgnored private let notificationSender: any NotificationSending
    @ObservationIgnored private let updateService: any UpdateChecking
    @ObservationIgnored private let logWriter: any AppLogWriting
    @ObservationIgnored private let updateCheckScheduler: any UpdateCheckScheduling
    @ObservationIgnored private let notificationTaskYield: @Sendable () async -> Void
    @ObservationIgnored private let applicationNotificationCenter: NotificationCenter
    @ObservationIgnored private var terminationObservation: ApplicationTerminationObservation?
    @ObservationIgnored private var isStarted = false
    @ObservationIgnored private var hasRequestedNotificationAuthorization = false
    @ObservationIgnored private var notificationAuthorizationTask: Task<Void, Never>?
    @ObservationIgnored private var updateCheckTask: Task<Void, Never>?
    @ObservationIgnored private var updateCheckGeneration = 0
    @ObservationIgnored private var updateStatusBeforeChecking: AppUpdateStatus?

    init(
        settings: AppSettings,
        store: UsageStore,
        refreshScheduler: any RefreshScheduling,
        loginItemManager: any LoginItemManaging,
        keyRepository: KeyRepository,
        apiClient: any UsageFetching,
        notificationSender: any NotificationSending,
        applicationNotificationCenter: NotificationCenter = .default,
        updateService: any UpdateChecking = NoUpdateService(),
        updateCheckScheduler: (any UpdateCheckScheduling)? = nil,
        notificationTaskYield: @escaping @Sendable () async -> Void = { await Task.yield() },
        logWriter: any AppLogWriting = NoopAppLogWriter(),
        routinCheckIn: RoutinCheckInService? = nil,
        routinWebSession: RoutinWebSession? = nil
    ) {
        self.settings = settings
        self.store = store
        self.refreshScheduler = refreshScheduler
        self.loginItemManager = loginItemManager
        self.keyRepository = keyRepository
        self.apiClient = apiClient
        self.notificationSender = notificationSender
        self.applicationNotificationCenter = applicationNotificationCenter
        self.updateService = updateService
        self.logWriter = logWriter
        self.updateCheckScheduler = updateCheckScheduler ?? UpdateCheckScheduler()
        self.notificationTaskYield = notificationTaskYield
        self.routinWebSession = routinWebSession
        self.routinCheckIn = routinCheckIn ?? RoutinCheckInService(session: UnavailableRoutinWebSession())
        updateCompletionNotice = UpdateCompletionNotice.consume()
    }

    static func live() -> AppEnvironment {
        let defaults = UserDefaults.standard
        let logWriter = AppLogStore.shared
        let settings = AppSettings(defaults: defaults)
        let localStore = LocalKeyStore(defaults: defaults)
        let keyRepository = KeyRepository(defaults: defaults, localStore: localStore)
        let cache = UsageCache(defaults: defaults)
        let apiClient = UsageAPIClient(session: .shared, mapper: UsageMapper())
        let alertEvaluator = AlertEvaluator(defaults: defaults)
        let notificationSender = AuthorizationCachingNotificationSender(
            sender: UserNotificationSender()
        )
        let routinWebSession = RoutinWebSession()
        let routinCheckIn = RoutinCheckInService(session: routinWebSession)
        routinWebSession.onLoginCompleted = {
            Task { @MainActor in
                await routinCheckIn.didFinishLogin()
            }
        }
        let store = UsageStore(
            keyRepository: keyRepository,
            localStore: localStore,
            apiClient: apiClient,
            cache: cache,
            alertEvaluator: alertEvaluator,
            notificationSender: notificationSender,
            defaults: defaults,
            refreshMinutes: settings.refreshMinutes,
            thresholds: settings.thresholds,
            notificationsEnabled: settings.notificationsEnabled
        )

        return AppEnvironment(
            settings: settings,
            store: store,
            refreshScheduler: RefreshScheduler(),
            loginItemManager: LoginItemManager(),
            keyRepository: keyRepository,
            apiClient: apiClient,
            notificationSender: notificationSender,
            updateService: GitHubUpdateService(logWriter: logWriter),
            updateCheckScheduler: UpdateCheckScheduler(),
            logWriter: logWriter,
            routinCheckIn: routinCheckIn,
            routinWebSession: routinWebSession
        )
    }

    func start() async {
        guard !isStarted else {
            return
        }
        isStarted = true
        observeApplicationTermination()
        LoginItemSettingSynchronizer.synchronize(
            settings: settings,
            manager: loginItemManager
        )
        showsOnboarding = store.orderedKeyIDs.isEmpty
        synchronizeStoreSettings()

        // 更新检查与六小时周期从启动阶段立即开始，不依赖首次用量刷新或通知授权。
        updateCheckScheduler.start { [weak self] in
            Task { @MainActor [weak self] in
                _ = self?.beginUpdateCheckIfNeeded(requiresStarted: true)
            }
        }
        _ = beginUpdateCheckIfNeeded(requiresStarted: true)

        await store.refreshAll()
        guard isStarted else {
            return
        }
        refreshScheduler.start(minutes: settings.refreshMinutes) { [weak self] in
            Task { @MainActor [weak self] in
                await self?.store.refreshAll()
            }
        }
        await notificationsDidChange(enabled: settings.notificationsEnabled)
        guard isStarted else {
            return
        }
    }

    func refreshIntervalDidChange(to minutes: Int) {
        synchronizeStoreSettings()
        guard isStarted else {
            return
        }
        refreshScheduler.reschedule(minutes: minutes)
    }

    func notificationsDidChange(enabled: Bool) async {
        synchronizeStoreSettings()
        guard enabled else {
            hasRequestedNotificationAuthorization = false
            notificationAuthorizationTask?.cancel()
            notificationAuthorizationTask = nil
            return
        }
        guard !hasRequestedNotificationAuthorization else {
            return
        }
        hasRequestedNotificationAuthorization = true
        let notificationSender = notificationSender
        notificationAuthorizationTask = Task {
            _ = try? await notificationSender.requestAuthorization()
        }
        await notificationTaskYield()
    }

    func thresholdsDidChange(to _: AlertThresholds) {
        synchronizeStoreSettings()
    }

    func selectedKeyDidChange(to keyID: UUID?) async {
        guard
            let keyID,
            let state = store.state(for: keyID),
            state.snapshot == nil
        else {
            return
        }
        await store.refresh(keyID: keyID)
    }

    func dismissOnboarding() {
        showsOnboarding = false
    }

    func checkForUpdates() async {
        await logWriter.log(level: .info, event: "update_check_requested", details: "source=user")
        guard let task = beginUpdateCheckIfNeeded(requiresStarted: false) else { return }
        await task.value
    }

    func installAvailableUpdate() async {
        guard case let .available(update) = updateStatus else { return }
        await logWriter.log(
            level: .info,
            event: "update_install_started",
            details: "version=\(update.version)"
        )
        updateStatus = .downloading(progress: nil)
        do {
            let dmgURL = try await updateService.download(update) { [weak self] progress in
                await self?.setUpdateDownloadProgress(progress)
            }
            try UpdateInstaller.install(
                dmgURL: dmgURL,
                version: update.version,
                logWriter: logWriter
            )
            updateStatus = .completed(update.version)
        } catch is CancellationError {
            await logWriter.log(level: .warning, event: "update_install_cancelled", details: nil)
            updateStatus = .available(update)
        } catch let error as UpdateServiceError {
            await logWriter.log(
                level: .error,
                event: "update_install_failed",
                details: String(describing: error)
            )
            updateStatus = .failed(updateErrorDescription(error))
        } catch {
            await logWriter.log(
                level: .error,
                event: "update_install_failed",
                details: String(describing: error)
            )
            updateStatus = .failed("安装更新失败，请下载后手动安装")
        }
    }

    func openIssueReport() async {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
        let context = IssueReportContext(
            version: version,
            operatingSystem: ProcessInfo.processInfo.operatingSystemVersionString,
            architecture: IssueReporter.architecture(),
            updateStatus: IssueReporter.statusDescription(updateStatus),
            logs: await logWriter.recentText(maxCharacters: IssueReporter.maxLogCharacters)
        )
        guard let url = IssueReporter.makeIssueURL(context: context) else {
            await logWriter.log(level: .error, event: "issue_report_url_failed", details: nil)
            return
        }
        guard IssueReporter.openIssueURL(url) else {
            await logWriter.log(level: .error, event: "issue_report_open_failed", details: url.absoluteString)
            let alert = NSAlert()
            alert.messageText = "无法打开问题提交页面"
            alert.informativeText = "请检查网络连接后重试。"
            alert.alertStyle = .warning
            alert.addButton(withTitle: "好")
            alert.runModal()
            return
        }
        await logWriter.log(level: .info, event: "issue_report_opened", details: nil)
    }

    func presentUpdateCompletionNoticeIfNeeded() {
        guard let version = updateCompletionNotice else {
            return
        }
        updateCompletionNotice = nil

        let alert = NSAlert()
        alert.messageText = "更新完成"
        alert.informativeText = "MyRoutin 已更新到 \(version)，应用已重新启动。"
        alert.alertStyle = .informational
        alert.addButton(withTitle: "好")
        NSApp.activate(ignoringOtherApps: true)
        alert.runModal()
    }

    func updateValidatedKey(
        id: UUID,
        name: String,
        secret: String
    ) async throws -> KeyEditorSaveResult {
        let input = try KeyEditorValidation.validate(name: name, secret: secret)
        let validationTime = Date()
        let snapshot: UsageSnapshot?
        do {
            snapshot = try await apiClient.fetchUsage(apiKey: input.secret, now: validationTime)
            try Task.checkCancellation()
        } catch is CancellationError {
            throw CancellationError()
        } catch let error as UsageAPIError {
            throw Self.storeError(from: error)
        } catch {
            throw UsageStoreError.invalidResponse
        }

        do {
            _ = try keyRepository.update(id: id, name: input.name, secret: input.secret)
        } catch {
            throw UsageStoreError.persistence
        }
        await store.applyValidatedSnapshot(snapshot, for: id, validatedAt: validationTime)
        return snapshot == nil ? .savedWithoutSubscription : .saved
    }

    func moveKey(fromOffsets: IndexSet, toOffset: Int) {
        keyRepository.move(fromOffsets: fromOffsets, toOffset: toOffset)
        store.reloadConfigurations()
    }

    func readKey(id: UUID) -> String? {
        try? keyRepository.read(id: id)
    }

    func startRoutinCheckIn() async {
        if routinWebSession != nil {
            NotificationCenter.default.post(name: .showRoutinCheckInWindow, object: nil)
        }
        await routinCheckIn.startCheckIn()
        if routinCheckIn.state == .needsLogin {
            await beginRoutinLogin()
        }
    }

    func beginRoutinLogin() async {
        guard routinWebSession != nil else {
            return
        }
        await routinCheckIn.beginLogin()
        NotificationCenter.default.post(name: .showRoutinCheckInWindow, object: nil)
    }

    func signOutRoutin() async {
        await routinCheckIn.signOut()
    }

    func stop() {
        guard isStarted else {
            return
        }
        isStarted = false
        terminationObservation = nil
        notificationAuthorizationTask?.cancel()
        notificationAuthorizationTask = nil
        refreshScheduler.stop()
        updateCheckScheduler.stop()
        cancelActiveUpdateCheck()
    }

    deinit {
        updateCheckTask?.cancel()
    }
}

private actor UnavailableRoutinWebSession: RoutinWebSessionManaging {
    func hasAuthenticatedSession() async -> Bool { false }
    func prepareLogin() async {}
    func performCheckIn() async throws -> RoutinCheckInOutcome { .needsLogin }
    func clearRoutinWebsiteData() async {}
}

private extension AppEnvironment {
    enum UpdateCheckOutcome {
        case success(AppUpdate?)
        case cancelled
        case failed
    }

    @discardableResult
    func beginUpdateCheckIfNeeded(requiresStarted: Bool) -> Task<Void, Never>? {
        guard !requiresStarted || isStarted else { return nil }
        if let updateCheckTask {
            return updateCheckTask
        }
        guard updateStatus != .checking, !isDownloadingUpdate else { return nil }

        let previousStatus = updateStatus
        updateCheckGeneration &+= 1
        let generation = updateCheckGeneration
        updateStatusBeforeChecking = previousStatus
        updateStatus = .checking
        let updateService = updateService
        let logWriter = logWriter
        let task = Task { [weak self] in
            let outcome: UpdateCheckOutcome
            do {
                outcome = .success(try await updateService.checkForUpdate())
            } catch is CancellationError {
                await logWriter.log(level: .warning, event: "update_check_cancelled", details: nil)
                outcome = .cancelled
            } catch let error as UpdateServiceError {
                await logWriter.log(
                    level: .error,
                    event: "update_check_failed",
                    details: String(describing: error)
                )
                outcome = .failed
            } catch {
                await logWriter.log(
                    level: .error,
                    event: "update_check_failed",
                    details: String(describing: error)
                )
                outcome = .failed
            }
            guard let self else { return }
            self.finishUpdateCheck(
                outcome,
                generation: generation,
                requiresStarted: requiresStarted
            )
        }
        updateCheckTask = task
        return task
    }

    func finishUpdateCheck(
        _ outcome: UpdateCheckOutcome,
        generation: Int,
        requiresStarted: Bool
    ) {
        guard generation == updateCheckGeneration else { return }
        updateCheckTask = nil
        let previousStatus = updateStatusBeforeChecking ?? .idle
        updateStatusBeforeChecking = nil
        guard !requiresStarted || isStarted else { return }
        guard !Task.isCancelled else { return }

        switch outcome {
        case let .success(update):
            updateStatus = update.map(AppUpdateStatus.available) ?? .idle
        case .cancelled:
            updateStatus = previousStatus
        case .failed:
            if case .available = previousStatus {
                updateStatus = previousStatus
            } else {
                updateStatus = .failed("检查更新失败，请稍后重试")
            }
        }
    }

    func cancelActiveUpdateCheck() {
        updateCheckGeneration &+= 1
        updateCheckTask?.cancel()
        updateCheckTask = nil
        if updateStatus == .checking {
            updateStatus = updateStatusBeforeChecking ?? .idle
        }
        updateStatusBeforeChecking = nil
    }

    var isDownloadingUpdate: Bool {
        if case .downloading = updateStatus {
            return true
        }
        return false
    }

    func setUpdateDownloadProgress(_ progress: Double?) {
        guard case .downloading = updateStatus else {
            return
        }
        updateStatus = .downloading(progress: progress.map { min(max($0, 0), 1) })
    }

    func synchronizeStoreSettings() {
        store.updateSettings(
            refreshMinutes: settings.refreshMinutes,
            thresholds: settings.thresholds,
            notificationsEnabled: settings.notificationsEnabled
        )
    }

    func observeApplicationTermination() {
        guard terminationObservation == nil else {
            return
        }
        terminationObservation = ApplicationTerminationObservation(
            notificationCenter: applicationNotificationCenter
        ) { [weak self] in
            Task { @MainActor [weak self] in
                self?.stop()
            }
        }
    }

    static func storeError(from error: UsageAPIError) -> UsageStoreError {
        switch error {
        case .invalidKey:
            return .invalidKey
        case .transport:
            return .network
        case .invalidResponse:
            return .invalidResponse
        case .server(statusCode: 401):
            return .invalidKey
        case let .server(statusCode):
            return .server(statusCode: statusCode)
        }
    }

    func updateErrorDescription(_ error: UpdateServiceError) -> String {
        switch error {
        case .unavailable:
            return "更新服务暂时不可用，请稍后重试"
        case .invalidResponse:
            return "更新信息不完整，请前往 GitHub 手动下载"
        case .downloadFailed:
            return "下载更新失败，请检查网络后重试"
        case let .installFailed(message):
            return "安装更新失败：\(message)。请从 GitHub 手动安装"
        }
    }
}

private final class ApplicationTerminationObservation: @unchecked Sendable {
    private let notificationCenter: NotificationCenter
    private var token: NSObjectProtocol?

    init(
        notificationCenter: NotificationCenter,
        action: @escaping @Sendable () -> Void
    ) {
        self.notificationCenter = notificationCenter
        token = notificationCenter.addObserver(
            forName: NSApplication.willTerminateNotification,
            object: nil,
            queue: nil
        ) { _ in
            action()
        }
    }

    deinit {
        if let token {
            notificationCenter.removeObserver(token)
        }
    }
}
