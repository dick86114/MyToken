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
    case downloading
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
    var showsOnboarding = false
    private(set) var updateStatus: AppUpdateStatus = .idle

    @ObservationIgnored private let refreshScheduler: any RefreshScheduling
    @ObservationIgnored private let keyRepository: KeyRepository
    @ObservationIgnored private let apiClient: any UsageFetching
    @ObservationIgnored private let notificationSender: any NotificationSending
    @ObservationIgnored private let updateService: any UpdateChecking
    @ObservationIgnored private let updateCheckScheduler: any UpdateCheckScheduling
    @ObservationIgnored private let applicationNotificationCenter: NotificationCenter
    @ObservationIgnored private var terminationObservation: ApplicationTerminationObservation?
    @ObservationIgnored private var isStarted = false
    @ObservationIgnored private var hasRequestedNotificationAuthorization = false
    @ObservationIgnored private var notificationAuthorizationTask: Task<Void, Never>?

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
        updateCheckScheduler: (any UpdateCheckScheduling)? = nil
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
        self.updateCheckScheduler = updateCheckScheduler ?? UpdateCheckScheduler()
    }

    static func live() -> AppEnvironment {
        let defaults = UserDefaults.standard
        let settings = AppSettings(defaults: defaults)
        let localStore = LocalKeyStore(defaults: defaults)
        let keyRepository = KeyRepository(defaults: defaults, localStore: localStore)
        let cache = UsageCache(defaults: defaults)
        let apiClient = UsageAPIClient(session: .shared, mapper: UsageMapper())
        let alertEvaluator = AlertEvaluator(defaults: defaults)
        let notificationSender = AuthorizationCachingNotificationSender(
            sender: UserNotificationSender()
        )
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
            updateService: GitHubUpdateService(),
            updateCheckScheduler: UpdateCheckScheduler()
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
        // 更新检查不应阻塞首次显示菜单栏或用量刷新。
        updateCheckScheduler.start { [weak self] in
            Task { @MainActor [weak self] in
                await self?.checkForUpdates()
            }
        }
        Task { [weak self] in
            await self?.checkForUpdates()
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
        await Task.yield()
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
        guard updateStatus != .checking, updateStatus != .downloading else { return }
        let availableUpdate: AppUpdate?
        if case let .available(update) = updateStatus {
            availableUpdate = update
        } else {
            availableUpdate = nil
        }
        updateStatus = .checking
        do {
            updateStatus = try await updateService.checkForUpdate().map(AppUpdateStatus.available) ?? .idle
        } catch is CancellationError {
            updateStatus = availableUpdate.map(AppUpdateStatus.available) ?? .idle
        } catch {
            updateStatus = availableUpdate.map(AppUpdateStatus.available) ?? .failed("检查更新失败，请稍后重试")
        }
    }

    func installAvailableUpdate() async {
        guard case let .available(update) = updateStatus else { return }
        updateStatus = .downloading
        do {
            let dmgURL = try await updateService.download(update)
            try UpdateInstaller.install(dmgURL: dmgURL)
        } catch is CancellationError {
            updateStatus = .available(update)
        } catch let error as UpdateServiceError {
            updateStatus = .failed(updateErrorDescription(error))
        } catch {
            updateStatus = .failed("安装更新失败，请下载后手动安装")
        }
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
    }
}

private extension AppEnvironment {
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
