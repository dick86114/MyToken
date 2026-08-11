import Foundation
import ServiceManagement

protocol LoginItemManaging: Sendable {
    var isEnabled: Bool { get }
    func setEnabled(_ enabled: Bool) throws
}

@MainActor
enum LoginItemSettingSynchronizer {
    static func synchronize(
        settings: AppSettings,
        manager: any LoginItemManaging
    ) {
        settings.launchAtLogin = manager.isEnabled
    }

    static func setEnabled(
        _ enabled: Bool,
        settings: AppSettings,
        manager: any LoginItemManaging
    ) throws {
        do {
            try manager.setEnabled(enabled)
        } catch {
            synchronize(settings: settings, manager: manager)
            throw error
        }
        synchronize(settings: settings, manager: manager)
    }
}

enum LoginItemStatus: Sendable {
    case notRegistered
    case enabled
    case requiresApproval
    case notFound
}

protocol LoginItemServicing: Sendable {
    var status: LoginItemStatus { get }
    func register() throws
    func unregister() throws
}

final class LoginItemManager: LoginItemManaging, @unchecked Sendable {
    private static let lock = NSRecursiveLock()
    private let service: any LoginItemServicing

    init(service: any LoginItemServicing = MainAppLoginItemService()) {
        self.service = service
    }

    var isEnabled: Bool {
        Self.lock.withLock {
            service.status == .enabled
        }
    }

    func setEnabled(_ enabled: Bool) throws {
        try Self.lock.withLock {
            if enabled {
                guard service.status != .enabled else {
                    return
                }
                try service.register()
            } else {
                guard service.status != .notRegistered else {
                    return
                }
                try service.unregister()
            }
        }
    }
}

private final class MainAppLoginItemService: LoginItemServicing, @unchecked Sendable {
    private let service: SMAppService

    init(service: SMAppService = .mainApp) {
        self.service = service
    }

    var status: LoginItemStatus {
        switch service.status {
        case .notRegistered:
            return .notRegistered
        case .enabled:
            return .enabled
        case .requiresApproval:
            return .requiresApproval
        case .notFound:
            return .notFound
        @unknown default:
            return .notFound
        }
    }

    func register() throws {
        try service.register()
    }

    func unregister() throws {
        try service.unregister()
    }
}
