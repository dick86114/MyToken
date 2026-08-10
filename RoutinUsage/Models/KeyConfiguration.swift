import Foundation

struct KeyConfiguration: Codable, Equatable, Identifiable, Sendable {
    let id: UUID
    let name: String
    let keySuffix: String
    let sortOrder: Int
}
