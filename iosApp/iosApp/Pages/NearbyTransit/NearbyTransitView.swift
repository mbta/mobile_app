//
//  NearbyTransitView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-19.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
import FirebaseAnalytics
import os
import shared
import SwiftUI
@_spi(Experimental) import MapboxMaps

struct NearbyTransitView: View {
    var analytics: NearbyTransitAnalytics = AnalyticsProvider.shared
    var togglePinnedUsecase = UsecaseDI().toggledPinnedRouteUsecase
    var pinnedRouteRepository = RepositoryDI().pinnedRoutes
    @State var predictionsRepository = RepositoryDI().predictions
    var schedulesRepository = RepositoryDI().schedules
    var getNearby: (GlobalResponse, CLLocationCoordinate2D) -> Void
    @Binding var state: NearbyViewModel.NearbyTransitState
    @Binding var location: CLLocationCoordinate2D?
    var globalRepository = RepositoryDI().global
    @State var globalData: GlobalResponse?
    @ObservedObject var nearbyVM: NearbyViewModel
    @State var scheduleResponse: ScheduleResponse?
    @State var nearbyWithRealtimeInfo: [StopsAssociated]?
    @State var now = Date.now
    @State var pinnedRoutes: Set<String> = []
    @State var predictions: PredictionsStreamDataResponse?
    @State var predictionsError: SocketError?

    let timer = Timer.publish(every: 5, on: .main, in: .common).autoconnect()
    let inspection = Inspection<Self>()
    let scrollSubject = PassthroughSubject<String, Never>()

