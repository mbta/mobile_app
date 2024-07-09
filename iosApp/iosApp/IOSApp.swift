import AppcuesKit
import FirebaseCore
import os
import shared
import SwiftPhoenixClient
import SwiftUI

class AppDelegate: NSObject, UIApplicationDelegate {
    var appcues: Appcues?

    func application(
        _: UIApplication,
        didFinishLaunchingWithOptions _: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        // Don't configure GA/Firebase for debug builds to reduce pollution of events
        #if !DEBUG
            FirebaseApp.configure()
        #endif

        // Don't configure Appcues for debug builds to not waste active user slots
        #if !DEBUG
            let bundle = Bundle.main
            let info = bundle.infoDictionary
            if let info, let appcuesAccountID = info["APPCUES_ACCOUNT_ID"] as? String,
               let appcuesAppID = info["APPCUES_APP_ID"] as? String, !appcuesAccountID.isEmpty, !appcuesAppID.isEmpty {
                let appcuesConfig = Appcues.Config(
                    accountID: appcuesAccountID,
                    applicationID: appcuesAppID
                )

                appcues = Appcues(config: appcuesConfig)
            } else {
                Logger().info("Appcues config not set, skipping initialization")
            }
        #endif
        return true
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
                    .onAppear {
                        delegate.appcues?.anonymous()
                    }
            }
        }
    }
}
