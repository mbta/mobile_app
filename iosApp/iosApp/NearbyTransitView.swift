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
    @Environment(\.scenePhase) private var scenePhase
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
            guard let stopIds = nearbyFetcher.nearbyByRouteAndStop?
                .stopIds() else { return }
            let stopIdList = Array(stopIds)
            await predictionsFetcher.run(stopIds: stopIdList)
        }
    }

    func leavePredictions() {
        Task {
            await predictionsFetcher.leave()
        }
    }

    var body: some View {
        VStack {
            if let nearby = nearbyFetcher.withRealtimeInfo(
                predictions: predictionsFetcher.predictions,
                filterAtTime: now.toKotlinInstant()
            ) {
                if let predictionsError = predictionsFetcher.socketError {
                    Text("Error fetching predictions: \(predictionsError.localizedDescription)")
                }
                List(nearby, id: \.route.id) { nearbyRoute in
                    NearbyRouteView(nearbyRoute: nearbyRoute, now: now.toKotlinInstant())
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
        .onChange(of: scenePhase) { newPhase in
            if newPhase == .inactive {
                leavePredictions()
            } else if newPhase == .active {
                joinPredictions()
            } else if newPhase == .background {
                leavePredictions()
            }
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
    let nearbyRoute: StopAssociatedRoute
    let now: Instant

    var body: some View {
        Section {
            ForEach(nearbyRoute.patternsByStop, id: \.stop.id) { patternsAtStop in
                NearbyStopView(patternsAtStop: patternsAtStop, now: now)
            }
        }
        header: {
            RoutePill(route: nearbyRoute.route).padding(.leading, -20)
        }
    }
}

struct NearbyStopView: View {
    let patternsAtStop: PatternsByStop
    let now: Instant

    var body: some View {
        VStack(alignment: .leading) {
            Text(patternsAtStop.stop.name).fontWeight(.bold)

            VStack(alignment: .leading) {
                ForEach(patternsAtStop.patternsByHeadsign, id: \.headsign) { patternsByHeadsign in
                    NearbyStopRoutePatternView(
                        headsign: patternsByHeadsign.headsign,
                        predictions: .from(predictions: patternsByHeadsign.predictions, now: now)
                    )
                }
            }
        }
    }
}

struct NearbyStopRoutePatternView: View {
    let headsign: String
    let predictions: PredictionState

    struct PredictionWithFormat: Identifiable {
        let prediction: Prediction
        let format: Prediction.Format

        var id: String { prediction.id }

        init(_ prediction: Prediction, now: Instant) {
            self.prediction = prediction
            format = prediction.format(now: now)
        }

        func isHidden() -> Bool {
            format is Prediction.FormatHidden
        }
    }

    enum PredictionState {
        case loading
        case none
        case some([PredictionWithFormat])

        static func from(predictions: [Prediction]?, now: Instant) -> Self {
            guard let predictions else { return .loading }
            let predictionsToShow = predictions
                .map { PredictionWithFormat($0, now: now) }
                .filter { !$0.isHidden() }
                .prefix(2)
            if predictionsToShow.isEmpty {
                return .none
            }
            return .some(Array(predictionsToShow))
        }
    }

    var body: some View {
        HStack {
            Text(headsign)
            Spacer()
            switch predictions {
            case let .some(predictions):
                ForEach(predictions) { prediction in
                    PredictionView(prediction: .some(prediction.format))
                }
            case .none:
                PredictionView(prediction: .none)
            case .loading:
                PredictionView(prediction: .loading)
            }
        }
    }
}

extension Prediction.FormatOverridden {
    func textWithLocale() -> AttributedString {
        var result = AttributedString(text)
        result.languageIdentifier = "en-US"
        return result
    }
}

struct PredictionView: View {
    let prediction: State

    enum State: Equatable {
        case loading
        case none
        case some(Prediction.Format)
    }

    var body: some View {
        let predictionView: any View = switch prediction {
        case let .some(prediction):
            switch onEnum(of: prediction) {
            case let .overridden(overridden):
                Text(overridden.textWithLocale())
            case .hidden:
                // should have been filtered out already
                Text(verbatim: "")
            case .boarding:
                Text("BRD")
            case .arriving:
                Text("ARR")
            case .approaching:
                Text("1 min")
            case let .distantFuture(format):
                Text(Date(instant: format.predictionTime), style: .time)
            case let .minutes(format):
                Text("\(format.minutes, specifier: "%ld") min")
            }
        case .none:
            Text("No Predictions")
        case .loading:
            ProgressView()
        }
        AnyView(predictionView)
            .frame(minWidth: 48, alignment: .trailing)
    }
}

struct NearbyTransitView_Previews: PreviewProvider {
    static var previews: some View {
        let busTrip = Trip(
            id: "trip1",
            headsign: "Houghs Neck",
            routePatternId: "206-_-1",
            shape: nil,
            stops: nil
        )
        let crTrip = Trip(
            id: "canonical-CR-Providence-C1-0",
            headsign: "Wickford Junction",
            routePatternId: nil,
            shape: nil,
            stops: nil
        )
        List {
            NearbyRouteView(
                nearbyRoute: StopAssociatedRoute(
                    route: Route(
                        id: "216",
                        type: RouteType.bus,
                        color: "FFC72C",
                        directionNames: ["Outbound", "Inbound"],
                        directionDestinations: ["Houghs Neck", "Quincy Center Station"],
                        longName: "Houghs Neck - Quincy Center Station via Germantown",
                        shortName: "216",
                        sortOrder: 52160,
                        textColor: "000000",
                        routePatterns: nil
                    ),
                    patternsByStop: [
                        PatternsByStop(
                            stop: Stop(
                                id: "3276",
                                latitude: 42.265969,
                                longitude: -70.969853,
                                name: "Sea St opp Peterson Rd",
                                parentStation: nil
                            ),
                            patternsByHeadsign: [
                                PatternsByHeadsign(headsign: "Houghs Neck", patterns:
                                    [RoutePattern(
                                        id: "206-_-1",
                                        directionId: 1,
                                        name: "Houghs Neck - Quincy Center Station",
                                        sortOrder: 521_601_000,
                                        typicality: .typical,
                                        representativeTrip: busTrip,
                                        routeId: "216"
                                    )], predictions: [Prediction(
                                        id: "1",
                                        arrivalTime: nil,
                                        departureTime: (Date.now + 5 * 60).toKotlinInstant(),
                                        directionId: 0,
                                        revenue: true,
                                        scheduleRelationship: .scheduled,
                                        status: nil,
                                        stopSequence: 30,
                                        stopId: "3276",
                                        trip: busTrip,
                                        vehicle: nil
                                    ), Prediction(
                                        id: "2",
                                        arrivalTime: nil,
                                        departureTime: (Date.now + 12 * 60).toKotlinInstant(),
                                        directionId: 0,
                                        revenue: true,
                                        scheduleRelationship: .scheduled,
                                        status: nil,
                                        stopSequence: 90,
                                        stopId: "3276",
                                        trip: busTrip,
                                        vehicle: nil
                                    )]),
                            ]
                        ),
                    ]
                ),
                now: Date.now.toKotlinInstant()
            )
            NearbyRouteView(
                nearbyRoute: StopAssociatedRoute(
                    route: Route(
                        id: "CR-Providence",
                        type: RouteType.commuterRail,
                        color: "80276C",
                        directionNames: ["Outbound", "Inbound"],
                        directionDestinations: ["Stoughton or Wickford Junction", "South Station"],
                        longName: "Providence/Stoughton Line",
                        shortName: "",
                        sortOrder: 20012,
                        textColor: "FFFFFF",
                        routePatterns: nil
                    ),
                    patternsByStop: [
                        PatternsByStop(
                            stop: Stop(
                                id: "place-sstat",
                                latitude: 42.265969,
                                longitude: -70.969853,
                                name: "South Station",
                                parentStation: nil
                            ),
                            patternsByHeadsign: [
                                PatternsByHeadsign(headsign: "Houghs Neck", patterns:
                                    [RoutePattern(
                                        id: "CR-Providence-C1-0",
                                        directionId: 0,
                                        name: "South Station - Wickford Junction via Back Bay",
                                        sortOrder: 200_120_991,
                                        typicality: .typical,
                                        representativeTrip: crTrip,
                                        routeId: "CR-Providence"
                                    )], predictions: [Prediction(
                                        id: "1",
                                        arrivalTime: nil,
                                        departureTime: (Date.now + 32 * 60).toKotlinInstant(),
                                        directionId: 0,
                                        revenue: true,
                                        scheduleRelationship: .scheduled,
                                        status: nil,
                                        stopSequence: 30,
                                        stopId: "place-sstat",
                                        trip: crTrip,
                                        vehicle: nil
                                    ), Prediction(
                                        id: "2",
                                        arrivalTime: nil,
                                        departureTime: (Date.now + 72 * 60).toKotlinInstant(),
                                        directionId: 0,
                                        revenue: true,
                                        scheduleRelationship: .scheduled,
                                        status: nil,
                                        stopSequence: 90,
                                        stopId: "place-sstat",
                                        trip: crTrip,
                                        vehicle: nil
                                    )]),
                            ]
                        ),
                    ]
                ),
                now: Date.now.toKotlinInstant()
            )
        }.previewDisplayName("NearbyRouteView")
    }
}
