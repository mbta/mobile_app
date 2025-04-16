//
//  NotAccessibleCard.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 3/25/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct NotAccessibleCard: View {
    var body: some View {
        VStack {
            HStack {
                Image(.accessibilityIconNotAccessible)
                    .accessibilityHidden(true)
                    .tag("wheelchair_not_accessible")
                Text("This stop is not accessible")
                    .frame(maxWidth: .infinity, alignment: .leading)
            }
        }
        .frame(maxWidth: .infinity)
        .padding(.horizontal, 12)
        .padding(.vertical, 10)
        .background(Color.fill3)
        .clipShape(RoundedRectangle(cornerRadius: 8))
        .padding(1)
        .overlay(RoundedRectangle(cornerRadius: 8).stroke(Color.halo, lineWidth: 2))
        .padding(.top, 1)
    }
}
