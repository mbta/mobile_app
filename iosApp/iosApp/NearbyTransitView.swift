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

extension CLLocationCoordinate2D: Equatable {}

public func == (lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool {
    lhs.latitude == rhs.latitude && lhs.longitude == rhs.longitude
}

struct NearbyTransitView: View {
    let location: CLLocationCoordinate2D?
    @ObservedObject var fetcher: NearbyFetcher

    var didAppear: ((Self) -> Void)?
    var didChange: ((Self) -> Void)?

    func getNearby(location: CLLocationCoordinate2D?) {
        Task {
            if location == nil { return }
            do {
                try await fetcher
                    .getNearby(latitude: location!.latitude, longitude: location!.longitude)
            } catch {
                debugPrint(error)
            }
        }
    }

    var body: some View {
        VStack {
            if let nearby = fetcher.nearbyByRouteAndStop {
                List(nearby, id: \.route.id) { nearbyRoute in
                    NearbyRouteView(nearbyRoute: nearbyRoute)
                }
            } else {
                Text("Loading...")
            }
        }.onAppear {
            getNearby(location: location)
            didAppear?(self)
        }
        .onChange(of: location) { location in
            getNearby(location: location)
            didChange?(self)
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
            Text(verbatim: "\(nearbyRoute.route.shortName) \(nearbyRoute.route.longName)")
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

struct NearbyTransitView_Previews: PreviewProvider {
    static var previews: some View {
        NearbyRouteView(
            nearbyRoute: NearbyRoute(
                route: Route(
                    id: "216",
                    color: "FFC72C",
                    directionNames: ["Outbound", "Inbound"],
                    directionDestinations: ["Houghs Neck", "Quincy Center Station"],
                    longName: "Houghs Neck - Quincy Center Station via Germantown",
                    shortName: "216",
                    sortOrder: 52160,
                    textColor: "000000"
                ),
                patternsByStop: [
                    NearbyPatternsByStop(
                        stop: Stop(
                            id: "3276",
                            latitude: 42.265969,
                            longitude: -70.969853,
                            name: "Sea St opp Peterson Rd",
                            parentStation: nil
                        ),
                        routePatterns: [
                            RoutePattern(
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
                        ]
                    ),
                ]
            )
        ).previewDisplayName("NearbyRouteView")
    }
}
