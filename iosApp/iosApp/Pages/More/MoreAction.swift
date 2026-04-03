//
//  MoreAction.swift
//  iosApp
//
//  Created by esimon on 3/17/26.
//  Copyright © 2026 MBTA. All rights reserved.
//

import SwiftUI

struct MoreAction: View {
    var label: String
    var callback: () -> Void

    var body: some View {
        Button(action: callback) {
            HStack(alignment: .center, spacing: 0) {
                Text(label)
                    .foregroundStyle(Color.text)
                    .font(Typography.body)
            }
        }
        .padding(.vertical, 10)
        .padding(.horizontal, 16)
        .frame(minHeight: 44)
    }
}
