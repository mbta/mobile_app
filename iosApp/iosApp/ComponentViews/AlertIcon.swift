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
    var color: Color
    var accessibilityHidden: Bool = false
    let icon: Image?

    init(
        alertState: StopAlertState,
        color: Color? = nil,
        accessibilityHidden: Bool = false,
        overrideIcon: Image? = nil
    ) {
        icon = overrideIcon ?? Self.icon(alertState: alertState)
        self.color = color ?? .text
        self.accessibilityHidden = accessibilityHidden
    }

    private static func icon(alertState: StopAlertState) -> Image? {
        switch alertState {
        case .elevator: Image(.accessibilityIconAlert)
        case .issue: Image(.alertBorderlessIssue)
        case .shuttle: Image(.alertBorderlessShuttle)
        case .suspension: Image(.alertBorderlessSuspension)
        case .allClear: Image(.alertBorderlessAllclear)
        case .normal: nil
        }
    }

    var body: some View {
        if let icon {
            icon
                .resizable()
                .foregroundStyle(color)
                .accessibilityLabel(Text("Alert"))
                .accessibilityHidden(accessibilityHidden)
        }
    }
}
