//
//  SaveFavoritesFlow.swift
//  iosApp
//
//  Created by Kayla Brady on 6/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//
import Foundation
import Shared
import SwiftUI

enum SaveFavoritesContext {
    case Favorites
    case StopDetails
}

struct SaveFavorietsFlow: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let stop: Stop
    let directions: [Direction]
    let selectedDirection: Int32
    let context: SaveFavoritesContext
    let isFavorite: (RouteStopDirection) -> Bool
    let updateFavorites: ([RouteStopDirection: Bool]) -> Void
    let onClose: () -> Void

    @State var showDialog = false

    var isUnFavoriting: Bool {
        (directions.contains(where: { $0.id == selectedDirection })) &&
            isFavorite(RouteStopDirection(route: lineOrRoute.id, stop: stop.id, direction: selectedDirection))
    }

    var isBusOneDirection: Bool { directions.count == 1 && lineOrRoute.sortRoute.type == RouteType.bus }

    var body: some View {
        // Save automatically without confirmation modal
        if isUnFavoriting || isBusOneDirection, directions.contains(where: { $0.id == selectedDirection }) {
            let _ = print("SaveFavoritesFlow: Automatic")
            VStack {}
                .onAppear {
                    let rsd = RouteStopDirection(route: lineOrRoute.id, stop: stop.id, direction: selectedDirection)
                    updateFavorites([rsd: !isFavorite(rsd)])

                    onClose()
                }
        } else {
            VStack {}
                .onAppear {
                    print("SaveFavoritesFlow: showDialog to true")
                    showDialog = true
                }
                .alert("Favorites Confirmation", isPresented: $showDialog, actions: {}, message: {})
        }
    }
}
