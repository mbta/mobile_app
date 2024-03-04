//
//  Message.swift
//  iosApp
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftPhoenixClient

extension Message {
    func jsonPayload() -> String? {
        payload["jsonPayload"] as? String
    }
}
