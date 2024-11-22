//
//  LoadingCard.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/18/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct LoadingCard<Message: View>: View {
    var message: () -> Message?

    init(message: @escaping () -> Message? = { Text("Loading...") }) {
        self.message = message
    }

    var body: some View {
        ZStack {
            Color.fill2.ignoresSafeArea(.all)
            VStack {
                ProgressView()
                    .padding([.bottom], 8)
                message()
            }
        }
        .withRoundedBorder()
    }
}

#Preview {
    List {
        LoadingCard()
        LoadingCard { Text(verbatim: "Custom message") }
    }
}
