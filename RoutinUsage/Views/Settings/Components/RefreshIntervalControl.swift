import SwiftUI

struct RefreshIntervalControl: View {
    @Binding var minutes: Int

    @State private var showsCustomField: Bool
    @State private var customText: String
    @FocusState private var isCustomFieldFocused: Bool

    private let presetMinutes = AppSettings.presetRefreshMinutes

    init(minutes: Binding<Int>) {
        _minutes = minutes
        let isCustom = !AppSettings.presetRefreshMinutes.contains(minutes.wrappedValue)
        _showsCustomField = State(initialValue: isCustom)
        _customText = State(initialValue: String(minutes.wrappedValue))
    }

    var body: some View {
        HStack(spacing: 10) {
            SettingsSegmentedControl(
                options: RefreshIntervalOption.allCases,
                selection: selectionBinding,
                title: \.title,
                accessibilityLabel: "自动刷新间隔"
            )

            if showsCustomField {
                HStack(spacing: 4) {
                    TextField("自定义", text: $customText)
                        .textFieldStyle(.roundedBorder)
                        .multilineTextAlignment(.trailing)
                        .font(.system(size: 12, design: .monospaced))
                        .monospacedDigit()
                        .frame(width: 52)
                        .focused($isCustomFieldFocused)
                        .accessibilityLabel("自定义刷新分钟数")

                    Text("分钟")
                        .font(.system(size: 11))
                        .foregroundStyle(.secondary)
                }
                .foregroundStyle(isCustomTextValid ? Color.primary : Color.red)
            }
        }
        .onChange(of: minutes) { _, updatedMinutes in
            showsCustomField = !presetMinutes.contains(updatedMinutes)
            if showsCustomField, parsedCustomMinutes != updatedMinutes {
                customText = String(updatedMinutes)
            }
        }
        .onChange(of: customText) { _, text in
            if let updatedMinutes = parsedCustomMinutes, updatedMinutes != minutes {
                minutes = updatedMinutes
            }
        }
    }

    private enum RefreshIntervalOption: CaseIterable, Hashable {
        case oneMinute
        case fiveMinutes
        case fifteenMinutes
        case thirtyMinutes
        case custom

        var title: String {
            switch self {
            case .oneMinute: return "1 分钟"
            case .fiveMinutes: return "5 分钟"
            case .fifteenMinutes: return "15 分钟"
            case .thirtyMinutes: return "30 分钟"
            case .custom: return "自定义"
            }
        }
    }

    private var selection: RefreshIntervalOption {
        if showsCustomField { return .custom }
        switch minutes {
        case 1: return .oneMinute
        case 5: return .fiveMinutes
        case 15: return .fifteenMinutes
        case 30: return .thirtyMinutes
        default: return .custom
        }
    }

    private var selectionBinding: Binding<RefreshIntervalOption> {
        Binding(
            get: { selection },
            set: { select($0) }
        )
    }

    private func select(_ option: RefreshIntervalOption) {
        switch option {
        case .oneMinute: selectPreset(1)
        case .fiveMinutes: selectPreset(5)
        case .fifteenMinutes: selectPreset(15)
        case .thirtyMinutes: selectPreset(30)
        case .custom:
            showsCustomField = true
            customText = String(minutes)
            isCustomFieldFocused = true
        }
    }

    private func selectPreset(_ value: Int) {
        showsCustomField = false
        isCustomFieldFocused = false
        minutes = value
    }

    private var parsedCustomMinutes: Int? {
        let value = Int(customText.trimmingCharacters(in: .whitespaces))
        return AppSettings.refreshMinutesRange.contains(value ?? 0) ? value : nil
    }

    private var isCustomTextValid: Bool {
        parsedCustomMinutes != nil
    }
}
