//
//  PredictionsErrorExtension.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 5/8/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

extension PredictionsError {
    var text: LocalizedStringResource {
        switch self {
        case .connection:
            "Failed to load predictions, could not connect to the server"
        case .unknown:
            "Failed to load new predictions, something went wrong"
        }
    }
}
