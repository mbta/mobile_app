//
//  NearbyTransitView.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-01-19.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import CoreLocation
@_spi(Experimental) import MapboxMaps
import os
import Shared
import SwiftUI

struct NearbyTransitView: View {
    var analytics: Analytics = AnalyticsProvider.shared
    @State var predictionsRepository = RepositoryDI().predictions
    var schedulesRepository = RepositoryDI().schedules
    @Binding var location: CLLocationCoordinate2D?
    let setIsReturningFromBackground: (Bool) -> Void
    @State var favorites: Favorites = LoadedFavorites.last
    @State var globalData: GlobalResponse?
    @ObservedObject var nearbyVM: NearbyViewModel
    @State var scheduleResponse: ScheduleResponse?
    @State var now = Date.now
    @State var predictionsByStop: PredictionsByStopJoinResponse?

    var errorBannerRepository = RepositoryDI().errorBanner
    let noNearbyStops: () -> NoNearbyStopsView

    let inspection = Inspection<Self>()
    let scrollSubject = PassthroughSubject<LineOrRoute.Id, Never>()

    struct RouteCardParams: Equatable {
        let state: NearbyViewModel.NearbyTransitState
        let global: GlobalResponse?
        let schedules: ScheduleResponse?
        let predictions: PredictionsByStopJoinResponse?
        let alerts: AlertsStreamDataResponse?
        let now: Date
    }

