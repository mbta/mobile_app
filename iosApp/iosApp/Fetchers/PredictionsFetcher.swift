//
//  PredictionsFetcher.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-03-15.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class PredictionsFetcher: ChannelFetcher<PredictionsStreamDataResponse> {
    init(socket: any PhoenixSocket,
         onMessageSuccessCallback: (() -> Void)? = nil, onErrorCallback: (() -> Void)? = nil)
    {
        super.init(socket: socket, spec: PredictionsForStopsChannel(stopIds: []),
                   onMessageSuccessCallback: onMessageSuccessCallback, onErrorCallback: onErrorCallback)
    }

    func run(stopIds: [String]) {
        spec = PredictionsForStopsChannel(stopIds: stopIds)
        run()
    }
}
