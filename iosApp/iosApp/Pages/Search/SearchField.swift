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

    // Don't update this FocusState isFocused directly, only toggle through searchObserver.isFocused,
    // otherwise it's possible for the update in searchObserver to get skipped. It's also not
    // possible to test state variable changes, while testing this will stay set to false.
    @FocusState var isFocused: Bool

    @ScaledMetric var searchIconSize: CGFloat = 16

    var body: some View {
        HStack(spacing: 14) {
            HStack(alignment: .center, spacing: 0) {
                Image(.faMagnifyingGlassSolid)
                    .resizable()
                    .frame(width: searchIconSize, height: searchIconSize)
                    .padding(.all, 4)
                    .foregroundStyle(Color.deemphasized)
                    .accessibilityHidden(true)
                TextField(
                    NSLocalizedString("Stops", comment: "Placeholder text in the empty search field"),
                    text: $searchObserver.searchText
                )
                .accessibilityAddTraits(.isSearchField)
                .focused($isFocused)
                .submitLabel(.done)
                .padding(.horizontal, 2)
                .padding(.vertical, 8)
                .animation(.smooth, value: searchObserver.isSearching)
                if !searchObserver.searchText.isEmpty {
                    ActionButton(kind: .clear) {
                        searchObserver.isFocused = true
                        searchObserver.clear()
                    }
                    .accessibilityLabel(Text(
                        "clear search text",
                        comment: "VoiceOver label for clear search text button"
                    ))
                    .padding(.all, 4)
                }
            }
            .padding(.leading, 8)
            .padding(.trailing, 6)
            .frame(maxWidth: .infinity, minHeight: 44)
            .background(Color.fill3)
            .clipShape(RoundedRectangle(cornerRadius: 8))
            .overlay(RoundedRectangle(cornerRadius: 8)
                .stroke(
                    searchObserver.isSearching ? Color.key : Color.halo,
                    lineWidth: searchObserver.isSearching ? 3 : 2
                ))

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
        .onAppear { isFocused = searchObserver.isFocused }
        .onChange(of: isFocused) { searchObserver.isFocused = $0 }
        .onChange(of: searchObserver.isFocused) { isFocused = $0 }
    }
}
