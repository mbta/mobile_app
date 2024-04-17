//
//  NearbyTransitView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-19.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
import os
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct NearbyTransitView: View {
    @Environment(\.scenePhase) private var scenePhase
    @EnvironmentObject private var togglePinnedUsecaseProvider: TogglePinnedRouteUsecaseProvider
    @EnvironmentObject private var pinnedRouteRepositoryProvider: PinnedRouteRepositoryProvider
    var location: CLLocationCoordinate2D
    @ObservedObject var globalFetcher: GlobalFetcher
    @ObservedObject var nearbyFetcher: NearbyFetcher
    @ObservedObject var scheduleFetcher: ScheduleFetcher
    @ObservedObject var predictionsFetcher: PredictionsFetcher
    @ObservedObject var alertsFetcher: AlertsFetcher
    @State var nearbyWithRealtimeInfo: [StopAssociatedRoute]?
    @State var now = Date.now
    @State var scrollPosition: String?
    @State var pinnedRoutes: Set<String> = []

    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()
    let inspection = Inspection<Self>()

    var body: some View {
        VStack {
            if let nearbyWithRealtimeInfo {
                nearbyList(nearbyWithRealtimeInfo)
            } else {
                Text("Loading...")
                    .frame(maxWidth: .infinity)
                    .padding(.top, 24)
            }
        }
        .onAppear {
            getNearby(location: location)
            joinPredictions()
            updateNearbyRoutes()
            updatePinnedRoutes()
            didAppear?(self)
        }
        .onChange(of: globalFetcher.response) { _ in
            getNearby(location: location)
        }
        .onChange(of: location) { newLocation in
            getNearby(location: newLocation)
        }
        .onChange(of: nearbyFetcher.nearbyByRouteAndStop) { _ in
            updateNearbyRoutes()
            getSchedule()
            joinPredictions()
            scrollToTop()
        }
        .onChange(of: scheduleFetcher.schedules) { _ in
            updateNearbyRoutes()
        }
        .onChange(of: predictionsFetcher.predictions) { _ in
            updateNearbyRoutes()
        }
        .onChange(of: alertsFetcher.alerts) { _ in
            updateNearbyRoutes()
        }
        .onChange(of: scenePhase) { newPhase in
            onSceneChange(newPhase)
        }
        .onReceive(timer) { input in
            now = input
            updateNearbyRoutes()
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onDisappear {
            leavePredictions()
        }
        .replaceWhen(nearbyFetcher.errorText) { errorText in
            errorCard(errorText)
        }
    }

    private func nearbyList(_ routes: [StopAssociatedRoute]) -> some View {
        ScrollViewReader { proxy in
            List(routes, id: \.route.id) { nearbyRoute in
                NearbyRouteView(
                    nearbyRoute: nearbyRoute,
                    pinned: pinnedRoutes.contains(nearbyRoute.route.id),
                    onPin: { id in toggledPinnedRoute(id) },
                    now: now.toKotlinInstant()
                )
            }
            .onChange(of: scrollPosition) { id in
                guard let id else { return }
                withAnimation {
                    proxy.scrollTo(id, anchor: .center)
                    scrollPosition = nil
                }
            }
            .putAboveWhen(predictionsFetcher.errorText) { errorText in
                IconCard(iconName: "network.slash", details: errorText)
            }
        }
    }

    private func errorCard(_ errorText: Text) -> some View {
        IconCard(iconName: "network.slash", details: errorText)
            .refreshable(nearbyFetcher.loading) { getNearby(location: location) }
    }

    var didAppear: ((Self) -> Void)?

    func getNearby(location: CLLocationCoordinate2D) {
        Task {
            guard let globalData = globalFetcher.response else { return }
            await nearbyFetcher.getNearby(global: globalData, location: location)
        }
    }

    func getSchedule() {
        Task {
            guard let stopIds = nearbyFetcher.nearbyByRouteAndStop?
                .stopIds() else { return }
            let stopIdList = Array(stopIds)
            await scheduleFetcher.getSchedule(stopIds: stopIdList)
        }
    }

    func joinPredictions() {
        guard let stopIds = nearbyFetcher.nearbyByRouteAndStop?
            .stopIds() else { return }
        let stopIdList = Array(stopIds)
        predictionsFetcher.run(stopIds: stopIdList)
    }

    func leavePredictions() {
        predictionsFetcher.leave()
    }

    private func scrollToTop() {
        guard let id = nearbyWithRealtimeInfo?.first?.route.id else { return }
        scrollPosition = id
    }

    private func updateNearbyRoutes() {
        nearbyWithRealtimeInfo = nearbyFetcher.withRealtimeInfo(
            schedules: scheduleFetcher.schedules,
            predictions: predictionsFetcher.predictions,
            alerts: alertsFetcher.alerts,
            filterAtTime: now.toKotlinInstant()
        )
    }

    private func onSceneChange(_ phase: ScenePhase) {
        Task {
            if phase == .inactive {
                leavePredictions()
            } else if phase == .active {
                joinPredictions()
            } else if phase == .background {
                leavePredictions()
            }
        }
    }

    func updatePinnedRoutes() {
        Task {
            do {
                let repository = pinnedRouteRepositoryProvider.repository
                pinnedRoutes = try await repository.getPinnedRoutes()
            } catch {
                debugPrint(error)
            }
        }
    }

    func toggledPinnedRoute(_ routeId: String) {
        Task {
            do {
                let usecase = togglePinnedUsecaseProvider.usecase
                try await usecase.execute(route: routeId)
                updatePinnedRoutes()
            } catch {
                debugPrint(error)
            }
        }
    }
}

struct NearbyTransitView_Previews: PreviewProvider {
    static var previews: some View {
        let busRoute = Route(
            id: "216",
            type: RouteType.bus,
            color: "FFC72C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Houghs Neck", "Quincy Center Station"],
            longName: "Houghs Neck - Quincy Center Station via Germantown",
            shortName: "216",
            sortOrder: 52160,
            textColor: "000000",
            routePatternIds: ["216-_-1"]
        )
        let busStop = Stop(
            id: "3276",
            latitude: 42.265969,
            longitude: -70.969853,
            name: "Sea St opp Peterson Rd",
            locationType: LocationType.stop,
            parentStationId: nil,
            childStopIds: []
        )
        let busTrip = Trip(
            id: "trip1",
            directionId: 1,
            headsign: "Houghs Neck",
            routePatternId: "216-_-1",
            shapeId: "2160144",
            stopIds: nil
        )
        let busPattern = RoutePattern(
            id: "216-_-1",
            directionId: 1,
            name: "Houghs Neck - Quincy Center Station",
            sortOrder: 521_601_000,
            typicality: .typical,
            representativeTripId: busTrip.id,
            routeId: busRoute.id
        )
        let busPrediction1 = Prediction(
            id: "1",
            arrivalTime: nil,
            departureTime: (Date.now + 5 * 60).toKotlinInstant(),
            directionId: 0,
            revenue: true,
            scheduleRelationship: .scheduled,
            status: nil,
            stopSequence: 30,
            routeId: "216",
            stopId: busStop.id,
            tripId: busTrip.id,
            vehicleId: nil
        )
        let busPrediction2 = Prediction(
            id: "2",
            arrivalTime: nil,
            departureTime: (Date.now + 12 * 60).toKotlinInstant(),
            directionId: 0,
            revenue: true,
            scheduleRelationship: .scheduled,
            status: nil,
            stopSequence: 90,
            routeId: "216",
            stopId: busStop.id,
            tripId: busTrip.id,
            vehicleId: nil
        )
        let crRoute = Route(
            id: "CR-Providence",
            type: RouteType.commuterRail,
            color: "80276C",
            directionNames: ["Outbound", "Inbound"],
            directionDestinations: ["Stoughton or Wickford Junction", "South Station"],
            longName: "Providence/Stoughton Line",
            shortName: "",
            sortOrder: 20012,
            textColor: "FFFFFF",
            routePatternIds: nil
        )
        let crStop = Stop(
            id: "place-sstat",
            latitude: 42.265969,
            longitude: -70.969853,
            name: "South Station",
            locationType: LocationType.stop,
            parentStationId: nil,
            childStopIds: []
        )
        let crTrip = Trip(
            id: "canonical-CR-Providence-C1-0",
            directionId: 0,
            headsign: "Wickford Junction",
            routePatternId: "CR-Providence-C1-0",
            shapeId: "canonical-9890009",
            stopIds: nil
        )
        let crPattern = RoutePattern(
            id: "CR-Providence-C1-0",
            directionId: 0,
            name: "South Station - Wickford Junction via Back Bay",
            sortOrder: 200_120_991,
            typicality: .typical,
            representativeTripId: crTrip.id,
            routeId: crRoute.id
        )
        let crPrediction1 = Prediction(
            id: "1",
            arrivalTime: nil,
            departureTime: (Date.now + 32 * 60).toKotlinInstant(),
            directionId: 0,
            revenue: true,
            scheduleRelationship: .scheduled,
            status: nil,
            stopSequence: 30,
            routeId: crRoute.id,
            stopId: "place-sstat",
            tripId: crTrip.id,
            vehicleId: nil
        )
        let crPrediction2 = Prediction(
            id: "2",
            arrivalTime: nil,
            departureTime: (Date.now + 72 * 60).toKotlinInstant(),
            directionId: 0,
            revenue: true,
            scheduleRelationship: .scheduled,
            status: nil,
            stopSequence: 90,
            routeId: crRoute.id,
            stopId: "place-sstat",
            tripId: crTrip.id,
            vehicleId: nil
        )
        List {
            NearbyRouteView(
                nearbyRoute: StopAssociatedRoute(
                    route: busRoute,
                    patternsByStop: [
                        PatternsByStop(
                            route: busRoute,
                            stop: busStop,
                            patternsByHeadsign: [
                                PatternsByHeadsign(
                                    route: busRoute,
                                    headsign: "Houghs Neck",
                                    patterns: [busPattern],
                                    upcomingTrips: [
                                        UpcomingTrip(trip: busTrip, prediction: busPrediction1),
                                        UpcomingTrip(trip: busTrip, prediction: busPrediction2),
                                    ],
                                    alertsHere: nil
                                ),
                            ]
                        ),
                    ]
                ),
                pinned: false,
                onPin: { _ in },
                now: Date.now.toKotlinInstant()
            )
            NearbyRouteView(
                nearbyRoute: StopAssociatedRoute(
                    route: crRoute,
                    patternsByStop: [
                        PatternsByStop(
                            route: crRoute,
                            stop: crStop,
                            patternsByHeadsign: [
                                PatternsByHeadsign(
                                    route: crRoute,
                                    headsign: "Houghs Neck",
                                    patterns: [crPattern],
                                    upcomingTrips: [
                                        UpcomingTrip(trip: crTrip, prediction: crPrediction1),
                                        UpcomingTrip(trip: crTrip, prediction: crPrediction2),
                                    ],
                                    alertsHere: nil
                                ),
                            ]
                        ),
                    ]
                ),
                pinned: true,
                onPin: { _ in },
                now: Date.now.toKotlinInstant()
            )
        }.previewDisplayName("NearbyRouteView")
    }
}
