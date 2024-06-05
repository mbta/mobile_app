//
//  GlobalData.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 5/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class GlobalData: ObservableObject {
    static let shared = GlobalData()

    @Published var response: GlobalResponse?
    var stops: [String: Stop] {
        response?.stops ?? [:]
    }

    var routes: [String: Route] {
        response?.routes ?? [:]
    }
}
