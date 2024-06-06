//
//  BackButton.swift
//  iosApp
//
//  Created by Brady, Kayla on 6/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct BackButton: View {
    let onPress: () -> Void

    var body: some View {
        Text("Back")
    }
}

struct BackButton_Previews: PreviewProvider {
    static var previews: some View {
        BackButton(onPress: {}).previewDisplayName("Back Button")
    }
}