    var body: some View {
        VStack(spacing: 0) {
            if let routeCardData = nearbyVM.routeCardData,
               let global = globalData {
                nearbyList(routeCardData, global)
                    .onAppear { didLoadData?(self) }
            } else {
                loadingBody()
            }
        }
        .favorites($favorites)
        .global($globalData, errorKey: "NearbyTransitView")
        .onAppear {
            loadEverything()
            didAppear?(self)
        }
        .onChange(of: globalData) { globalData in
            getNearby(location: location, globalData: globalData)
        }
        .onChange(of: location) { newLocation in
            getNearby(location: newLocation, globalData: globalData)
        }
        .onChange(of: nearbyVM.nearbyState.stopIds) { [oldValue = nearbyVM.nearbyState.stopIds] newNearbyStops in
            let oldSet = oldValue != nil ? Set(oldValue ?? []) : nil
            let newSet = newNearbyStops != nil ? Set(newNearbyStops ?? []) : nil
            if oldSet != newSet {
                getSchedule()
                joinPredictions(newNearbyStops)
                scrollToTop()
            }
        }
        .onChange(of: RouteCardParams(
            state: nearbyVM.nearbyState,
            global: globalData,
            schedules: scheduleResponse,
            predictions: predictionsByStop,
            alerts: nearbyVM.alerts,
            now: now,
        )) { newParams in
            DispatchQueue.main.async {
                nearbyVM.loadRouteCardData(
                    state: newParams.state,
                    global: newParams.global,
                    schedules: newParams.schedules,
                    predictions: newParams.predictions,
                    alerts: newParams.alerts,
                    now: newParams.now,
                )
            }
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
        .onDisappear {
            leavePredictions()
        }
        .task {
            while !Task.isCancelled {
                now = Date.now
                checkPredictionsStale()
                try? await Task.sleep(for: .seconds(5))
            }
        }
        .withScenePhaseHandlers(
            onActive: {
                if let predictionsByStop,
                   predictionsRepository
                   .shouldForgetPredictions(predictionCount: predictionsByStop.predictionQuantity()) {
                    self.predictionsByStop = nil
                }
                joinPredictions(nearbyVM.nearbyState.stopIds)
            },
            onInactive: leavePredictions,
            onBackground: {
                leavePredictions()
                setIsReturningFromBackground(true)
            }
        )
    }

    @ViewBuilder private func nearbyList(_ routeCardData: [RouteCardData], _ global: GlobalResponse) -> some View {
        if routeCardData.isEmpty {
            ScrollView {
                noNearbyStops().padding(.horizontal, 16)
                Spacer()
            }
        } else {
            ScrollViewReader { proxy in
                HaloScrollView {
                    LazyVStack(spacing: 18) {
                        ForEach(routeCardData) { cardData in
                            RouteCard(
                                cardData: cardData,
                                global: global,
                                now: now.toEasternInstant(),
                                isFavorite: { favorites.isFavorite($0) },
                                pushNavEntry: { entry in nearbyVM.pushNavEntry(entry) },
                                showStopHeader: true
                            )
                        }
                    }
                    .padding(.vertical, 4)
                    .padding(.horizontal, 16)
                }
                .onReceive(scrollSubject) { id in
                    withAnimation {
                        proxy.scrollTo(id, anchor: .top)
                    }
                }
            }
        }
    }

    @ViewBuilder private func loadingBody() -> some View {
        ScrollView([]) {
            LazyVStack(spacing: 18) {
                ForEach(1 ... 5, id: \.self) { _ in
                    RouteCard(
                        cardData: LoadingPlaceholders.shared.nearbyRoute(),
                        global: globalData,
                        now: now.toEasternInstant(),
                        isFavorite: { _ in false },
                        pushNavEntry: { _ in },
                        showStopHeader: true
                    )
                    .loadingPlaceholder()
                }
            }
            .padding(.vertical, 4)
            .padding(.horizontal, 16)
        }
    }

    var didAppear: ((Self) -> Void)?
    var didLoadData: ((Self) -> Void)?

    private func loadEverything() {
        getNearby(location: location, globalData: globalData)
        joinPredictions(nearbyVM.nearbyState.stopIds)
        getSchedule()
    }

    func getNearby(location: CLLocationCoordinate2D?, globalData: GlobalResponse?) {
        self.location = location
        self.globalData = globalData
        guard let globalData else { return }
        guard let location else {
            // if location was set to nil, forget previously loaded data
            predictionsByStop = nil
            scheduleResponse = nil
            return
        }
        nearbyVM.getNearbyStops(global: globalData, location: location)
    }

    func getSchedule() {
        Task {
            guard let stopIds = nearbyVM.nearbyState.stopIds else { return }
            await fetchApi(
                errorKey: "NearbyTransitView.getSchedule",
                getData: { try await schedulesRepository.getSchedule(stopIds: stopIds) },
                onSuccess: { scheduleResponse = $0 },
                onRefreshAfterError: loadEverything
            )
        }
    }

    func joinPredictions(_ stopIds: [String]?) {
        guard let stopIds else { return }
        predictionsRepository.connectV2(stopIds: stopIds, onJoin: { outcome in
            DispatchQueue.main.async {
                switch onEnum(of: outcome) {
                case let .ok(result):
                    predictionsByStop = result.data
                    checkPredictionsStale()
                case .error: break
                }
                setIsReturningFromBackground(false)
            }
        }, onMessage: { outcome in
            DispatchQueue.main.async {
                switch onEnum(of: outcome) {
                case let .ok(result):
                    if let existingPredictionsByStop = predictionsByStop {
                        predictionsByStop = existingPredictionsByStop.mergePredictions(updatedPredictions: result.data)
                    } else {
                        predictionsByStop = PredictionsByStopJoinResponse(
                            partialResponse: result.data
                        )
                    }
                    checkPredictionsStale()
                case .error: break
                }
                setIsReturningFromBackground(false)
            }

        })
    }

    func leavePredictions() {
        predictionsRepository.disconnect()
    }

    private func checkPredictionsStale() {
        if let lastPredictions = predictionsRepository.lastUpdated {
            errorBannerRepository.checkPredictionsStale(
                predictionsLastUpdated: lastPredictions,
                predictionQuantity: Int32(
                    predictionsByStop?.predictionQuantity() ??
                        0
                ),
                action: {
                    leavePredictions()
                    joinPredictions(nearbyVM.nearbyState.stopIds)
                }
            )
        }
    }

    private func scrollToTop() {
        guard let id = nearbyVM.routeCardData?.first?.lineOrRoute.id else { return }
        scrollSubject.send(id)
    }
}
