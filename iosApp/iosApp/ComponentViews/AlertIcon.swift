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
    var accessibilityHidden: Bool = false

    init(alertState: StopAlertState, color: Color? = nil, accessibilityHidden: Bool = false) {
        self.alertState = alertState
        self.color = color ?? .text
        self.accessibilityHidden = accessibilityHidden
    }

    private var iconName: String? {
        switch alertState {
        case .elevator: "accessibility-icon-alert"
        case .issue: "alert-borderless-issue"
        case .shuttle: "alert-borderless-shuttle"
        case .suspension: "alert-borderless-suspension"
        case .allClear: "alert-borderless-allclear"
        case .normal: nil
        }
    }

    var body: some View {
        Image(iconName ?? "")
            .resizable()
            .foregroundStyle(color)
            .accessibilityLabel(Text("Alert"))
            .accessibilityHidden(accessibilityHidden)
    }
}
