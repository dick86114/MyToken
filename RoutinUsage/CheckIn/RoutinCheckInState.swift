import Foundation

enum RoutinCheckInFailure: Equatable, Sendable {
    case network
    case pageChanged
    case cancelled
}

enum RoutinCheckInOutcome: Equatable, Sendable {
    case succeeded
    case alreadyCheckedIn
    case needsLogin
    case cannotConfirm
    case suspended
}

enum RoutinCheckInState: Equatable, Sendable {
    case idle
    case needsLogin
    case loggingIn
    case checkingIn
    case succeeded
    case alreadyCheckedIn
    case failed(RoutinCheckInFailure)

    var statusText: String {
        switch self {
        case .idle:
            return "尚未登录 Routin"
        case .needsLogin:
            return "请先登录 Routin"
        case .loggingIn:
            return "正在登录 Routin"
        case .checkingIn:
            return "正在签到"
        case .succeeded:
            return "签到成功"
        case .alreadyCheckedIn:
            return "今天已签到"
        case let .failed(failure):
            switch failure {
            case .network:
                return "网络错误，请稍后重试"
            case .pageChanged:
                return "无法确认签到结果，请在 Routin 页面中检查"
            case .cancelled:
                return "登录已取消"
            }
        }
    }

    var isBusy: Bool {
        self == .loggingIn || self == .checkingIn
    }

    var isTerminalResult: Bool {
        switch self {
        case .succeeded, .alreadyCheckedIn, .failed:
            return true
        case .idle, .needsLogin, .loggingIn, .checkingIn:
            return false
        }
    }

    var requiresLogin: Bool {
        self == .idle || self == .needsLogin || self == .failed(.cancelled)
    }
}
