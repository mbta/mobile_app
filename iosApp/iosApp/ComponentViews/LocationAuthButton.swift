//
//  LocationAuthButton.swift
//  iosApp
//
//  Created by Jack Curtis on 10/28/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import SwiftUI

struct LocationAuthButtonStyle: ButtonStyle {
    func makeBody(configuration: Configuration) -> some View {
        configuration.label
            .padding()
            .background(Color.key)
            .foregroundStyle(.white)
            .clipShape(Capsule())
    }
}

struct LocationAuthLabelStyle: LabelStyle {
    func makeBody(configuration: Configuration) -> some View {
        HStack(alignment: .center, spacing: 8) {
            configuration.title
            configuration.icon
        }
        .fontWeight(.bold)
    }
}

struct LocationAuthButton: View {
    @EnvironmentObject var locationDataManager: LocationDataManager

    @Binding var showingAlert: Bool

    var body: some View {
        switch locationDataManager.authorizationStatus {
        case .notDetermined, .denied, .restricted:
            Button(action: {
                if locationDataManager.locationDeferred {
                    locationDataManager.setLocationDeferred(false)
                    locationDataManager.requestWhenInUseAuthorization()
                } else {
                    showingAlert = true
                }
            }, label: {
                Label(title: {
                    Text("Location Services is off")
                }, icon: {
                    Image(.faChevronRight)
                        .resizable()
                        .scaleEffect(0.6)
                        .aspectRatio(contentMode: .fit)
                        .frame(width: 20, height: 20, alignment: .center)
                        .backgroundStyle(.white)
                        .background(in: .circle)
                        .foregroundColor(Color.key)
                })
                .labelStyle(LocationAuthLabelStyle())
            })
            .buttonStyle(LocationAuthButtonStyle())
            .accessibilityIdentifier("locationServicesButton")
            .alert(
                "MBTA Go works best with Location Services turned on",
                isPresented: $showingAlert,
                actions: {
                    Button("Turn On in Settings") {
                        showingAlert = false
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                    Button("Keep Location Services Off") {
                        showingAlert = false
                    }
                },
                message: {
                    Text(
                        "You’ll see nearby transit options and get better search results when you turn on Location Services for MBTA Go."
                    )
                }
            )
        case .authorizedAlways, .authorizedWhenInUse:
            EmptyView()
        @unknown default:
            Text("Location access state unknown")
        }
    }
}
