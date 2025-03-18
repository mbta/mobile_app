//
//  AlertIcon.swift
//  iosApp
//
//  Created by Simon, Emma on 7/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct AlertIcon: View {
    var alertState: StopAlertState
    var color: Color
    let elevatorClosureIcon: ElevatorClosureIconType

    init(alertState: StopAlertState, color: Color? = nil, elevatorClosureIcon: ElevatorClosureIconType = .color) {
        self.alertState = alertState
        self.color = color ?? .text
        self.elevatorClosureIcon = elevatorClosureIcon
    }

    private var iconName: String? {
        switch alertState {
        case .elevator: elevatorClosureIcon.iconName
        case .issue: "alert-borderless-issue"
        case .shuttle: "alert-borderless-shuttle"
        case .suspension: "alert-borderless-suspension"
        case .normal: nil
        }
    }

    var body: some View {
        Image(iconName ?? "")
            .resizable()
            .foregroundStyle(color)
            .accessibilityLabel(Text("Alert"))
    }

    enum ElevatorClosureIconType {
        case color
        case mono

        var iconName: String {
            switch self {
            case .color: "accessibility-icon-alert"
            case .mono: "accessibility-icon-alert-mono"
            }
        }
    }
}
