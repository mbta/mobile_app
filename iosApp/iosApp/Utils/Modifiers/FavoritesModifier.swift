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
    @Binding var favorites: Favorites

    @MainActor
    func activateListener() async {
        for await state in favoritesVM.models {
            let nextFavorites = Favorites(routeStopDirection: state.favorites ?? [:])
            // The shared favorites VM emits on every prediction update, skip unchanged favorites
            guard nextFavorites != favorites else { continue }
            favorites = nextFavorites
            LoadedFavorites.last = nextFavorites
        }
    }

    func body(content: Content) -> some View {
        content
            .onAppear {
                favorites = LoadedFavorites.last
                favoritesVM.reloadFavorites()
            }
            .task(priority: .high) { await activateListener() }
            .enableInjection()
    }
}

public extension View {
    func favorites(_ favorites: Binding<Favorites>) -> some View {
        modifier(FavoritesModifier(favorites: favorites))
    }
}
