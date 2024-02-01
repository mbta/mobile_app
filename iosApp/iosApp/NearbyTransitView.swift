//
//  NearbyTransitView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-19.
//  Copyright © 2024 MBTA. All rights reserved.
//

import CoreLocation
import shared
import SwiftUI

struct NearbyTransitView: View {
    @ObservedObject private(set) var viewModel: ViewModel
    @EnvironmentObject var locationDataManager: LocationDataManager

    var body: some View {
        if let nearby = viewModel.nearby {
            List {
                ForEach(nearby, id: \.route.id) { nearbyRoute in
                    NearbyRouteView(nearbyRoute: nearbyRoute)
                }
            }
        } else {
            Text("Loading...")
        }
    }
}

struct NearbyRouteView: View {
    let nearbyRoute: NearbyRoute

    var body: some View {
        Section {
            ForEach(nearbyRoute.patternsByStop, id: \.stop.id) { patternsByStopByStop in

                NearbyStopView(patternsByStopByStop: patternsByStopByStop)
            }
        }
        header: {
            Text("\(nearbyRoute.route.shortName) \(nearbyRoute.route.longName)")
        }
    }
}

struct NearbyStopView: View {
    let patternsByStopByStop: NearbyPatternsByStop
    var body: some View {
        VStack(alignment: .leading) {
            Text(patternsByStopByStop.stop.name).fontWeight(.bold)

            VStack(alignment: .leading) {
                ForEach(patternsByStopByStop.routePatterns, id: \.id) { routePattern in
                    Text(routePattern.name)
                }
            }
        }
    }
}

struct NearbyRoutePatternView: View {
    let routePattern: RoutePattern
    let stop: Stop

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Route \(routePattern.route.shortName) \(routePattern.route.longName)")
            Text("Pattern \(routePattern.id) \(routePattern.name)")
            Text("Stop \(stop.name)")
        }
    }
}

extension NearbyTransitView {
    @MainActor class ViewModel: ObservableObject {
        let backend: BackendDispatcher
        @Published var nearby: [NearbyRoute]? = nil
        let location: CLLocationCoordinate2D?

        init(location: CLLocationCoordinate2D?, backend: BackendDispatcher, nearby: [NearbyRoute]? = nil) {
            self.location = location
            self.backend = backend
            self.nearby = nearby
            getNearby()
        }

        func getNearby() {
            Task {
                nearby = nil
                guard let location = self.location else { return }
                do {
                    nearby = try await backend.getNearby(latitude: location.latitude, longitude: location.longitude).byRouteAndStop()
                } catch {
                    debugPrint(error)
                }
            }
        }
    }
}

struct NearbyTransitView_Previews: PreviewProvider {
    static var previews: some View {
        NearbyTransitView(viewModel: .init(
            location: CLLocationCoordinate2D(latitude: 42.271405, longitude: -71.080781),
            backend: BackendDispatcher(backend: IdleBackend())
        )).previewDisplayName("NearbyTransitView")

        NearbyRoutePatternView(
            routePattern: RoutePattern(
                id: "206-_-1",
                directionId: 1,
                name: "Houghs Neck - Quincy Center Station",
                sortOrder: 521_601_000,
                route: Route(
                    id: "216",
                    color: "FFC72C",
                    directionNames: ["Outbound", "Inbound"],
                    directionDestinations: ["Houghs Neck", "Quincy Center Station"],
                    longName: "Houghs Neck - Quincy Center Station via Germantown",
                    shortName: "216",
                    sortOrder: 52160,
                    textColor: "000000"
                )
            ),
            stop: Stop(id: "3276", latitude: 42.265969, longitude: -70.969853, name: "Sea St opp Peterson Rd", parentStation: nil)
        ).previewDisplayName("NearbyRoutePatternView")
    }
}
