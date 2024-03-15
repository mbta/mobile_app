//
//  AlertsFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-15.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class AlertsFetcher: ChannelFetcher<AlertsStreamDataResponse> {
    init(socket: any PhoenixSocket,
         onMessageSuccessCallback: (() -> Void)? = nil, onErrorCallback: (() -> Void)? = nil)
    {
        super.init(socket: socket, spec: AlertsChannel.shared,
                   onMessageSuccessCallback: onMessageSuccessCallback, onErrorCallback: onErrorCallback)
    }
}
