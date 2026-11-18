import SwiftUI

/// 设置窗口的统一布局与表面语言：克制层级、系统字体和稳定的留白节奏。
enum SettingsDesign {
    static let pageHorizontalPadding: CGFloat = 24
    static let pageTopPadding: CGFloat = 22
    static let pageBottomPadding: CGFloat = 30
    static let sectionSpacing: CGFloat = 16
    static let cardCornerRadius: CGFloat = 14
    static let modalCornerRadius: CGFloat = 18
    static let modalPadding: CGFloat = 24
    static let modalHeaderSpacing: CGFloat = 16
    static let controlCornerRadius: CGFloat = 10
    static let fieldHeight: CGFloat = 38
    static let rowIconSize: CGFloat = 28
    static let dividerLeadingInset: CGFloat = 40
    static let closeButtonSize: CGFloat = 30

    static func dividerColor(_ colorScheme: ColorScheme) -> Color {
        Color.primary.opacity(colorScheme == .dark ? 0.085 : 0.06)
    }

    static func fieldFill(_ colorScheme: ColorScheme) -> Color {
        Color.primary.opacity(colorScheme == .dark ? 0.055 : 0.032)
    }

    static func fieldBorder(_ colorScheme: ColorScheme) -> Color {
        Color.primary.opacity(colorScheme == .dark ? 0.13 : 0.10)
    }

    static func hoverFill(_ colorScheme: ColorScheme) -> Color {
        Color.primary.opacity(colorScheme == .dark ? 0.055 : 0.035)
    }
}

enum SettingsTypography {
    static let pageTitle = Font.system(size: 24, weight: .semibold)
    static let modalTitle = Font.system(size: 20, weight: .semibold)
    static let sectionTitle = Font.system(size: 13.5, weight: .semibold)
    static let rowTitle = Font.system(size: 13.5, weight: .semibold)
    static let body = Font.system(size: 12.5)
    static let secondary = Font.system(size: 11.5)
    static let caption = Font.system(size: 10.5, weight: .medium)
    static let button = Font.system(size: 12, weight: .semibold)
    static let numeric = Font.system(size: 12.5, design: .monospaced)
}

/// 设置页统一的滚动容器，保证四个页面的首屏留白和底部呼吸感一致。
struct SettingsPageScroll<Content: View>: View {
    private let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: SettingsDesign.sectionSpacing) {
                content
            }
            .padding(.horizontal, SettingsDesign.pageHorizontalPadding)
            .padding(.top, SettingsDesign.pageTopPadding)
            .padding(.bottom, SettingsDesign.pageBottomPadding)
            .frame(maxWidth: .infinity, alignment: .leading)
        }
    }
}

/// Apple 风格的分组面板：标题负责说明，行内容负责操作，避免每项都变成独立小卡片。
struct SettingsGroup<Content: View>: View {
    private let title: String?
    private let subtitle: String?
    private let content: Content

    init(
        title: String? = nil,
        subtitle: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.subtitle = subtitle
        self.content = content()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if title != nil || subtitle != nil {
                VStack(alignment: .leading, spacing: 3) {
                    if let title {
                        Text(title)
                            .font(SettingsTypography.sectionTitle)
                            .tracking(-0.05)
                    }

                    if let subtitle {
                        Text(subtitle)
                            .font(SettingsTypography.secondary)
                            .foregroundStyle(.secondary)
                            .fixedSize(horizontal: false, vertical: true)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.top, 14)
                .padding(.bottom, 8)
                .accessibilityElement(children: .combine)
                .accessibilityAddTraits(.isHeader)
            }

            content
                .padding(.horizontal, 16)
                .padding(.vertical, 5)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .liquidGlassSurface(cornerRadius: SettingsDesign.cardCornerRadius)
    }
}

/// 设置行只保留一个清晰图标、标题/说明和最右侧操作，和控制中心的分层方式一致。
struct SettingsRow<Accessory: View>: View {
    private let systemImage: String
    private let title: String
    private let detail: String?
    private let tint: Color?
    private let accessory: Accessory

    @Environment(\.colorScheme) private var colorScheme

