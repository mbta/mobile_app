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
    let backend: BackendDispatcher
    @State var nearby: NearbyResponse?

    var didAppear: ((Self) -> Void)?
    var didChange: ((Self) -> Void)?

    func getNearby(location: CLLocationCoordinate2D?) {
        Task {
            if location == nil { return }
            do {
                nearby = try await backend.getNearby(latitude: location!.latitude, longitude: location!.longitude)
            } catch {
                debugPrint(error)
            }
        }
    }

    var body: some View {
        VStack {
            if nearby != nil {
                List(nearby!.routePatternsByStop(), id: \.first!.id) {
                    NearbyRoutePatternView(routePattern: $0.first!, stop: $0.second!)
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

struct NearbyRoutePatternView: View {
    let routePattern: RoutePattern
    let stop: Stop

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Route \(routePattern.route!.shortName) \(routePattern.route!.longName)")
            Text("Pattern \(routePattern.id) \(routePattern.name)")
            Text("Stop \(stop.name)")
        }
    }
}

struct NearbyTransitView_Previews: PreviewProvider {
    static var previews: some View {
        NearbyTransitView(
            location: CLLocationCoordinate2D(latitude: 42.271405, longitude: -71.080781),
            backend: BackendDispatcher(backend: IdleBackend())
        ).previewDisplayName("NearbyTransitView")

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
            stop: Stop(
                id: "3276",
                latitude: 42.265969,
                longitude: -70.969853,
                name: "Sea St opp Peterson Rd",
                parentStation: nil
            )
        ).previewDisplayName("NearbyRoutePatternView")
    }
}
