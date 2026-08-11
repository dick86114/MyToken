import SwiftUI

enum UpdateNotesAccessibility {
    static func label(notes: String) -> String {
        let trimmed = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return "更新日志，此版本未提供更新日志"
        }
        let readableText = notes
            .components(separatedBy: .newlines)
            .map { $0.trimmingCharacters(in: .whitespaces) }
            .filter { !$0.isEmpty }
            .map { line in
                if let markdown = try? AttributedString(markdown: line) {
                    return String(markdown.characters)
                }
                return line
            }
            .joined(separator: "\n")
        return "更新日志，\(readableText)"
    }
}

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
        .accessibilityLabel(UpdateNotesAccessibility.label(notes: notes))
    }
}
