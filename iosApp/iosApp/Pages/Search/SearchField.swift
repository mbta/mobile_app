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
                TextField("Stops", text: $searchObserver.searchText)
                    .accessibilityLabel("search")
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
                    .accessibilityLabel("clear search text")
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
                    searchObserver.isSearching ? Color.keyInverse.opacity(0.40) : Color.halo,
                    lineWidth: searchObserver.isSearching ? 3 : 2
                ))

            if searchObserver.isSearching {
                Button(action: {
                    searchObserver.isFocused = false
                    searchObserver.clear()
                }, label: { Text("Cancel") }).accessibilityLabel("close search page")
                    .dynamicTypeSize(...DynamicTypeSize.large)
            }
        }
        .padding(.horizontal, 16)
        .padding(.top, 12)
        .onAppear { isFocused = searchObserver.isFocused }
        .onChange(of: isFocused) { searchObserver.isFocused = $0 }
        .onChange(of: searchObserver.isFocused) { isFocused = $0 }
    }
}