    init(
        systemImage: String,
        title: String,
        detail: String? = nil,
        tint: Color? = nil,
        @ViewBuilder accessory: () -> Accessory
    ) {
        self.systemImage = systemImage
        self.title = title
        self.detail = detail
        self.tint = tint
        self.accessory = accessory()
    }

    var body: some View {
        HStack(alignment: .center, spacing: 12) {
            SettingsIconBadge(
                systemImage: systemImage,
                tint: tint ?? Color.accentColor
            )

            VStack(alignment: .leading, spacing: 3) {
                Text(title)
                    .font(SettingsTypography.rowTitle)

                if let detail {
                    Text(detail)
                        .font(SettingsTypography.secondary)
                        .foregroundStyle(.secondary)
                        .fixedSize(horizontal: false, vertical: true)
                }
            }
            .frame(maxWidth: 320, alignment: .leading)
            .layoutPriority(1)

            Spacer(minLength: 16)

            accessory
                .layoutPriority(2)
        }
        .padding(.vertical, 10)
        .accessibilityElement(children: .contain)
        .accessibilityLabel(detail.map { "\(title)，\($0)" } ?? title)
    }
}

struct SettingsIconBadge: View {
    let systemImage: String
    let tint: Color

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        RoundedRectangle(cornerRadius: 7, style: .continuous)
            .fill(tint.opacity(colorScheme == .dark ? 0.17 : 0.11))
            .frame(width: SettingsDesign.rowIconSize, height: SettingsDesign.rowIconSize)
            .overlay {
                Image(systemName: systemImage)
                    .font(.system(size: 12, weight: .semibold))
                    .foregroundStyle(tint)
            }
            .overlay {
                RoundedRectangle(cornerRadius: 7, style: .continuous)
                    .strokeBorder(tint.opacity(colorScheme == .dark ? 0.20 : 0.13), lineWidth: 0.8)
            }
            .accessibilityHidden(true)
    }
}

struct SettingsRowDivider: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        Rectangle()
            .fill(SettingsDesign.dividerColor(colorScheme))
            .frame(height: 1)
            .padding(.leading, SettingsDesign.dividerLeadingInset)
            .accessibilityHidden(true)
    }
}

/// 所有轻量按钮共享按下即反馈，避免等待 mouse-up 才出现变化。
struct SettingsPressFeedbackButtonStyle: ButtonStyle {
    var pressedScale: CGFloat = 0.98

    @Environment(\.accessibilityReduceMotion) private var reduceMotion

    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .scaleEffect(
                configuration.isPressed && !reduceMotion
                    ? pressedScale
                    : 1
            )
            .opacity(configuration.isPressed ? 0.82 : 1)
            .animation(.easeOut(duration: 0.1), value: configuration.isPressed)
    }
}

struct SettingsCloseButton: View {
    let action: () -> Void
    var help: String = "关闭"
    var accessibilityLabel: String = "关闭"

    var body: some View {
        SettingsHoverIconButton(
            systemImage: "xmark",
            size: SettingsDesign.closeButtonSize,
            iconSize: 11,
            hoverScale: 1.14,
            hoverRotationDegrees: -8,
            help: help,
            accessibilityLabel: accessibilityLabel,
            action: action
        )
        .keyboardShortcut(.cancelAction)
    }
}

