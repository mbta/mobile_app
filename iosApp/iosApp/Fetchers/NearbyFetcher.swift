//
//  NearbyFetcher.swift
//  iosApp
//
//  Created by Simon, Emma on 2/2/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
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

    private var currentTask: Task<Void, Never>?

    init(backend: any BackendProtocol) {
        self.backend = backend
    }

    func getNearby(location: CLLocationCoordinate2D) async {
        if location.isRoughlyEqualTo(loadedLocation) { return }
        if loading, currentTask != nil, !currentTask!.isCancelled {
            currentTask?.cancel()
        }
        currentTask = Task { @MainActor [weak self] in
            guard let self else { return }

            self.loading = true
            do {
                let response = try await self.backend.getNearby(
                    latitude: location.latitude,
                    longitude: location.longitude
                )
                try Task.checkCancellation()
                self.nearby = response
                self.nearbyByRouteAndStop = NearbyStaticData(response: response)
                self.loadedLocation = location
                self.error = nil
                self.errorText = nil
            } catch is CancellationError {
                // Do nothing when cancelled
                return
            } catch let error as NSError {
                withUnsafeCurrentTask { thisTask in
                    if self.currentTask?.hashValue == thisTask?.hashValue {
                        self.error = error
                        self.errorText = self.getErrorText(error: error)
                    }
                }
            }
            self.loading = false
        }
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
        guard let loadedLocation else { return nil }
        return nearbyByRouteAndStop?.withRealtimeInfo(
            sortByDistanceFrom: .init(longitude: loadedLocation.longitude, latitude: loadedLocation.latitude),
            schedules: schedules,
            predictions: predictions,
            filterAtTime: filterAtTime
        )
    }
}
