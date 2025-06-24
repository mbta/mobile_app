//
//  FavoriteConfirmation.swift
//  iosApp
//
//  Created by Kayla Brady on 6/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//
import Foundation
import Shared
import SwiftUI

struct FavoriteConfirmation: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let stop: Stop
    let directions: [Direction]
    let selectedDirection: Int32
    let isFavorite: (RouteStopDirection) -> Bool
    let updateFavorites: ([RouteStopDirection: Bool]) -> Void
    let onClose: () -> Void

    @State var showDialog = false

    var body: some View {
        if directions.count < 2, directions.allSatisfy({ $0.id == selectedDirection }) {
            EmptyView()
                .onAppear {
                    updateFavorites(
                        directions.reduce(into: [RouteStopDirection: Bool]()) {
                            $0[RouteStopDirection(route: lineOrRoute.id, stop: stop.id, direction: $1.id)] =
                                !isFavorite(RouteStopDirection(
                                    route: lineOrRoute.id,
                                    stop: stop.id,
                                    direction: selectedDirection
                                ))
                        }
                    )
                    onClose()
                }
        } else {
            EmptyView()
                .onAppear { showDialog = true }
                .alert("Favorites Confirmation", isPresented: $showDialog, actions: {}, message: {})
        }
    }
}
