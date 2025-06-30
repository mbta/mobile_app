//
//  SaveFavoritesFlow.swift
//  iosApp
//
//  Created by Kayla Brady on 6/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//
import Collections
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
    @State var favoritesToSave: OrderedDictionary<Direction, Bool> = [:]
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
                favoritesToSave = directions.reduce(into: OrderedDictionary()) { acc, direction in
                    acc[direction] = proposedFavorites[direction.id] ?? false
                }
                showDialog = true
            }
            .customAlert(
                isPresented: $showDialog,
                content: {
                    VStack {
                        Text(AttributedString.tryMarkdown(headerText))
                            .font(Typography.body)
                            .accessibilityAddTraits(.isHeader)
                            .accessibilityHeading(.h1)
                        if directions.count == 1, directions.first!.id != selectedDirection {
                            Text("\(DirectionLabel.directionNameFormatted(directions.first!)) service only")
                                .font(Typography.footnoteSemibold)
                                .padding(.horizontal, 16)
                                .padding(.top, 8)
                        }
                        VStack(spacing: 0) {
                            directionButtons
                        }
                    }.accessibilityAddTraits(.isModal)
                },
                actions: {
                    MultiButton {
                        Button {
                            onClose()
                        } label: {
                            Text("Cancel")
                        }

                        Button {
                            updateFavorites(favoritesToSave
                                .reduce(into: [RouteStopDirection: Bool]()) { partialResult, entry in
                                    partialResult[RouteStopDirection(
                                        route: lineOrRoute.id,
                                        stop: stop.id,
                                        direction: entry.key.id
                                    )] = entry.value
                                })
                            onClose()
                        } label: {
                            Text("Add")
                        }.disabled(favoritesToSave.values.allSatisfy { $0 == false })
                    }
                }
            )
    }

    /**
     Convenience struct to support iterating through the map of favoritesToSave entries via `ForEach`.
     Cannot use an id of direction.id, as we need to rerender each row when the value of isFavorite changes.
     */
    struct DirectionFavorite: Hashable {
        let direction: Direction
        let isFavorite: Bool
    }

    @ViewBuilder var directionButtons: some View {
        let directionValues: [DirectionFavorite] = favoritesToSave.map { DirectionFavorite(
            direction: $0.key,
            isFavorite: $0.value
        ) }
        VStack(spacing: 0) {
            ForEach(
                directionValues.enumerated().sorted(by: { $0.element.direction.id < $1.element.direction.id }),
                id: \.element.hashValue
            ) { index, directionValue in
                Button(action: {
                    favoritesToSave[directionValue.direction] = !directionValue.isFavorite
                }) {
                    VStack(spacing: 0) {
                        HStack(spacing: 0) {
                            DirectionLabel(direction: directionValue.direction)
                                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                            Spacer()
                            StarIcon(pinned: directionValue.isFavorite, color: .init(hex: lineOrRoute.backgroundColor))
                        }
                        .fixedSize(horizontal: false, vertical: true)
                        .padding(.horizontal, 16)
                        .padding(.vertical, 12)
                        if index < directionValues.count - 1 {
                            HaloSeparator()
                        }
                    }
                }
                .accessibilityAddTraits(directionValue.isFavorite ? [.isSelected] : [])
                .fullFocusSize()
                .background(Color.fill3)
            }
        }.withRoundedBorder()
            .padding(.horizontal, 12)
            .padding(.top, 8)
    }
}
