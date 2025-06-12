//
//  TextFieldObserver.swift
//  iosApp
//
//  Created by esimon on 9/26/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Combine
import SwiftUI

class TextFieldObserver: ObservableObject {
    @Published var searchText = ""
    @Published var isFocused: Bool = false
    var isSearching: Bool { isFocused || !searchText.isEmpty }

    private var subscriptions = Set<AnyCancellable>()

    func clear() {
        searchText = ""
    }
}
