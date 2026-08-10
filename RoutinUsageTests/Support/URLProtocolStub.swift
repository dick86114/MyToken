import Foundation

final class URLProtocolStub: URLProtocol, @unchecked Sendable {
    typealias Handler = @Sendable (URLRequest) throws -> (HTTPURLResponse, Data)

    private static let state = State()

    static var lastRequest: URLRequest? {
        state.lastRequest
    }

    static func setHandler(_ handler: @escaping Handler) {
        state.handler = handler
    }

    static func reset() {
        state.reset()
    }

    override class func canInit(with request: URLRequest) -> Bool {
        true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        request
    }

    override func startLoading() {
        URLProtocolStub.state.lastRequest = request

        do {
            let handler = try URLProtocolStub.state.requireHandler()
            let (response, data) = try handler(request)
            client?.urlProtocol(self, didReceive: response, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data)
            client?.urlProtocolDidFinishLoading(self)
        } catch {
            client?.urlProtocol(self, didFailWithError: error)
        }
    }

    override func stopLoading() {}
}

private extension URLProtocolStub {
    final class State: @unchecked Sendable {
        private let lock = NSLock()
        private var storedLastRequest: URLRequest?
        private var storedHandler: Handler?

        var lastRequest: URLRequest? {
            get { lock.withLock { storedLastRequest } }
            set { lock.withLock { storedLastRequest = newValue } }
        }

        var handler: Handler? {
            get { lock.withLock { storedHandler } }
            set { lock.withLock { storedHandler = newValue } }
        }

        func requireHandler() throws -> Handler {
            guard let handler else {
                throw StubError.missingHandler
            }
            return handler
        }

        func reset() {
            lock.withLock {
                storedLastRequest = nil
                storedHandler = nil
            }
        }
    }

    enum StubError: Error {
        case missingHandler
    }
}
