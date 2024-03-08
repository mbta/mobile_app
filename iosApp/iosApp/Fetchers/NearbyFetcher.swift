//
//  NearbyFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

class NearbyFetcher: ObservableObject {
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0
    @Published var error: NSError?
    @Published var errorText: Text?
    @Published var loading: Bool = false
    @Published var nearby: StopAndRoutePatternResponse?
    @Published var nearbyByRouteAndStop: NearbyStaticData?
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getNearby(latitude: Double, longitude: Double) async {
        loading = true
        self.latitude = latitude
        self.longitude = longitude
        do {
            let response = try await backend.getNearby(
                latitude: latitude,
                longitude: longitude
            )
            nearby = response
            nearbyByRouteAndStop = NearbyStaticData(response: response)
            error = nil
            errorText = nil
        } catch let error as NSError {
            self.error = error
            errorText = getErrorText(error: error)
        }
        loading = false
    }

    func getErrorText(error: NSError) -> Text {
        switch error.kotlinException {
        case is Ktor_client_coreHttpRequestTimeoutException:
            Text("Couldn't load nearby transit, no response from the server")
        case is Ktor_ioIOException:
            Text("Couldn't load nearby transit, connection was interrupted")
        case is Ktor_serializationJsonConvertException:
            Text("Couldn't load nearby transit, unable to parse response")
        case is Ktor_client_coreResponseException:
            Text("Couldn't load nearby transit, invalid response")
        default:
            Text("Couldn't load nearby transit, something went wrong")
        }
    }

    func withRealtimeInfo(
        predictions: PredictionsStreamDataResponse?,
        filterAtTime: Instant
    ) -> [StopAssociatedRoute]? {
        nearbyByRouteAndStop?.withRealtimeInfo(
            sortByDistanceFrom: .init(longitude: longitude, latitude: latitude),
            predictions: predictions,
            filterAtTime: filterAtTime
        )
    }
}
