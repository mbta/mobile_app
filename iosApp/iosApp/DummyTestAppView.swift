//
//  DummyTestAppView.swift
//  iosApp
//
//  Created by Brady, Kayla on 4/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared
import SwiftUI

struct DummyTestAppView: View {
    init() {
        HelpersKt.startKoinIOSTestApp()
    }

    var body: some View {
        Text(verbatim: "Dummy app for launching unit tests")
    }
}
