//
//  FavoritesModifier.swift
//  iosApp
//
//  Created by esimon on 9/4/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct FavoritesModifier: ViewModifier {
    @State var favoritesRepo = RepositoryDI().favorites
    @Binding var favorites: Favorites

    @MainActor
    func activateListener() async {
        for await state in favoritesRepo.state {
//            print("~~~ modifier update from repo state")
            favorites = if let nextFavorites = state {
                nextFavorites
            } else {
                Favorites(routeStopDirection: [:])
            }
        }
    }

    func loadFavorites() {
        Task(priority: .high) {
//            print("~~~ modifier activate")
            await activateListener()
        }
        Task {
//            print("~~~ modifier get")
//            try? await favoritesRepo.getFavorites()
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
