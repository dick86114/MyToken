import CoreGraphics

/// 方案 A 的视觉层级契约：颜色语义、材质、阴影和圆角都从这里取值。
enum PopoverVisualPolicy {
    enum SurfaceRole: Equatable {
        case window
        case card
        case metric
        case control
        case modal
    }

    enum Material: Equatable {
        case windowGlass
        case solid
        case modalGlass
    }

    enum AccentRole: Equatable {
        case brand
        case warning
        case critical
    }

    enum BalanceAccentRole: Equatable {
        case positive
        case critical
        case secondary
    }

    enum HealthAccentRole: Equatable {
        case brand
        case warning
        case critical
        case secondary
    }

    enum RadiusLevel: Equatable {
        case outer
        case inner
        case button
    }

    static func material(for role: SurfaceRole) -> Material {
        switch role {
        case .window:
            return .windowGlass
        case .card, .metric, .control:
            return .solid
        case .modal:
            return .modalGlass
        }
    }

    static func allowsShadow(for role: SurfaceRole) -> Bool {
        role == .modal
    }

    static func cornerRadius(for level: RadiusLevel) -> CGFloat {
        switch level {
        case .outer:
            return 14
        case .inner, .button:
            return 10
        }
    }

    static func gaugeAccentRole(for tone: UsageMetricTone) -> AccentRole {
        switch tone {
        case .normal:
            return .brand
        case .warning:
            return .warning
        case .critical:
            return .critical
        }
    }

    static func balanceAccentRole(for state: UsageMetricHealthState) -> BalanceAccentRole {
        switch state {
        case .normal:
            return .positive
        case .warning, .critical, .unavailable, .stale:
            return .critical
        case .unknown:
            return .secondary
        }
    }

    static func healthAccentRole(for state: UsageMetricHealthState) -> HealthAccentRole {
        switch state {
        case .normal:
            return .brand
        case .warning:
            return .warning
        case .critical, .unavailable:
            return .critical
        case .stale, .unknown:
            return .secondary
        }
    }
}
