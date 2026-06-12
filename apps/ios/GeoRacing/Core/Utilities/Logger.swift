import Foundation
import os

struct Logger {
    private static let logger = os.Logger(subsystem: Bundle.main.bundleIdentifier ?? "com.georacing", category: "General")

    static func debug(_ message: String) {
        logger.debug("[DEBUG] \(message)")
    }

    static func info(_ message: String) {
        logger.info("[INFO] \(message)")
    }

    static func warning(_ message: String) {
        logger.warning("[WARNING] \(message)")
    }

    static func error(_ message: String) {
        logger.error("[ERROR] \(message)")
    }
}
