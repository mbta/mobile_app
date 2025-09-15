//
//  FavoritesModifier.swift
//  iosApp
//
//  Created by esimon on 9/4/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

class LoadedFavorites {
    static var last = Favorites(routeStopDirection: [])
}

struct FavoritesModifier: ViewModifier {
    var favoritesRepository: IFavoritesRepository = RepositoryDI().favorites
    @Binding var favorites: Favorites
    @Binding var awaitingUpdate: Bool

    func loadFavorites() {
        favorites = LoadedFavorites.last
        Task {
            do {
                let nextFavorites = try await favoritesRepository.getFavorites()
                Task { @MainActor in
                    favorites = nextFavorites
                    LoadedFavorites.last = nextFavorites
                    awaitingUpdate = false
                }
            } catch is CancellationError {
                // do nothing on cancellation
            } catch {
                // getFavorites shouldn't actually fail
                debugPrint(error)
            }
        }
    }

    func body(content: Content) -> some View {
        content
            .onAppear { loadFavorites() }
            .onChange(of: awaitingUpdate) { shouldUpdate in
                guard shouldUpdate else { return }
                loadFavorites()
            }
    }
}

public extension View {
    func favorites(_ favorites: Binding<Favorites>, awaitingUpdate: Binding<Bool>) -> some View {
        modifier(FavoritesModifier(favorites: favorites, awaitingUpdate: awaitingUpdate))
    }
}
