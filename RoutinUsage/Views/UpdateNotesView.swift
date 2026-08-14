import AppKit
import SwiftUI

enum UpdateNotesRenderer {
    static func attributedText(notes: String) -> AttributedString? {
        if let html = htmlAttributedText(notes: notes) {
            return AttributedString(html)
        }
        return try? AttributedString(markdown: notes)
    }

    static func plainText(notes: String) -> String {
        if let html = htmlAttributedText(notes: notes) {
            return html.string.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return notes
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
            .trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private static func htmlAttributedText(notes: String) -> NSAttributedString? {
        guard notes.contains("<"), notes.contains(">"),
              let data = notes.data(using: .utf8) else {
            return nil
        }
        return try? NSAttributedString(
            data: data,
            options: [
                .documentType: NSAttributedString.DocumentType.html,
                .characterEncoding: String.Encoding.utf8.rawValue
            ],
            documentAttributes: nil
        )
    }
}

enum UpdateNotesAccessibility {
    static func label(notes: String) -> String {
        let trimmed = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !trimmed.isEmpty else {
            return "更新日志，此版本未提供更新日志"
        }
        let readableText = UpdateNotesRenderer.plainText(notes: notes)
        return "更新日志，\(readableText)"
    }
}

struct UpdateNotesView: View {
    let notes: String

    var body: some View {
        Group {
            if notes.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                Text("此版本未提供更新日志")
            } else if let attributedText = UpdateNotesRenderer.attributedText(notes: notes) {
                Text(attributedText)
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
