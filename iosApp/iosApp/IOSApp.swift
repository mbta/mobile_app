import FirebaseAnalytics
import FirebaseCore
import os
import Shared
import SwiftPhoenixClient
import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate {
    func application(
        _: UIApplication,
        didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        #if DEBUG
            // Don't configure GA/Firebase for debug builds to reduce pollution of events
            Analytics.setAnalyticsCollectionEnabled(false)
        #endif
        FirebaseApp.configure()
        return true
    }

    /// from https://developer.apple.com/documentation/xcode/supporting-universal-links-in-your-app
    func application(
        _: UIApplication,
        continue userActivity: NSUserActivity,
        restorationHandler _: @escaping ([any UIUserActivityRestoring]?) -> Void
    ) -> Bool {
        // Get URL components from the incoming user activity.
        guard userActivity.activityType == NSUserActivityTypeBrowsingWeb,
              let incomingURL = userActivity.webpageURL,
              let components = NSURLComponents(url: incomingURL, resolvingAgainstBaseURL: true) else {
            return false
        }

        if components.path == "/" {
            // just opening app, let default route happen
            return true
        } else {
            // unhandled path
            debugPrint("Unhandled deep link URI", components)
            return false
        }
    }

    func application(
        _: UIApplication,
        configurationForConnecting connectingSceneSession: UISceneSession,
        options _: UIScene.ConnectionOptions
    ) -> UISceneConfiguration {
        let sceneConfig = UISceneConfiguration(name: nil, sessionRole: connectingSceneSession.role)
        sceneConfig.delegateClass = CustomPopupSceneDelegate.self
        return sceneConfig
    }
}

@main
struct IOSApp: App {
    // register app delegate for Firebase setup and deep linking
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    // When running unit tests or previews, don't mount the entire app which makes real API requests.
    private let isTestOrPreview = ProcessInfo.processInfo.arguments.contains("--dummy-test-app")
        || ProcessInfo.processInfo.environment["XCODE_RUNNING_FOR_PREVIEWS"] != nil

    var body: some Scene {
        WindowGroup {
            if isTestOrPreview {
                DummyTestAppView()
            } else {
                ProductionAppView()
            }
        }
    }
}
