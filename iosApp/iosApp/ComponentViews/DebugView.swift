//
//  DebugView.swift
//  iosApp
//
//  Created by Kayla Brady on 11/7/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct DebugView<Content: View>: View {
    let content: () -> Content
    var body: some View {
        ZStack {
            Rectangle()
                .strokeBorder(Color(.text), style: .init(lineWidth: 2, dash: [10]))
            VStack(alignment: .leading) {
                content()
            }.padding(4)
        }
    }
}
