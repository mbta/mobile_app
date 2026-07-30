import FirebaseAnalytics
import FirebaseCore
import FirebaseMessaging
import os
import Shared
import SwiftPhoenixClient
import SwiftUI

@_exported import Inject

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate, MessagingDelegate {
    class NotificationDeepLinkOwner: ObservableObject {
        var notificationDeepLink: DeepLinkState?
    }

    static let notificationDeepLinkOwner: NotificationDeepLinkOwner = .init()

    func application(
        _ app: UIApplication,
        didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        #if DEBUG
            // Don't configure GA/Firebase for debug builds to reduce pollution of events
            Analytics.setAnalyticsCollectionEnabled(false)
        #endif
        FirebaseApp.configure()

        UNUserNotificationCenter.current().delegate = self

        Messaging.messaging().delegate = self

        app.registerForRemoteNotifications()

        return true
    }

    func application(_: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        Messaging.messaging().apnsToken = deviceToken
        Messaging.messaging().register(completion: { _ in })
    }

    func application(
        _: UIApplication,
        didFailToRegisterForRemoteNotificationsWithError error: any Error
    ) {
        print("Failed to register remote notifications: \(error)")
    }

    func application(
        _: UIApplication,
        didReceiveRemoteNotification userInfo: [AnyHashable: Any],
        fetchCompletionHandler completionHandler: @escaping (UIBackgroundFetchResult) -> Void
    ) {
        Messaging.messaging().appDidReceiveMessage(userInfo)
        completionHandler(.noData)
    }

    func messaging(_: Messaging, didReceiveRegistration installationId: String?) {
        FcmInstallationIdContainer.shared.installationId = installationId
    }

    func userNotificationCenter(
        _: UNUserNotificationCenter,
        willPresent notification: UNNotification
    ) async -> UNNotificationPresentationOptions {
        let userInfo = notification.request.content.userInfo
        Messaging.messaging().appDidReceiveMessage(userInfo)
        return [[.banner, .list, .sound]]
    }

    func userNotificationCenter(
        _: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        Messaging.messaging().appDidReceiveMessage(userInfo)

        if let analyticsLabel = userInfo["analytics_label"] as? String {
            AnalyticsProvider.shared.notificationClicked(analyticsLabel: analyticsLabel)
        }

        if let deepLinkPath = userInfo["deep_link_path"] as? String {
            Self.notificationDeepLinkOwner.notificationDeepLink = .companion.from(url: deepLinkPath)
        }
        completionHandler()
    }
}

@main
struct IOSApp: App {
    /// register app delegate for Firebase setup
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    /// When running unit tests or previews, don't mount the entire app which makes real API requests.
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
