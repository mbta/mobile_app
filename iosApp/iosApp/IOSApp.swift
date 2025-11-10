import FirebaseAnalytics
import FirebaseCore
import FirebaseMessaging
import os
import Shared
import SwiftPhoenixClient
import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    func application(
        _: UIApplication,
        didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        #if DEBUG
            // Don't configure GA/Firebase for debug builds to reduce pollution of events
            Analytics.setAnalyticsCollectionEnabled(false)
        #endif
        FirebaseApp.configure()

        UNUserNotificationCenter.current().delegate = self

        Messaging.messaging().delegate = self

        return true
    }

    func application(_: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
    }

    func application(_: UIApplication,
                     didReceiveRemoteNotification userInfo: [AnyHashable: Any]) async -> UIBackgroundFetchResult {
        Messaging.messaging().appDidReceiveMessage(userInfo)
        return .noData
    }

    func userNotificationCenter(_: UNUserNotificationCenter,
                                willPresent notification: UNNotification) async -> UNNotificationPresentationOptions {
        let userInfo = notification.request.content.userInfo
        Messaging.messaging().appDidReceiveMessage(userInfo)
        return [[.banner, .list, .sound]]
    }

    func userNotificationCenter(_: UNUserNotificationCenter, didReceive response: UNNotificationResponse) async {
        let userInfo = response.notification.request.content.userInfo
        Messaging.messaging().appDidReceiveMessage(userInfo)
    }
}

@main
struct IOSApp: App {
    // register app delegate for Firebase setup
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
