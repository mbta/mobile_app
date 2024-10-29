//
//  MorePage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-30.
//  Copyright © 2024 MBTA. All rights reserved.
//

import shared
import SwiftUI

struct MorePage: View {
    let inspection = Inspection<Self>()

    var viewModel = SettingsViewModel()

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .bottom) {
                Text("MBTA Go")
                    .font(Typography.title1Bold)
                Spacer()
                let version = Bundle.main.infoDictionary?["CFBundleShortVersionString"] as? String
                if let version {
                    Text(
                        "version \(version)",
                        comment: "Version number label on the More page"
                    )
                    .font(Typography.footnote)
                }
            }
            .padding(.horizontal, 16)
            .padding(.vertical, 10)
            .background(Color.fill3)
            Rectangle()
                .fill(Color.halo)
                .frame(height: 2)
                .frame(maxWidth: .infinity)

            ScrollView {
                VStack(alignment: .leading, spacing: 16) {
                    ForEach(viewModel.sections) { section in
                        MoreSectionView(
                            section: section,
                            toggleSetting: { key in viewModel.toggleSetting(key: key) }
                        )
                    }
                    HStack(alignment: .center, spacing: 16) {
                        Image(.mbtaLogo).resizable().frame(width: 64, height: 64)
                        Text("Made with ♥ in Boston, for Boston").font(Typography.callout)
                    }
                }
                .padding(.horizontal, 16)
                .padding(.vertical, 24)
            }
        }
        .background(Color.fill1)
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}

#Preview {
    MorePage()
        .font(Typography.body)
}
