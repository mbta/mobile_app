//
//  BackgroundTimerModifier.swift
//  iosApp
//
//  Created by esimon on 10/20/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Foundation
import Shared
import SwiftUI

struct BackgroundTimerModifier: ViewModifier {
    let backgroundSeconds: Int
    let action: () -> Void
    @State var backgroundTime: EasternTimeInstant?

    func body(content: Content) -> some View {
        content.withScenePhaseHandlers(onActive: {
            let now = EasternTimeInstant.now()
            if let backgroundTime, now.minus(backgroundTime).inWholeSeconds >= backgroundSeconds {
                action()
            }
            backgroundTime = nil
        }, onBackground: {
            backgroundTime = EasternTimeInstant.now()
        })
    }
}

public extension View {
    /** Run the specified action when the app has been backgrounded for longer than the specified time (default 1hr). */
    func withBackgroundTimer(
        backgroundSeconds: Int = 60 * 60, // Default to 1h
        action: @escaping () -> Void = {},
    ) -> some View {
        modifier(BackgroundTimerModifier(backgroundSeconds: backgroundSeconds, action: action))
    }
}
