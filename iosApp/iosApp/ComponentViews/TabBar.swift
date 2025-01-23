//
//  TabBar.swift
//  iosApp
//
//  Created by Brandon Rodriguez on 1/21/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import SwiftUI

struct TabBar: View {
    @Binding var selectedTab: SelectedTab

    var body: some View {
        VStack(spacing: 0) {
            Divider()
            HStack(spacing: 0) {
                ForEach(SelectedTab.allCases, id: \.self) { tab in
                    Button { selectedTab = tab } label: { tabLabel(tab) }
                }
            }
            .frame(height: 55)
            .background(.regularMaterial)
        }
    }

    private func tabLabel(_ tab: SelectedTab) -> some View {
        VStack {
            Image(tab.imageResource)
            Text(tab.text)
                .font(.caption)
        }
        .foregroundStyle(selectedTab == tab ? Color.accentColor : .gray)
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .contentShape(.rect)
    }
}
