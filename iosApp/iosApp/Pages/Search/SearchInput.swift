//
//  SearchInput.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 8/1/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct SearchInput: View {
    @ObservedObject var searchObserver: TextFieldObserver
    var hint: String
    var onClear: (() -> Void)? = nil

    // Don't update this FocusState isFocused directly, only toggle through searchObserver.isFocused,
    // otherwise it's possible for the update in searchObserver to get skipped. It's also not
    // possible to test state variable changes, while testing this will stay set to false.
    @FocusState var isFocused: Bool

    @ScaledMetric var searchIconSize: CGFloat = 16

    var body: some View {
        HStack(alignment: .center, spacing: 0) {
            Image(.faMagnifyingGlassSolid)
                .resizable()
                .frame(width: searchIconSize, height: searchIconSize)
                .padding(.all, 4)
                .foregroundStyle(Color.deemphasized)
                .accessibilityHidden(true)
            TextField(
                hint,
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
                    onClear?()
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
        .onAppear { isFocused = searchObserver.isFocused }
        .onChange(of: isFocused) { searchObserver.isFocused = $0 }
        .onChange(of: searchObserver.isFocused) { isFocused = $0 }
    }
}
