//
//  HaloSeparator.swift
//  iosApp
//
//  Created by esimon on 12/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct HaloSeparator: View {
    var body: some View {
        Divider().frame(maxHeight: 1).foregroundStyle(Color.halo)
    }
}