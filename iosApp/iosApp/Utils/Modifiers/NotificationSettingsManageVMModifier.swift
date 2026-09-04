//
//  NotificationSettingsManageVMModifier.swift
//  iosApp
//
//  Created by Kayla Brady on 9/4/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct NotificationSettingsManageVMModifier: ViewModifier {
    var viewModel: INotificationSettingsViewModel
    @Binding var vmState: NotificationSettingsViewModel.State?
    var now: EasternTimeInstant

    @EnvironmentObject var settingsCache: SettingsCache
    var presetWindowsEnabled: Bool { settingsCache.get(.notificationPresetWindows) }

    func body(content: Content) -> some View {
        content
            .task {
                for await models in viewModel.models {
                    print("NEW STATE \(models)")
                    vmState = models
                }
            }
            .onAppear {
                viewModel.setNow(now: now)
                viewModel.setPresetsEnabledFlag(enabled: presetWindowsEnabled)
            }
            .onChange(of: presetWindowsEnabled) { newVal in
                viewModel.setPresetsEnabledFlag(enabled: newVal)
            }
            .enableInjection()
    }
}

public extension View {
    func manageVM(
        _ viewModel: INotificationSettingsViewModel,
        _ state: Binding<NotificationSettingsViewModel.State?>,
        _ now: EasternTimeInstant? = nil
    ) -> some View {
        let now = now ?? EasternTimeInstant.now()
        return modifier(NotificationSettingsManageVMModifier(
            viewModel: viewModel,
            vmState: state,
            now: now,
        ))
    }
}
