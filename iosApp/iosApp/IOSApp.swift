import os
import shared
import SwiftPhoenixClient
import SwiftUI

@main
struct IOSApp: App {
    var body: some Scene {
        WindowGroup {
            if ProcessInfo.processInfo.arguments.contains("--dummy-test-app")
                || ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] != nil {
                DummyTestAppView()
            } else {
                ProductionAppView()
            }
        }
    }
}
