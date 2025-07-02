//
//  SaveFavoritesFlow.swift
//  iosApp
//
//  Created by Kayla Brady on 6/24/25.
//  Copyright © 2025 MBTA. All rights reserved.
//
import Collections
import Foundation
import MijickPopups
import Shared
import SwiftUI

enum SaveFavoritesContext {
    case favorites
    case stopDetails
}

let popupId = "favoritesPopup"

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

    var body: some View {
        VStack {}
            .onAppear {
                Task {
                    await FavoriteConfirmationPopup(lineOrRoute: lineOrRoute,
                                                    stop: stop,
                                                    directions: directions,
                                                    selectedDirection: selectedDirection,
                                                    context: context,
                                                    proposedFavorites: proposedFavorites,
                                                    updateFavorites: updateFavorites,
                                                    onClose: {
                                                        Task { await dismissPopup(popupId) }
                                                        onClose()
                                                    })
                                                    .setCustomID(popupId)
                                                    .present()
                }
            }
    }
}

struct FavoriteConfirmationPopup: CenterPopup {
    let lineOrRoute: RouteCardData.LineOrRoute
    let stop: Stop
    let directions: [Direction]
    let selectedDirection: Int32
    let context: SaveFavoritesContext
    let proposedFavorites: [Int32: Bool]
    let updateFavorites: ([RouteStopDirection: Bool]) -> Void
    let onClose: () -> Void

    @State var favoritesToSave: [Direction: Bool] = [:]

    var body: some View {
        VStack(spacing: 16) {
            FavoriteConfirmationDialogContents(lineOrRoute: lineOrRoute,
                                               stop: stop, directions: directions,
                                               selectedDirection: selectedDirection,
                                               context: context,
                                               favoritesToSave: favoritesToSave,
                                               updateLocalFavorite: { direction, isFavorite in
                                                   favoritesToSave[direction] = isFavorite
                                               }).padding(.horizontal, 8)
            FavoriteConfirmationDialogActions(lineOrRoute: lineOrRoute,
                                              stop: stop,
                                              favoritesToSave: favoritesToSave,
                                              updateFavorites: updateFavorites,
                                              onClose: onClose)
        }
        .accessibilityAddTraits(.isModal)
        .onAppear {
            favoritesToSave = directions.reduce(into: [Direction: Bool]()) { acc, direction in
                acc[direction] = proposedFavorites[direction.id] ?? false
            }
        }
        .padding(.top, 16)
        .background(Color.fill2)
    }

    func configurePopup(config: CenterPopupConfig) -> CenterPopupConfig {
        config
            .popupHorizontalPadding(40)
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
                                             Title for a confirmation modal when a user adds a favorite favorite.
                                             Ex: Add [Green Line] at [Boylston]
                                             """), lineOrRoute.name, stop.name)
        } else {
            String(format: NSLocalizedString("Add **%1$@** at **%2$@** to Favorites",
                                             comment: """
                                             Title for a confirmation modal when a user adds a favorite route + stop.
                                             Ex: Add [Green Line] at [Boylston] to Favorites
                                             """), lineOrRoute.name, stop.name)
        }

        VStack {
            Text(AttributedString.tryMarkdown(headerText))
                .font(Typography.body)
                .accessibilityAddTraits(.isHeader)
                .accessibilityHeading(.h1)
                .multilineTextAlignment(.center)
            if directions.count == 1, directions.first!.id != selectedDirection {
                Text("\(DirectionLabel.directionNameFormatted(directions.first!)) service only")
                    .font(Typography.footnoteSemibold)
                    .padding(.horizontal, 16)
                    .padding(.top, 8)
                    .multilineTextAlignment(.center)
            }
            VStack(spacing: 0) {
                DirectionButtons(lineOrRoute: lineOrRoute,
                                 favorites: favoritesToSave,
                                 updateLocalFavorite: updateLocalFavorite)
            }
        }
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

    var directionValues: [DirectionFavorite] { favorites.map { DirectionFavorite(
        direction: $0.key,
        isFavorite: $0.value
    ) }
    }

    var body: some View {
        let sortedDirections = directionValues
            .sorted(by: { $0.direction.id < $1.direction.id })
            .enumerated()
            .sorted(by: { $0.offset < $1.offset })
        VStack(spacing: 0) {
            ForEach(
                sortedDirections,
                id: \.element.hashValue
            ) { index, directionValue in
                Button(action: {
                    updateLocalFavorite(directionValue.direction, !directionValue.isFavorite)
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

                        if index < sortedDirections.count - 1 {
                            HaloSeparator()
                        }
                    }
                }
                .foregroundStyle(Color.text)
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
        VStack(spacing: 0) {
            HaloSeparator()
            HStack(spacing: 0) {
                Button {
                    onClose()
                } label: {
                    Text("Cancel")
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .padding(.vertical, 16)
                VerticalHaloSeparator()
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
                }
                .disabled(favoritesToSave.values.allSatisfy { $0 == false })
                .padding(.vertical, 16)
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            .frame(maxWidth: .infinity, maxHeight: .infinity)
        }
    }
}