struct SettingsHoverIconButton: View {
    let systemImage: String
    var size: CGFloat = 28
    var iconSize: CGFloat = 12
    var tint: Color = .secondary
    var hoverScale: CGFloat = 1.06
    var hoverRotationDegrees: Double = 0
    let help: String
    let accessibilityLabel: String
    let action: () -> Void

    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.accessibilityReduceMotion) private var reduceMotion
    @State private var isHovered = false

    var body: some View {
        Button(action: action) {
            Image(systemName: systemImage)
                .font(.system(size: iconSize, weight: .semibold))
                .foregroundStyle(isHovered ? Color.primary : tint)
                .frame(width: size, height: size)
                .background {
                    Circle()
                        .fill(
                            Color.primary.opacity(
                                isHovered
                                    ? (colorScheme == .dark ? 0.10 : 0.065)
                                    : 0
                            )
                        )
                }
                .scaleEffect(
                    isHovered && !reduceMotion ? hoverScale : 1
                )
                .rotationEffect(
                    .degrees(
                        isHovered && !reduceMotion
                            ? hoverRotationDegrees
                            : 0
                    )
                )
                .shadow(
                    color: .black.opacity(isHovered ? 0.10 : 0),
                    radius: isHovered ? 4 : 0,
                    y: 1
                )
                .contentShape(Circle())
        }
        .buttonStyle(SettingsPressFeedbackButtonStyle(pressedScale: 0.94))
        .onHover { isHovered = $0 }
        .animation(
            reduceMotion
                ? nil
                : .spring(response: 0.28, dampingFraction: 0.72),
            value: isHovered
        )
        .help(help)
        .accessibilityLabel(accessibilityLabel)
    }
}

struct SettingsModalTitle: View {
    let title: String
    let subtitle: String?

    init(_ title: String, subtitle: String? = nil) {
        self.title = title
        self.subtitle = subtitle
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 3) {
            Text(title)
                .font(SettingsTypography.modalTitle)
                .tracking(-0.2)
                .lineLimit(2)
                .accessibilityAddTraits(.isHeader)

            if let subtitle {
                Text(subtitle)
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
    }
}

struct SettingsSectionHeader: View {
    let title: String
    var systemImage: String?
    var tint: Color?
    var trailingText: String?

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        HStack(alignment: .center, spacing: 8) {
            if let systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 12.5, weight: .semibold))
                    .foregroundStyle(tint ?? Color.accentColor)
            }

            Text(title)
                .font(SettingsTypography.sectionTitle)

            Spacer(minLength: 12)

            if let trailingText {
                Text(trailingText)
                    .font(SettingsTypography.caption)
                    .monospacedDigit()
                    .foregroundStyle(.tertiary)
            }
        }
        .accessibilityElement(children: .combine)
        .accessibilityAddTraits(.isHeader)
    }
}

struct SettingsPanel<Content: View>: View {
    let title: String?
    let systemImage: String?
    let trailingText: String?
    private let content: Content

    @Environment(\.colorScheme) private var colorScheme

    init(
        title: String? = nil,
        systemImage: String? = nil,
        trailingText: String? = nil,
        @ViewBuilder content: () -> Content
    ) {
        self.title = title
        self.systemImage = systemImage
        self.trailingText = trailingText
        self.content = content()
    }

    var body: some View {
        let shape = RoundedRectangle(
            cornerRadius: SettingsDesign.cardCornerRadius,
            style: .continuous
        )

        VStack(alignment: .leading, spacing: 14) {
            if let title {
                SettingsSectionHeader(
                    title: title,
                    systemImage: systemImage,
                    trailingText: trailingText
                )
            }

            content
        }
        .padding(18)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            shape.fill(CompactPopoverPalette.surface(.card, colorScheme))
        }
        .overlay {
            shape.strokeBorder(
                SettingsDesign.fieldBorder(colorScheme),
                lineWidth: 0.8
            )
        }
    }
}

struct SettingsFieldLabel: View {
    let text: String

    var body: some View {
        Text(text)
            .font(.system(size: 11.5, weight: .medium))
            .foregroundStyle(.secondary)
    }
}

struct SettingsHintText: View {
    let text: String
    var tint: Color = .secondary

    var body: some View {
        HStack(alignment: .top, spacing: 5) {
            Image(systemName: "info.circle")
                .font(.system(size: 10, weight: .medium))
                .padding(.top, 1)

            Text(text)
                .font(SettingsTypography.secondary)
                .fixedSize(horizontal: false, vertical: true)
        }
        .foregroundStyle(tint)
    }
}

struct SettingsFieldSurface<Content: View>: View {
    let isFocused: Bool
    let hasError: Bool
    let accentColor: Color
    var height: CGFloat = SettingsDesign.fieldHeight
    private let content: Content

    @Environment(\.colorScheme) private var colorScheme

