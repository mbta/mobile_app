//
//  Channel.swift
//  iosApp
//
//  Created by Brady, Kayla on 3/4/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftPhoenixClient

public protocol PhoenixChannel: AnyObject {
    func onOpen(response: URLResponse?)
    func on(_ event: String, callback: @escaping ((Message) -> Void)) -> Int
    func onError(error: Error, response: URLResponse?)
    func onMessage(message: String)
    func onClose(code: Int, reason: String?)
    func join(timeout: TimeInterval?)
    func leave(timeout: TimeInterval)
}
