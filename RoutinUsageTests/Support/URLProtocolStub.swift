import Foundation

final class URLProtocolStub: URLProtocol, @unchecked Sendable {
    typealias Handler = @Sendable (URLRequest) throws -> (HTTPURLResponse, Data)

    private static let routingHeader = "X-Routin-URLProtocolStub-ID"
    private static let registry = Registry()

    static func makeSession(
        handler: @escaping Handler
    ) -> (session: URLSession, registration: Registration) {
        let identifier = UUID().uuidString
        let state = SessionState(handler: handler)
        registry.register(state, for: identifier)

        let configuration = URLSessionConfiguration.ephemeral
        configuration.protocolClasses = [URLProtocolStub.self]
        configuration.httpAdditionalHeaders = [routingHeader: identifier]
        return (
            URLSession(configuration: configuration),
            Registration(identifier: identifier, state: state)
        )
    }

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        guard
            let identifier = request.value(forHTTPHeaderField: URLProtocolStub.routingHeader),
            let state = URLProtocolStub.registry.state(for: identifier)
        else {
            client?.urlProtocol(self, didFailWithError: StubError.missingRegistration)
            return
        }

        state.lastRequest = capturedRequest(request)

        do {
            let (response, data) = try state.handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}

    private func capturedRequest(_ request: URLRequest) -> URLRequest {
        guard request.httpBody == nil, let bodyStream = request.httpBodyStream else {
            return request
        }

        var captured = request
        bodyStream.open()
        defer { bodyStream.close() }

        var data = Data()
        var buffer = [UInt8](repeating: 0, count: 4_096)
        while bodyStream.hasBytesAvailable {
            let count = bodyStream.read(&buffer, maxLength: buffer.count)
            guard count > 0 else {
                break
            }
            data.append(buffer, count: count)
        }
        captured.httpBody = data
        return captured
    }
}

extension URLProtocolStub {
    final class Registration: @unchecked Sendable {
        fileprivate let identifier: String
        fileprivate let state: SessionState

        fileprivate init(identifier: String, state: SessionState) {
            self.identifier = identifier
            self.state = state
        }

        var lastRequest: URLRequest? {
            state.lastRequest
        }

        var requestCount: Int {
            state.requestCount
        }

        deinit {
            URLProtocolStub.registry.unregister(identifier: identifier, state: state)
        }
    }
}

private extension URLProtocolStub {
    final class SessionState: @unchecked Sendable {
        private let lock = NSLock()
        private var storedLastRequest: URLRequest?
        private var storedRequestCount = 0
        let handler: Handler

        init(handler: @escaping Handler) {
            self.handler = handler
        }

        var lastRequest: URLRequest? {
            get { lock.withLock { storedLastRequest } }
            set {
                lock.withLock {
                    storedLastRequest = newValue
                    storedRequestCount += 1
                }
            }
        }

        var requestCount: Int {
            lock.withLock { storedRequestCount }
        }
    }

    final class Registry: @unchecked Sendable {
        private let lock = NSLock()
        private var states: [String: SessionState] = [:]

        func register(_ state: SessionState, for identifier: String) {
            lock.withLock {
                states[identifier] = state
            }
        }

        func state(for identifier: String) -> SessionState? {
            lock.withLock {
                states[identifier]
            }
        }

        func unregister(identifier: String, state: SessionState) {
            lock.withLock {
                guard states[identifier] === state else {
                    return
                }
                states.removeValue(forKey: identifier)
            }
        }
    }

    enum StubError: Error {
        case missingRegistration
    }
}
