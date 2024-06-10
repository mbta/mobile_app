//
//  SocketErrorExtension.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 6/5/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared

extension shared.SocketError {
    var predictionsErrorText: String {
        switch self {
        case .connection:
            "Failed to load predictions, could not connect to the server"
        case .unknown:
            "Failed to load new predictions, something went wrong"
        }
    }
}
