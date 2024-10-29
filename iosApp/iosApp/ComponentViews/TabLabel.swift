//
//  TabLabel.swift
//  iosApp
//
//  Created by esimon on 10/24/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct TabLabel: View {
    var title: String
    var image: ImageResource

    init(_ title: String, image: ImageResource) {
        self.title = title
        self.image = image
    }

    var body: some View {
        Label(title: { Text(title) }, icon: { Image(image) })
    }
}
