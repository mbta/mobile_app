//
//  WarningIcon.swift
//  iosApp
//
//  Created by Kayla Brady on 7/6/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import SwiftUI

func warningIcon(_ iconName: String) -> some View {
    Image(iconName)
        .accessibilityLabel("Alert")
        .frame(width: 18, height: 18)
}
