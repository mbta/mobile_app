//
//  SearchField.swift
//  iosApp
//
//  Created by esimon on 9/25/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import SwiftUI

struct SearchField: View {
    @ObservedObject var searchObserver: TextFieldObserver

    var body: some View {
        HStack(spacing: 14) {
            SearchInput(
                searchObserver: searchObserver,
                hint: NSLocalizedString("Stops", comment: "Placeholder text in the empty search field"),
                onClear: { searchObserver.isFocused = true }
            )
            if searchObserver.isSearching {
                Button(action: {
                    searchObserver.isFocused = false
                    searchObserver.clear()
                }, label: {
                    Text("Cancel", comment: "Cancel searching, clears the search term and closes the search page")
                }).accessibilityLabel(Text("close search page", comment: "VoiceOver label for cancel search button"))
                    .dynamicTypeSize(...DynamicTypeSize.large)
            }
        }
        .padding(.horizontal, 16)
    }
}