    var body: some View {
        VStack {
            if let nearbyWithRealtimeInfo {
                nearbyList(nearbyWithRealtimeInfo)
            } else {
                LoadingCard()
            }
        }
        .onAppear {
            getGlobal()
            getNearby(location: location, globalData: globalData)
            joinPredictions(state.nearbyByRouteAndStop?.stopIds())
            updateNearbyRoutes()
            updatePinnedRoutes()
            getSchedule()
            didAppear?(self)
        }
        .onChange(of: globalData) { globalData in
            getNearby(location: location, globalData: globalData)
        }
        .onChange(of: location) { newLocation in
            getNearby(location: newLocation, globalData: globalData)
        }
        .onChange(of: state.nearbyByRouteAndStop) { nearbyByRouteAndStop in
            updateNearbyRoutes()
            getSchedule()
            joinPredictions(nearbyByRouteAndStop?.stopIds())
            scrollToTop()
        }
        .onChange(of: scheduleResponse) { response in
            updateNearbyRoutes(scheduleResponse: response)
        }
        .onChange(of: predictions) { predictions in
            updateNearbyRoutes(predictions: predictions)
        }
        .onChange(of: nearbyVM.alerts) { alerts in
            updateNearbyRoutes(alerts: alerts)
        }
        .onReceive(timer) { input in
            now = input
            updateNearbyRoutes()
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onDisappear {
            leavePredictions()
        }
        .withScenePhaseHandlers(
            onActive: { joinPredictions(state.nearbyByRouteAndStop?.stopIds()) },
            onInactive: leavePredictions,
            onBackground: leavePredictions
        )
        .replaceWhen(state.error) { errorText in
            errorCard(errorText)
        }
    }

    @ViewBuilder private func nearbyList(_ transit: [StopsAssociated]) -> some View {
        if transit.isEmpty {
            VStack(spacing: 8) {
                Spacer()
                Text("No nearby MBTA stops")
                    .font(Typography.headlineBold)
                Text("Your current location is outside of our search area.")
                Spacer()
            }
        } else {
            ScrollViewReader { proxy in
                ScrollView {
                    LazyVStack {
                        ForEach(transit, id: \.id) { nearbyTransit in
                            switch onEnum(of: nearbyTransit) {
                            case let .withRoute(nearbyRoute):
                                NearbyRouteView(
                                    nearbyRoute: nearbyRoute,
                                    pinned: pinnedRoutes.contains(nearbyRoute.route.id),
                                    onPin: { id in toggledPinnedRoute(id) },
                                    pushNavEntry: nearbyVM.pushNavEntry,
                                    now: now.toKotlinInstant()
                                )
                            case let .withLine(nearbyLine):
                                NearbyLineView(
                                    nearbyLine: nearbyLine,
                                    pinned: pinnedRoutes.contains(nearbyLine.line.id),
                                    onPin: { id in toggledPinnedRoute(id) },
                                    pushNavEntry: nearbyVM.pushNavEntry,
                                    now: now.toKotlinInstant()
                                )
                            }
                        }
                    }
                }
                .onReceive(scrollSubject) { id in
                    withAnimation {
                        proxy.scrollTo(id, anchor: .top)
                    }
                }
                .putAboveWhen(predictionsError) { error in
                    IconCard(iconName: "network.slash", details: Text(error.predictionsErrorText))
                }
            }
        }
    }

    private func errorCard(_ errorText: String) -> some View {
        IconCard(iconName: "network.slash", details: Text(errorText))
            .refreshable(state.loading) { getNearby(location: location, globalData: globalData) }
    }

    var didAppear: ((Self) -> Void)?

    func getGlobal() {
        Task {
            globalData = try await globalRepository.getGlobalData()
            // this should be handled by the onChange but in tests it just isn't
            getNearby(location: location, globalData: globalData)
        }
    }

    func getNearby(location: CLLocationCoordinate2D?, globalData: GlobalResponse?) {
        self.location = location
        self.globalData = globalData
        guard let location, let globalData else { return }
        getNearby(globalData, location)
    }

    func getSchedule() {
        Task {
            guard let stopIds = state.nearbyByRouteAndStop?
                .stopIds() else { return }
            let stopIdList = Array(stopIds)
            scheduleResponse = try await schedulesRepository.getSchedule(stopIds: stopIdList)
        }
    }

    func joinPredictions(_ stopIds: Set<String>?) {
        guard let stopIds else { return }
        predictionsRepository.connect(stopIds: Array(stopIds)) { outcome in
            DispatchQueue.main.async {
                if let data = outcome.data {
                    predictions = data
                    predictionsError = nil
                } else if let error = outcome.error {
                    predictionsError = error.toSwiftEnum()
                }
            }
        }
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    func updatePinnedRoutes() {
        Task {
            do {
                pinnedRoutes = try await pinnedRouteRepository.getPinnedRoutes()
                updateNearbyRoutes(pinnedRoutes: pinnedRoutes)
            } catch {
                debugPrint(error)
            }
        }
    }

    func toggledPinnedRoute(_ routeId: String) {
        Task {
            do {
                let pinned = try await togglePinnedUsecase.execute(route: routeId).boolValue
                analytics.toggledPinnedRoute(pinned: pinned, routeId: routeId)
                updatePinnedRoutes()
            } catch {
                debugPrint(error)
            }
        }
    }

    private func scrollToTop() {
        guard let id = nearbyWithRealtimeInfo?.first?.sortRoute().id else { return }
        scrollSubject.send(id)
    }

    private func updateNearbyRoutes(
        scheduleResponse: ScheduleResponse? = nil,
        predictions: PredictionsStreamDataResponse? = nil,
        alerts: AlertsStreamDataResponse? = nil,
        pinnedRoutes: Set<String>? = nil
    ) {
        nearbyWithRealtimeInfo = withRealtimeInfo(
            schedules: scheduleResponse ?? self.scheduleResponse,
            predictions: predictions ?? self.predictions,
            alerts: alerts ?? nearbyVM.alerts,
            filterAtTime: now.toKotlinInstant(),
            pinnedRoutes: pinnedRoutes ?? self.pinnedRoutes
        )
    }

    private func withRealtimeInfo(
        schedules: ScheduleResponse?,
        predictions: PredictionsStreamDataResponse?,
        alerts: AlertsStreamDataResponse?,
        filterAtTime: Instant,
        pinnedRoutes: Set<String>
    ) -> [StopsAssociated]? {
        guard let loadedLocation = state.loadedLocation else { return nil }
        return state.nearbyByRouteAndStop?.withRealtimeInfo(
            sortByDistanceFrom: .init(longitude: loadedLocation.longitude, latitude: loadedLocation.latitude),
            schedules: schedules,
            predictions: predictions,
            alerts: alerts,
            filterAtTime: filterAtTime,
            pinnedRoutes: pinnedRoutes
        )
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
            lineId: "line-214216",
            routePatternIds: ["216-_-1"]
        )
        let busStop = Stop(
            id: "3276",
            latitude: 42.265969,
            longitude: -70.969853,
            name: "Sea St opp Peterson Rd",
            locationType: LocationType.stop,
            description: nil,
            platformName: nil,
            vehicleType: .bus,
            childStopIds: [],
            connectingStopIds: [],
            parentStationId: nil
        )
        let busTrip = Trip(
            id: "trip1",
            directionId: 1,
            headsign: "Houghs Neck",
            routeId: "216",
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
            lineId: "line-Providence",
            routePatternIds: nil
        )
        let crStop = Stop(
            id: "place-sstat",
            latitude: 42.265969,
            longitude: -70.969853,
            name: "South Station",
            locationType: LocationType.stop,
            description: nil,
            platformName: nil,
            vehicleType: nil,
            childStopIds: [],
            connectingStopIds: [],
            parentStationId: nil
        )
        let crTrip = Trip(
            id: "canonical-CR-Providence-C1-0",
            directionId: 0,
            headsign: "Wickford Junction",
            routeId: "CR-Providence",
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
                nearbyRoute: StopsAssociated.WithRoute(
                    route: busRoute,
                    patternsByStop: [
                        PatternsByStop(
                            route: busRoute,
                            stop: busStop,
                            patterns: [
                                RealtimePatterns.ByHeadsign(
                                    route: busRoute,
                                    headsign: busTrip.headsign,
                                    line: nil,
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
                pushNavEntry: { _ in },
                now: Date.now.toKotlinInstant()
            )
            NearbyRouteView(
                nearbyRoute: StopsAssociated.WithRoute(
                    route: crRoute,
                    patternsByStop: [
                        PatternsByStop(
                            route: crRoute,
                            stop: crStop,
                            patterns: [
                                RealtimePatterns.ByHeadsign(
                                    route: crRoute,
                                    headsign: crTrip.headsign,
                                    line: nil,
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
                pushNavEntry: { _ in },
                now: Date.now.toKotlinInstant()
            )
        }.font(Typography.body).previewDisplayName("NearbyRouteView")
    }
}
