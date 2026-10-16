import Foundation

/// 在进入周期性同步前验证 Group Container 可写，避免每轮刷新重复失败和写日志。
enum WidgetSnapshotAvailability {
    static func validateDirectory(
        _ directory: URL?,
        fileManager: FileManager = .default
    ) -> URL? {
        guard let directory else { return nil }

        let probeName = "mytoken-widget-availability-\(UUID().uuidString)"
        let probeURL = directory.appendingPathComponent(probeName)
        do {
            try fileManager.createDirectory(at: directory, withIntermediateDirectories: true)
            try Data("ok".utf8).write(to: probeURL, options: .atomic)
            let data = try Data(contentsOf: probeURL)
            try fileManager.removeItem(at: probeURL)
            return data == Data("ok".utf8) ? directory : nil
        } catch {
            try? fileManager.removeItem(at: probeURL)
            return nil
        }
    }
}
