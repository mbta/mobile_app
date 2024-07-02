//
//  MapViewModel.swift
//  iosApp
//
//  Created by Horn, Melody on 2024-06-26.
//  Copyright © 2024 MBTA. All rights reserved.
//

import Foundation
import shared

class MapViewModel: ObservableObject {
    @Published var selectedVehicle: Vehicle?
}
