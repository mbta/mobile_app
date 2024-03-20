//
//  NearbyFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import shared
import SwiftUI

class NearbyFetcher: ObservableObject {
    @Published var error: NSError?
    @Published var errorText: Text?
    @Published var loadedLocation: CLLocationCoordinate2D?
    @Published var loading: Bool = false
    @Published var nearby: StopAndRoutePatternResponse?
    @Published var nearbyByRouteAndStop: NearbyStaticData?
    let backend: any BackendProtocol

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    @MainActor func getNearby(location: CLLocationCoordinate2D) async {
        if loading || location == loadedLocation { return }
        loading = true
        do {
            let response = try await backend.getNearby(
                latitude: location.latitude,
                longitude: location.longitude
            )
            nearby = response
            nearbyByRouteAndStop = NearbyStaticData(response: response)
            loadedLocation = location
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
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        filterAtTime: Instant
    ) -> [StopAssociatedRoute]? {
        let lat = loadedLocation?.latitude ?? 0.0
        let lon = loadedLocation?.longitude ?? 0.0
        return nearbyByRouteAndStop?.withRealtimeInfo(
            sortByDistanceFrom: .init(longitude: lon, latitude: lat),
            schedules: schedules,
            predictions: predictions,
            filterAtTime: filterAtTime
        )
    }
}
