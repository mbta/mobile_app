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

public func == (lhs: CLLocationCoordinate2D, rhs: CLLocationCoordinate2D) -> Bool {
    lhs.latitude == rhs.latitude && lhs.longitude == rhs.longitude
}

struct NearbyTransitView: View {
    let location: CLLocationCoordinate2D?
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var predictionsFetcher: PredictionsFetcher
    @State var now = Date.now
    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()

    var didAppear: ((Self) -> Void)?
    var didChange: ((Self) -> Void)?

    func getNearby(location: CLLocationCoordinate2D?) {
        Task {
            if location == nil { return }
            do {
                try await nearbyFetcher
                    .getNearby(latitude: location!.latitude, longitude: location!.longitude)
            } catch {
                debugPrint(error)
            }
        }
    }

    func joinPredictions() {
        Task {
            guard let stopIds = nearbyFetcher.nearbyByRouteAndStop?.flatMap({ $0.patternsByStop.map(\.stop.id) }) else { return }
            do {
                let stopIds = Array(Set(stopIds))
                try await predictionsFetcher.run(stopIds: stopIds)
            } catch {
                debugPrint("failed to run predictions", error)
            }
        }
    }

    func leavePredictions() {
        Task {
            do {
                try await predictionsFetcher.leave()
            } catch {
                debugPrint(error)
            }
        }
    }

    var body: some View {
        VStack {
            if let nearby = nearbyFetcher.nearbyByRouteAndStop {
                List(nearby, id: \.route.id) { nearbyRoute in
                    NearbyRouteView(nearbyRoute: nearbyRoute, allPredictions: predictionsFetcher.predictions, now: now.toKotlinInstant())
                }
            } else {
                Text("Loading...")
            }
        }.onAppear {
            getNearby(location: location)
            didAppear?(self)
            joinPredictions()
        }
        .onChange(of: location) { location in
            getNearby(location: location)
            didChange?(self)
        }
        .onChange(of: nearbyFetcher.nearbyByRouteAndStop) { _ in
            joinPredictions()
        }
        .onReceive(timer) { input in
            now = input
        }
        .onDisappear {
            leavePredictions()
        }
    }
}

struct NearbyRouteView: View {
    let nearbyRoute: NearbyRoute
    let allPredictions: [Prediction]?
    let now: Instant

    var body: some View {
        Section {
            ForEach(nearbyRoute.patternsByStop, id: \.stop.id) { patternsByStopByStop in
                NearbyStopView(patternsByStopByStop: patternsByStopByStop, allPredictions: allPredictions, now: now)
            }
        }
        header: {
            Text(verbatim: "\(nearbyRoute.route.shortName) \(nearbyRoute.route.longName)")
        }
    }
}

struct NearbyStopView: View {
    let patternsByStopByStop: NearbyPatternsByStop
    let allPredictions: [Prediction]?
    let now: Instant

    var body: some View {
        VStack(alignment: .leading) {
            Text(patternsByStopByStop.stop.name).fontWeight(.bold)

            VStack(alignment: .leading) {
                ForEach(patternsByStopByStop.routePatterns, id: \.id) { routePattern in
                    let prediction: NearbyStopRoutePatternView.PredictionState =
                        if let predictions = allPredictions {
                            if let firstPrediction = predictions
                                .filter({ $0.trip.routePatternId == routePattern.id })
                                .map({ $0.format(now: now) })
                                .filter({ ($0 as? Prediction.FormatHidden) == nil })
                                .first
                            {
                                .some(firstPrediction)
                            } else { .none }
                        } else { .loading }
                    NearbyStopRoutePatternView(
                        routePattern: routePattern,
                        prediction: prediction
                    )
                }
            }
        }
    }
}

struct NearbyStopRoutePatternView: View {
    let routePattern: RoutePattern
    let prediction: PredictionState

    enum PredictionState {
        case loading
        case none
        case some(Prediction.Format)
    }

    var body: some View {
        HStack {
            Text(routePattern.name).layoutPriority(1)
            Spacer()
            let predictionText =
                switch prediction {
                case let .some(prediction):
                    switch onEnum(of: prediction) {
                    case let .overridden(overridden):
                        Text(verbatim: overridden.text)
                    case .hidden:
                        // should have been filtered out already
                        Text(verbatim: "")
                    case .boarding:
                        Text("Boarding")
                    case .arriving:
                        Text("Arriving")
                    case .approaching:
                        Text("Approaching")
                    case .distantFuture:
                        Text("20+ minutes")
                    case let .minutes(format):
                        Text("\(format.minutes, specifier: "%ld") minutes")
                    }
                case .none:
                    Text("No Predictions")
                case .loading:
                    Text("Loading...")
                }
            predictionText
                .lineLimit(1)
                .layoutPriority(2)
                .frame(minWidth: 64, alignment: .trailing)
        }
    }
}

struct NearbyTransitView_Previews: PreviewProvider {
    static var previews: some View {
        List {
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
                ),
                allPredictions: [
                    Prediction(
                        id: "",
                        arrivalTime: nil,
                        departureTime: (Date.now + 5 * 60).toKotlinInstant(),
                        directionId: 0,
                        revenue: true,
                        scheduleRelationship: .scheduled,
                        status: nil,
                        stopSequence: 30,
                        stopId: "3276",
                        trip: Trip(id: "", routePatternId: "206-_-1", stops: nil),
                        vehicle: nil
                    ),
                ],
                now: Date.now.toKotlinInstant()
            )
        }.previewDisplayName("NearbyRouteView")
    }
}
