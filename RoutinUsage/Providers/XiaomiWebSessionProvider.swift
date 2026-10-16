import Foundation

/// 只在用户手动登录或静默续期确实需要时创建 WKWebView。
@MainActor
final class XiaomiWebSessionProvider {
    private let factory: @MainActor () -> XiaomiWebSession
    private var storedSession: XiaomiWebSession?

    init(factory: @escaping @MainActor () -> XiaomiWebSession) {
        self.factory = factory
    }

    var hasSession: Bool {
        storedSession != nil
    }

    var existingSession: XiaomiWebSession? {
        storedSession
    }

    func sessionForLogin() -> XiaomiWebSession {
        if let storedSession {
            return storedSession
        }
        let session = factory()
        storedSession = session
        return session
    }

    func releaseSession(_ session: XiaomiWebSession) {
        guard storedSession === session else {
            return
        }
        storedSession = nil
    }
}