    init(
        isFocused: Bool = false,
        hasError: Bool = false,
        accentColor: Color = .accentColor,
        height: CGFloat = SettingsDesign.fieldHeight,
        @ViewBuilder content: () -> Content
    ) {
        self.isFocused = isFocused
        self.hasError = hasError
        self.accentColor = accentColor
        self.height = height
        self.content = content()
    }

    var body: some View {
        let shape = RoundedRectangle(
            cornerRadius: SettingsDesign.controlCornerRadius,
            style: .continuous
        )

        HStack(spacing: 8) {
            content
        }
        .padding(.horizontal, 11)
        .frame(minHeight: height)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            shape.fill(SettingsDesign.fieldFill(colorScheme))
        }
        .overlay {
            shape.strokeBorder(
                hasError
                    ? CompactPopoverPalette.criticalColor.opacity(0.82)
                    : (isFocused
                        ? accentColor.opacity(0.78)
                        : SettingsDesign.fieldBorder(colorScheme)),
                lineWidth: hasError || isFocused ? 1.5 : 1
            )
        }
        .shadow(
            color: hasError
                ? CompactPopoverPalette.criticalColor.opacity(0.10)
                : (isFocused ? accentColor.opacity(0.12) : .clear),
            radius: 4
        )
        .animation(.easeOut(duration: 0.16), value: isFocused || hasError)
    }
}

struct SettingsLabeledTextField: View {
    let label: String?
    @Binding var text: String
    let prompt: String
    var isSecret = false
    var visibility: Binding<Bool>?
    var canReveal = true
    var suffix: String?
    var helpText: String?
    var errorText: String?
    var accentColor: Color = .accentColor
    var autoFocus = false
    var onSubmit: (() -> Void)?

    @FocusState private var isFocused: Bool

    init(
        label: String? = nil,
        text: Binding<String>,
        prompt: String,
        isSecret: Bool = false,
        visibility: Binding<Bool>? = nil,
        canReveal: Bool = true,
        suffix: String? = nil,
        helpText: String? = nil,
        errorText: String? = nil,
        accentColor: Color = .accentColor,
        autoFocus: Bool = false,
        onSubmit: (() -> Void)? = nil
    ) {
        self.label = label
        _text = text
        self.prompt = prompt
        self.isSecret = isSecret
        self.visibility = visibility
        self.canReveal = canReveal
        self.suffix = suffix
        self.helpText = helpText
        self.errorText = errorText
        self.accentColor = accentColor
        self.autoFocus = autoFocus
        self.onSubmit = onSubmit
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 6) {
            if let label {
                SettingsFieldLabel(text: label)
            }

            SettingsFieldSurface(
                isFocused: isFocused,
                hasError: errorText != nil,
                accentColor: accentColor
            ) {
                inputField

                if isSecret, let visibility {
                    let fieldName = label ?? "内容"
                    SettingsHoverIconButton(
                        systemImage: visibility.wrappedValue ? "eye" : "eye.slash",
                        size: 24,
                        iconSize: 11,
                        tint: canReveal
                            ? Color.secondary
                            : Color.secondary.opacity(0.35),
                        help: visibility.wrappedValue ? "隐藏\(fieldName)" : "显示\(fieldName)",
                        accessibilityLabel: visibility.wrappedValue
                            ? "隐藏\(fieldName)"
                            : "显示\(fieldName)",
                        action: { visibility.wrappedValue.toggle() }
                    )
                    .disabled(!canReveal)
                }

                if let suffix {
                    Rectangle()
                        .fill(SettingsDesign.fieldBorder(colorScheme))
                        .frame(width: 1, height: 18)

                    Text(suffix)
                        .font(SettingsTypography.secondary)
                        .foregroundStyle(.secondary)
                        .padding(.trailing, 2)
                }
            }

            if let errorText {
                Label(errorText, systemImage: "exclamationmark.circle.fill")
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(CompactPopoverPalette.criticalColor)
                    .fixedSize(horizontal: false, vertical: true)
                    .accessibilityLabel("错误：\(errorText)")
            } else if let helpText {
                Text(helpText)
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .task {
            guard autoFocus else { return }
            await Task.yield()
            isFocused = true
        }
    }

    @Environment(\.colorScheme) private var colorScheme

    @ViewBuilder
    private var inputField: some View {
        if isSecret, let visibility, !visibility.wrappedValue {
            SecureField("", text: $text, prompt: Text(prompt))
                .textFieldStyle(.plain)
                .font(.system(size: 13))
                .focused($isFocused)
                .textContentType(.password)
                .onSubmit { onSubmit?() }
                .accessibilityLabel(label ?? prompt)
        } else {
            TextField("", text: $text, prompt: Text(prompt))
                .textFieldStyle(.plain)
                .font(.system(size: 13))
                .focused($isFocused)
                .onSubmit { onSubmit?() }
                .accessibilityLabel(label ?? prompt)
        }
    }
}

enum SettingsNoticeTone: Equatable {
    case neutral
    case info
    case success
    case warning
    case error

