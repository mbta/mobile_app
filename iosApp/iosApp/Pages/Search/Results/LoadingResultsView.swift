//
//  LoadingResultsView.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 10/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct LoadingResults: View {
    var body: some View {
        VStack(spacing: 8) {
            ForEach(0 ... 10, id: \.self) { _ in
                ResultContainer { skeletonResult.padding(12) }
            }
        }
    }

    var skeletonResult: some View {
        HStack {
            VStack(alignment: .leading, spacing: 4) {
                Rectangle()
                    .fill(Color.fill1)
                    .frame(height: 24)
                    .frame(maxWidth: 240)
                Rectangle()
                    .fill(Color.fill1)
                    .frame(height: 16)
                    .frame(maxWidth: 48)
            }
            Spacer()
        }
    }
}
