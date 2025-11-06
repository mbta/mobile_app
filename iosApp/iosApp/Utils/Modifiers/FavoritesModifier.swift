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
    static var last = Favorites(routeStopDirection: [:])
}

struct FavoritesModifier: ViewModifier {
    @State var favoritesVM = ViewModelDI().favorites
    @Binding var favorites: Favorites = LoadedFavorites.last

    @MainActor
    func activateListener() async {
        for await state in favoritesVM.models {
            favorites = if let nextFavorites = state.favorites {
                Favorites(routeStopDirection: nextFavorites)
            } else {
                Favorites(routeStopDirection: [:])
            }
            LoadedFavorites.last = favorites
        }
    }

    func loadFavorites() {
        Task(priority: .high) {
            await activateListener()
        }
        Task {
            favoritesVM.reloadFavorites()
        }
    }

    func body(content: Content) -> some View {
        content.onAppear { loadFavorites() }
    }
}

public extension View {
    func favorites(_ favorites: Binding<Favorites>) -> some View {
        modifier(FavoritesModifier(favorites: favorites))
    }
}
