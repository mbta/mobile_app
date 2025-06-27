//
//  SaveFavoritesFlow.swift
//  iosApp
//
//  Created by Kayla Brady on 6/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//
import CustomAlert
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
            FavoriteConfirmationDialog(lineOrRoute: lineOrRoute,
                                       stop: stop,
                                       directions: directions,
                                       selectedDirection: selectedDirection,
                                       context: context,
                                       proposedFavorites: directions.reduce(into: [Int32: Bool]()) { acc, direction in

                                           // if selectedDirection and already a favorite, then removing favorite.
                                           // if this is not the selected direction and already a favorite, then keep
                                           // it.
                                           let isFavorite = ((direction.id == selectedDirection) !=
                                               isFavorite(RouteStopDirection(
                                                   route: lineOrRoute.id,
                                                   stop: stop.id,
                                                   direction: direction.id
                                               ))) ||
                                               // If the only direction is the opposite one, mark it as favorite
                                               // whether or not it already is
                                               (directions.count == 1 && direction.id != selectedDirection)
                                           acc[direction.id] = isFavorite
                                       },
                                       updateFavorites: updateFavorites, onClose: onClose)
        }
    }
}

struct FavoriteConfirmationDialog: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let stop: Stop
    let directions: [Direction]
    let selectedDirection: Int32
    let context: SaveFavoritesContext
    let proposedFavorites: [Int32: Bool]
    let updateFavorites: ([RouteStopDirection: Bool]) -> Void
    let onClose: () -> Void
    @State var showDialog = false
    var body: some View {
        let headerText = if context == SaveFavoritesContext.Favorites { String(format: NSLocalizedString(
            "Add **%1$@** at **%2$@**",
            comment: """
            """
        ), lineOrRoute.name, stop.name)
        } else {
            String(format: NSLocalizedString(
                "Add **%1$@** at **%2$@** to Favorites",
                comment: """
                """
            ), lineOrRoute.name, stop.name)
        }

        VStack {}
            .onAppear {
                print("SaveFavoritesFlow: showDialog to true")
                showDialog = true
            }
            .customAlert(
                Text(AttributedString.tryMarkdown(headerText)).bold(false),
                isPresented: $showDialog,
                content: {
                    VStack {
                        if directions.count == 1, directions.first!.id != selectedDirection {
                            Text("\(DirectionLabel.directionNameFormatted(directions.first!)) service only")
                        }
                    }
                }
            )
    }
}
