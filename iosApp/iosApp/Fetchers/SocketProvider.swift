//
//  SocketProvider.swift
//  iosApp
//
//  Created by Brady, Kayla on 3/6/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftPhoenixClient

class SocketProvider: ObservableObject {
    @Published var socket: PhoenixSocket

    init(socket: PhoenixSocket) {
        self.socket = socket
    }
}
