//
//  EmptyStateView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct EmptyStateView: View {
    @ObserveInjection var inject
    var headline: String
    var subheadline: String

    var body: some View {
        VStack(spacing: 8) {
            Text(headline)
                .font(Typography.subheadlineSemibold)
            Text(subheadline)
                .font(Typography.subheadline)
        }
        .frame(maxWidth: .infinity)
        .enableInjection()
    }
}
