import Foundation

final class WidgetSnapshotStore: @unchecked Sendable {
    let directory: URL
    let filename: String

    private let fileManager: FileManager
    private let primaryURL: URL
    private let previousURL: URL

    init(
        directory: URL,
        filename: String = "widget-snapshot-v1.json",
        fileManager: FileManager = .default
    ) {
        self.directory = directory
        self.filename = filename
        self.fileManager = fileManager
        primaryURL = directory.appendingPathComponent(filename)
        previousURL = directory.appendingPathComponent("\(filename).previous")
    }

    func load() throws -> WidgetSnapshotBundle? {
        if let bundle = try load(from: primaryURL) {
            return bundle
        }
        return try load(from: previousURL)
    }

    func save(_ bundle: WidgetSnapshotBundle) throws {
        try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
        let data = try JSONEncoder().encode(bundle)
        try preservePreviousSnapshot()
        try data.write(to: primaryURL, options: .atomic)
    }

    private func load(from url: URL) throws -> WidgetSnapshotBundle? {
        guard fileManager.fileExists(atPath: url.path) else {
            return nil
        }
        let data = try Data(contentsOf: url)
        return try? JSONDecoder().decode(WidgetSnapshotBundle.self, from: data)
    }

    private func preservePreviousSnapshot() throws {
        guard fileManager.fileExists(atPath: primaryURL.path) else {
            return
        }
        if fileManager.fileExists(atPath: previousURL.path) {
            try fileManager.removeItem(at: previousURL)
        }
        try fileManager.copyItem(at: primaryURL, to: previousURL)
    }
}
