//
//  PromoPage.swift
//  iosApp
//
//  Created by esimon on 2/5/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import CoreLocation
import Shared
import SwiftUI

struct PromoPage: View {
    let screens: [FeaturePromo]
    @State var selectedIndex: Int = 0

    let onFinish: () -> Void

    let onAdvance: () -> Void

    let inspection = Inspection<Self>()

    init(
        screens: [FeaturePromo],
        onFinish: @escaping () -> Void,
        onAdvance: @escaping () -> Void = {}

    ) {
        self.screens = screens
        self.onFinish = onFinish
        self.onAdvance = onAdvance
    }

    var body: some View {
        let screen = screens[selectedIndex]
        PromoScreenView(screen: screen, advance: {
            if selectedIndex < screens.count - 1 {
                selectedIndex += 1
                onAdvance()
            } else {
                onFinish()
            }
        })
    }
}

#Preview {
    PromoPage(screens: FeaturePromo.allCases, onFinish: {})
}
