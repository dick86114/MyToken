import SwiftUI

struct UpdateNotesView: View {
    let notes: String

    var body: some View {
        Group {
            if notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text("此版本未提供更新日志")
            } else if let markdown = try? AttributedString(markdown: notes) {
                Text(markdown)
            } else {
                Text(notes)
            }
        }
        .font(.caption)
        .foregroundStyle(.secondary)
        .fixedSize(horizontal: false, vertical: true)
        .accessibilityLabel("更新日志")
    }
}
