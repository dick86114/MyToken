import Foundation
import XCTest

final class DeliveryScriptTests: XCTestCase {
    func test构建脚本从仓库外调用仍固定使用脚本所在仓库() throws {
        let fixture = try makeFixture()
        defer { try? FileManager.default.removeItem(at: fixture.root) }

        let result = try runScript(fixture.script, currentDirectory: fixture.outside)
        XCTAssertEqual(result.status, 0, result.output)
        XCTAssertTrue(
            result.output.contains("仓库根目录：")
                && result.output.contains("/temporary-repository"),
            result.output
        )
        XCTAssertTrue(
            result.output.contains("/temporary-repository/build/dist"),
            result.output
        )
        XCTAssertFalse(result.output.contains(fixture.outside.appendingPathComponent("build").path))
    }

    func test构建目录为外指符号链接时拒绝清理且保留外部文件() throws {
        let fixture = try makeFixture()
        defer { try? FileManager.default.removeItem(at: fixture.root) }
        let externalBuild = fixture.outside.appendingPathComponent("external-build")
        try FileManager.default.createDirectory(
            at: externalBuild,
            withIntermediateDirectories: true
        )
        let sentinel = externalBuild.appendingPathComponent("不可删除.txt")
        try Data("保留".utf8).write(to: sentinel)
        try FileManager.default.createSymbolicLink(
            at: fixture.repository.appendingPathComponent("build"),
            withDestinationURL: externalBuild
        )

        let result = try runScript(fixture.script, currentDirectory: fixture.outside)

        XCTAssertNotEqual(result.status, 0)
        XCTAssertTrue(result.output.contains("build 不能是符号链接"), result.output)
        XCTAssertTrue(FileManager.default.fileExists(atPath: sentinel.path))
    }

    private func makeFixture() throws -> ScriptFixture {
        let root = FileManager.default.temporaryDirectory
            .appendingPathComponent("RoutinUsage-DeliveryScriptTests-\(UUID().uuidString)")
        let repository = root.appendingPathComponent("temporary-repository")
        let scripts = repository.appendingPathComponent("scripts")
        let outside = root.appendingPathComponent("outside-working-directory")
        try FileManager.default.createDirectory(at: scripts, withIntermediateDirectories: true)
        try FileManager.default.createDirectory(at: outside, withIntermediateDirectories: true)
        try Data("name: Fixture\n".utf8).write(
            to: repository.appendingPathComponent("project.yml")
        )

        let testsDirectory = URL(fileURLWithPath: #filePath).deletingLastPathComponent()
        let sourceScript = testsDirectory
            .deletingLastPathComponent()
            .appendingPathComponent("scripts/build-dmg.sh")
        let copiedScript = scripts.appendingPathComponent("build-dmg.sh")
        try FileManager.default.copyItem(at: sourceScript, to: copiedScript)
        try FileManager.default.setAttributes(
            [.posixPermissions: 0o755],
            ofItemAtPath: copiedScript.path
        )
        return ScriptFixture(
            root: root,
            repository: repository,
            outside: outside,
            script: copiedScript
        )
    }

    private func runScript(
        _ script: URL,
        currentDirectory: URL
    ) throws -> (status: Int32, output: String) {
        let process = Process()
        let output = Pipe()
        process.executableURL = URL(fileURLWithPath: "/bin/bash")
        process.arguments = [script.path]
        process.currentDirectoryURL = currentDirectory
        process.standardOutput = output
        process.standardError = output
        var environment = ProcessInfo.processInfo.environment
        environment["ROUTIN_DMG_DRY_RUN"] = "1"
        process.environment = environment
        try process.run()
        process.waitUntilExit()
        let data = output.fileHandleForReading.readDataToEndOfFile()
        return (
            process.terminationStatus,
            String(data: data, encoding: .utf8) ?? ""
        )
    }
}

private struct ScriptFixture {
    let root: URL
    let repository: URL
    let outside: URL
    let script: URL
}
