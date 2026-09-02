import Foundation

enum GLMJSONValue: Codable, Equatable, Sendable {
    case object([String: GLMJSONValue])
    case array([GLMJSONValue])
    case string(String)
    case number(Decimal)
    case bool(Bool)
    case null

    init(from decoder: Decoder) throws {
        if let container = try? decoder.container(keyedBy: DynamicKey.self) {
            var object: [String: GLMJSONValue] = [:]
            for key in container.allKeys {
                object[key.stringValue] = try container.decode(GLMJSONValue.self, forKey: key)
            }
            self = .object(object)
            return
        }
        if var container = try? decoder.unkeyedContainer() {
            var values: [GLMJSONValue] = []
            while !container.isAtEnd {
                values.append(try container.decode(GLMJSONValue.self))
            }
            self = .array(values)
            return
        }
        let single = try decoder.singleValueContainer()
        if single.decodeNil() {
            self = .null
        } else if let value = try? single.decode(Bool.self) {
            self = .bool(value)
        } else if let value = try? single.decode(Decimal.self) {
            self = .number(value)
        } else {
            self = .string(try single.decode(String.self))
        }
    }

    func numberValue() -> Decimal? {
        switch self {
        case let .number(value): return value
        case let .string(value): return Decimal(string: value)
        default: return nil
        }
    }

    func stringValue() -> String? {
        if case let .string(value) = self { return value }
        return nil
    }
}

private struct DynamicKey: CodingKey {
    let stringValue: String
    let intValue: Int? = nil

    init?(stringValue: String) { self.stringValue = stringValue }
    init?(intValue: Int) { stringValue = String(intValue) }
}
