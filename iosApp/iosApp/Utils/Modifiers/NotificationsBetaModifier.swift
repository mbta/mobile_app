//
//  NotificationsBetaModifier.swift
//  iosApp
//
//  Created by esimon on 3/17/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import CustomAlert
import Foundation
import Shared
import SwiftUI

struct NotificationsBetaModifier: ViewModifier {
    @State var state: NotificationsBetaViewModel.State?
    @State var showDialog: Bool = false

    @Environment(\.openURL) private var openURL
    @EnvironmentObject var settingsCache: SettingsCache
    var notificationsEnabled: Bool { settingsCache.get(.notifications) }

    var instanceId: String? { InstanceIdCache.shared.instanceId }

    let navEntry: SheetNavigationStackEntry
    let onToastTap: () -> Void
    let onDismissDialog: () -> Void

    let viewModel: INotificationsBetaViewModel
    let toastViewModel: IToastViewModel

    func body(content: Content) -> some View {
        content.task {
            for await models in viewModel.models {
                state = models
            }
        }
        .onAppear {
            viewModel.setInstanceId(instanceId: instanceId)
            viewModel.setNotificationsEnabled(enabled: notificationsEnabled)
            viewModel.setSheetRoute(sheetRoute: navEntry.toSheetRoute())
        }
        .onChange(of: instanceId) { id in viewModel.setInstanceId(instanceId: id) }
        .onChange(of: notificationsEnabled) { enabled in viewModel.setNotificationsEnabled(enabled: enabled) }
        .onChange(of: navEntry) { entry in viewModel.setSheetRoute(sheetRoute: entry.toSheetRoute()) }
        .onChange(of: state?.showBetaToast) { showToast in
            if showToast == true {
                toastViewModel.showToast(toast: .init(
                    message: NOTIFICATIONS_BETA_TOAST_KEY,
                    duration: .indefinite,
                    isTip: false,
                    action: ToastViewModel.ToastActionBodyWithClose(
                        onAction: {
                            onToastTap()
                            viewModel.dismissBetaToast()
                        },
                        onClose: { viewModel.dismissBetaToast() }
                    )
                ))
            }
        }
        .onChange(of: state?.showBetaDialog) { showDialog in
            if showDialog == true { self.showDialog = true }
        }
        .onChange(of: showDialog) { showDialog in
            if !showDialog {
                viewModel.dismissBetaDialog()
                onDismissDialog()
            }
        }
        .customAlert(isPresented: $showDialog) {
            VStack(alignment: .leading, spacing: 12) {
                Text("We depend on riders like you to learn how we can improve!")
                    .font(Typography.headlineSemibold)
                    .foregroundStyle(Color.text)
                Text("May we contact you to share your feedback on Notifications?")
                    .font(Typography.body)
                    .foregroundStyle(Color.text)
            }.padding(.bottom, 12)
        } actions: {
            MultiButton {
                Button(
                    role: .cancel,
                    action: { showDialog = false },
                    label: { Text("No thanks").foregroundStyle(Color.text) }
                )

                Button(
                    action: {
                        Task { @MainActor in
                            if let url = URL(string: "https://www.mbta.com/go-contact") {
                                openURL(url)
                            }
                            showDialog = false
                        }
                    },
                    label: { Text("Yes").foregroundStyle(Color.fill3) }
                )
            }
        }
        .configureCustomAlert(.betaConfig)
    }
}

extension CustomAlertConfiguration {
    static let betaConfig = CustomAlertConfiguration()
        .button {
            // For some reason, calling background with a role is mutating,
            // so it fails to compile unless you call it on a var
            var button = CustomAlertConfiguration.Button().background(.color(.key))
            return button.background(.color(.fill2), for: .cancel)
        }
}

extension View {
    /** Handling for notifications beta opt in toast and feedback dialog. */
    func notificationsBeta(
        navEntry: SheetNavigationStackEntry,
        onToastTap: @escaping () -> Void,
        onDismissDialog: @escaping () -> Void,
        viewModel: INotificationsBetaViewModel = ViewModelDI().notificationsBeta,
        toastViewModel: IToastViewModel = ViewModelDI().toast,
    ) -> some View {
        modifier(NotificationsBetaModifier(
            navEntry: navEntry,
            onToastTap: onToastTap,
            onDismissDialog: onDismissDialog,
            viewModel: viewModel,
            toastViewModel: toastViewModel,
        ))
    }
}
