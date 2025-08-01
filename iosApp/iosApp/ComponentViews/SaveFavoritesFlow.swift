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
    case favorites
    case stopDetails
}

struct SaveFavoritesFlow: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let stop: Stop
    let directions: [Direction]
    let selectedDirection: Int32
    let context: SaveFavoritesContext
    let isFavorite: (RouteStopDirection) -> Bool
    let updateFavorites: ([RouteStopDirection: Bool]) -> Void
    let onClose: () -> Void

    var selectedDirectionIsAvailableAtStop: Bool { directions.contains(where: { $0.id == selectedDirection }) }

    var isUnFavoriting: Bool {
        selectedDirectionIsAvailableAtStop &&
            isFavorite(RouteStopDirection(route: lineOrRoute.id, stop: stop.id, direction: selectedDirection))
    }

    var isBusOneDirection: Bool { directions.count == 1 && lineOrRoute.type == RouteType.bus }

    let inspection = Inspection<Self>()

    func proposedFavorites() -> [Int32: Bool] {
        directions
            .reduce(into: [Int32: Bool]()) { acc, direction in
                // if selectedDirection and already a favorite, then removing favorite.
                // if this is not the selected direction and already a favorite, then keep it.
                let isFavorite = ((direction.id == selectedDirection) !=
                    isFavorite(RouteStopDirection(
                        route: lineOrRoute.id,
                        stop: stop.id,
                        direction: direction.id
                    ))) ||
                    // If the only direction is the opposite one, mark it as favorite whether or not it already is
                    (directions.count == 1 && direction.id != selectedDirection)
                acc[direction.id] = isFavorite
            }
    }

    var body: some View {
        // Save automatically without confirmation modal
        VStack(spacing: 0) {
            if isUnFavoriting || isBusOneDirection, selectedDirectionIsAvailableAtStop {
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
                                           proposedFavorites: proposedFavorites(),
                                           updateFavorites: updateFavorites, onClose: onClose)
            }
        }.onReceive(inspection.notice) { inspection.visit(self, $0) }
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
    @State var favoritesToSave: [Direction: Bool] = [:]

    var body: some View {
        VStack {}
            .onAppear {
                favoritesToSave = directions.reduce(into: [Direction: Bool]()) { acc, direction in
                    acc[direction] = proposedFavorites[direction.id] ?? false
                }
                showDialog = true
            }
            .customAlert(
                isPresented: $showDialog,
                content: {
                    FavoriteConfirmationDialogContents(lineOrRoute: lineOrRoute,
                                                       stop: stop, directions: directions,
                                                       selectedDirection: selectedDirection,
                                                       context: context,
                                                       favoritesToSave: favoritesToSave,
                                                       updateLocalFavorite: { direction, isFavorite in
                                                           favoritesToSave[direction] = isFavorite
                                                       })
                },
                actions: {
                    if directions.isEmpty {
                        Button {
                            onClose()
                        } label: {
                            Text("Okay")
                        }
                    } else {
                        MultiButton {
                            FavoriteConfirmationDialogActions(lineOrRoute: lineOrRoute,
                                                              stop: stop,
                                                              favoritesToSave: favoritesToSave,
                                                              updateFavorites: updateFavorites,
                                                              onClose: onClose)
                        }
                    }
                }
            )
    }
}

struct FavoriteConfirmationDialogContents: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let stop: Stop
    let directions: [Direction]
    let selectedDirection: Int32
    let context: SaveFavoritesContext
    let favoritesToSave: [Direction: Bool]
    let updateLocalFavorite: (Direction, Bool) -> Void

    var body: some View {
        let headerText = if context == SaveFavoritesContext.favorites {
            String(format: NSLocalizedString("Add **%1$@** at **%2$@**",
                                             comment: """
                                             Title for a confirmation modal when a user adds a favorite route + stop \
                                             and already has the context that what they are adding is a favorite. \
                                             Ex: Add [Green Line] at [Boylston]
                                             """), lineOrRoute.name, stop.name)
        } else {
            String(format: NSLocalizedString("Add **%1$@** at **%2$@** to Favorites",
                                             comment: """
                                             Title for a confirmation modal when a user adds a favorite route + stop. \
                                             Ex: Add [Green Line] at [Boylston] to Favorites
                                             """), lineOrRoute.name, stop.name)
        }

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

            if directions.isEmpty {
                Text("This stop is drop-off only")
                    .font(Typography.footnoteSemibold)
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
            } else {
                VStack(spacing: 0) {
                    DirectionButtons(lineOrRoute: lineOrRoute,
                                     favorites: favoritesToSave,
                                     updateLocalFavorite: updateLocalFavorite)
                }
            }
        }.accessibilityAddTraits(.isModal)
    }
}

struct DirectionButtons: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let favorites: [Direction: Bool]
    let updateLocalFavorite: (Direction, Bool) -> Void

    /**
     Convenience struct to support iterating through the map of favoritesToSave entries via `ForEach`.
     Cannot use an id of direction.id, as we need to rerender each row when the value of isFavorite changes.
     */
    struct DirectionFavorite: Hashable {
        let direction: Direction
        let isFavorite: Bool
    }

    var directionValues: [DirectionFavorite] {
        favorites
            .map { DirectionFavorite(direction: $0.key, isFavorite: $0.value) }
            .sorted(by: { $0.direction.id < $1.direction.id })
    }

    var body: some View {
        VStack(spacing: 0) {
            ForEach(
                directionValues
                    .enumerated()
                    .sorted(by: { $0.offset < $1.offset }),
                id: \.element.direction
            ) { index, directionValue in
                Button(action: {
                    updateLocalFavorite(directionValue.direction, !directionValue.isFavorite)
                }) {
                    VStack(spacing: 0) {
                        HStack(spacing: 0) {
                            DirectionLabel(direction: directionValue.direction)
                                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .leading)
                            Spacer()
                            StarIcon(starred: directionValue.isFavorite, color: .init(hex: lineOrRoute.backgroundColor))
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

struct FavoriteConfirmationDialogActions: View {
    let lineOrRoute: RouteCardData.LineOrRoute
    let stop: Stop
    let favoritesToSave: [Direction: Bool]
    let updateFavorites: ([RouteStopDirection: Bool]) -> Void
    let onClose: () -> Void

    var body: some View {
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
