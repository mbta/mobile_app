//
//  ResultContainer.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct ResultContainer<Content: View>: View {
    @ViewBuilder
    var content: () -> Content

    var body: some View {
        VStack {
            content()
        }
        .frame(maxWidth: .infinity)
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .overlay(
            RoundedRectangle(cornerRadius: 8)
                .stroke(Color.halo, lineWidth: 1)
        )
    }
}
