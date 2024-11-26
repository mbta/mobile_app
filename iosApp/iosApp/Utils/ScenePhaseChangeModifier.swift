//
//  ScenePhaseChangeModifier.swift
//  iosApp
//
//  Created by Brady, Kayla on 5/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct ScenePhaseChangeModifier: ViewModifier {
    @Environment(\.scenePhase) var scenePhase
    let onActive: () -> Void
    let onInactive: () -> Void
    let onBackground: () -> Void

    func body(content: Content) -> some View {
        content.onChange(of: scenePhase) { @MainActor newPhase in
            if newPhase == .active {
                onActive()
            } else if newPhase == .inactive {
                onInactive()
            } else if newPhase == .background {
                onBackground()
            }
        }
    }
}

public extension View {
    func withScenePhaseHandlers(
        onActive: @escaping () -> Void = {},
        onInactive: @escaping () -> Void = {},
        onBackground: @escaping () -> Void = {}
    ) -> some View {
        modifier(ScenePhaseChangeModifier(onActive: onActive, onInactive: onInactive, onBackground: onBackground))
    }
}
