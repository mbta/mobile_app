//
//  LoadingCard.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct LoadingCard: View {
    var message: String = .init(localized: "loading", comment: "Loading message")
    var body: some View {
        ZStack {
            Color.fill2.ignoresSafeArea(.all)
            VStack {
                ProgressView()
                    .padding([.bottom], 8)
                Text(message)
            }
        }
        .withRoundedBorder()
        .padding(32)
    }
}

#Preview {
    List {
        LoadingCard()
        LoadingCard(message: "Custom message")
    }
}
