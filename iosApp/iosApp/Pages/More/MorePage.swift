//
//  MorePage.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-04-30.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Shared
import SwiftUI

struct MorePage: View {
    let inspection = Inspection<Self>()

    var viewModel = SettingsViewModel()

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                HStack(alignment: .bottom) {
                    Text("MBTA Go")
                        .font(Typography.title1Bold)
                        .accessibilityAddTraits(.isHeader)
                        .accessibilityHeading(.h1)
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
                                section: section
                            )
                        }
                        HStack(alignment: .center, spacing: 16) {
                            Image(.mbtaLogo)
                                .resizable()
                                .frame(width: 64, height: 64)
                                .accessibilityLabel(Text("MBTA Logo", comment: "Accessibility text for logo"))
                            Text("Made with ♥ by the T")
                                .font(Typography.callout)
                                .accessibilityLabel(Text("Made with love, by the T"))
                        }
                    }
                    .padding(.horizontal, 16)
                    .padding(.vertical, 24)
                    .navigationDestination(for: MoreNavTarget.self) {
                        switch $0 {
                        case .licenses:
                            List(Dependency.companion.getAllDependencies(), id: \.id) { dependency in
                                NavigationLink(
                                    value: MoreNavTarget.dependency(dependency),
                                    label: { Text(dependency.name) }
                                )
                            }
                            .padding(.top, -11)
                            .background(Color.fill1)
                            .scrollContentBackground(.hidden)
                            .navigationTitle("Software Licenses")
                            .toolbarBackground(Color.fill2, for: .navigationBar)
                            .toolbarBackground(.visible, for: .navigationBar)
                        case let .dependency(dependency):
                            ScrollView {
                                VStack(alignment: .leading, spacing: 24) {
                                    Text(dependency.name)
                                        .font(Typography.title2Bold)
                                        .multilineTextAlignment(.leading)
                                    Text(dependency.licenseText)
                                }
                                .padding(.horizontal, 16)
                                .padding(.vertical, 24)
                            }
                            .background(Color.fill1)
                            .foregroundStyle(Color.text)
                            .toolbarBackground(Color.fill2, for: .navigationBar)
                            .toolbarBackground(.visible, for: .navigationBar)
                        }
                    }
                }
                .background(Color.fill1)
            }
            .background(Color.fill1)
        }
        .onReceive(inspection.notice) { inspection.visit(self, $0) }
    }
}

#Preview {
    MorePage()
        .font(Typography.body)
        .withFixedSettings([:])
}
