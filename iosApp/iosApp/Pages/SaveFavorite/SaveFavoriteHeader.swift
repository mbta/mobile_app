//
//  SaveFavoriteHeader.swift
//  iosApp
//
//  Created by esimon on 11/18/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct SaveFavoriteHeader: View {
    var isFavorite: Bool
    var onCancel: () -> Void
    var onSave: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .center, spacing: 6) {
                NavTextButton(
                    string: "Cancel",
                    backgroundColor: Color.clear,
                    textColor: Color.key,
                    action: onCancel
                ).padding(.leading, 4)
                Spacer()
                NavTextButton(string: "Save", backgroundColor: Color.key, textColor: Color.fill3, action: onSave)
            }
            Text(isFavorite ? "Edit Favorite" : "Add Favorite").font(Typography.title1Bold).padding(.leading, 16)
        }
        .padding([.bottom, .trailing], 16)
        .foregroundStyle(Color.text)
        .background(Color.fill3)
    }
}