    func color(_ colorScheme: ColorScheme) -> Color {
        switch self {
        case .neutral:
            return .secondary
        case .info:
            return .accentColor
        case .success:
            return CompactPopoverPalette.positive(colorScheme)
        case .warning:
            return CompactPopoverPalette.warningColor
        case .error:
            return CompactPopoverPalette.criticalColor
        }
    }

    var defaultSystemImage: String {
        switch self {
        case .neutral:
            return "minus.circle.fill"
        case .info:
            return "info.circle.fill"
        case .success:
            return "checkmark.circle.fill"
        case .warning:
            return "exclamationmark.triangle.fill"
        case .error:
            return "xmark.octagon.fill"
        }
    }
}

struct SettingsNotice: View {
    let tone: SettingsNoticeTone
    let message: String
    var title: String?
    var systemImage: String?

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let color = tone.color(colorScheme)
        let shape = RoundedRectangle(cornerRadius: 12, style: .continuous)

        HStack(alignment: .top, spacing: 9) {
            Image(systemName: systemImage ?? tone.defaultSystemImage)
                .font(.system(size: 12, weight: .semibold))
                .foregroundStyle(color)
                .padding(.top, 1)
                .accessibilityHidden(true)

            VStack(alignment: .leading, spacing: 2) {
                if let title {
                    Text(title)
                        .font(.system(size: 12, weight: .semibold))
                }

                Text(message)
                    .font(SettingsTypography.secondary)
                    .foregroundStyle(.secondary)
                    .fixedSize(horizontal: false, vertical: true)
            }

            Spacer(minLength: 0)
        }
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .frame(maxWidth: .infinity, alignment: .leading)
        .background {
            shape.fill(color.opacity(colorScheme == .dark ? 0.12 : 0.07))
        }
        .overlay {
            shape.strokeBorder(color.opacity(colorScheme == .dark ? 0.22 : 0.14), lineWidth: 0.8)
        }
        .accessibilityElement(children: .combine)
    }
}

struct SettingsStatusPill: View {
    let text: String
    let tone: SettingsNoticeTone
    var systemImage: String?

    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let color = tone.color(colorScheme)

        HStack(spacing: 5) {
            if let systemImage {
                Image(systemName: systemImage)
                    .font(.system(size: 9, weight: .semibold))
            } else {
                Circle()
                    .fill(color)
                    .frame(width: 6, height: 6)
            }

            Text(text)
                .font(.system(size: 10.5, weight: .semibold))
        }
        .foregroundStyle(color)
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(color.opacity(0.10), in: Capsule())
        .overlay {
            Capsule()
                .strokeBorder(color.opacity(0.18), lineWidth: 0.8)
        }
        .accessibilityElement(children: .combine)
    }
}

/// 详情区用极轻的品牌色光晕建立空间感，不叠加新的玻璃层。
struct SettingsDetailBackdrop: View {
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        ZStack(alignment: .topTrailing) {
            Color.clear

            RadialGradient(
                colors: [
                    Color.accentColor
                        .opacity(colorScheme == .dark ? 0.075 : 0.05),
                    .clear
                ],
                center: .topTrailing,
                startRadius: 0,
                endRadius: 360
            )
        }
        .ignoresSafeArea()
        .allowsHitTesting(false)
        .accessibilityHidden(true)
    }
}
