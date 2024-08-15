//
//  AlertIcon.swift
//  iosApp
//
//  Created by Simon, Emma on 7/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct AlertIcon: View {
    var alertState: StopAlertState
    var color: Color

    init(alertState: StopAlertState, color: Color? = nil) {
        self.alertState = alertState
        self.color = color ?? .text
    }

    private static let iconNames: [StopAlertState: String] = [
        .issue: "alert-borderless-issue",
        .shuttle: "alert-borderless-shuttle",
        .suspension: "alert-borderless-suspension",
    ]

    var body: some View {
        Image(AlertIcon.iconNames[alertState] ?? "")
            .resizable()
            .foregroundStyle(color)
    }
}
