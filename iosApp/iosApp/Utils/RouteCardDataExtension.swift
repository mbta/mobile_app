//
//  RouteCardDataExtension.swift
//  iosApp
//
//  Created by esimon on 4/11/25.
//  Copyright © 2025 MBTA. All rights reserved.
//

import Shared

extension RouteCardData: @retroactive Identifiable {}
extension RouteCardData.RouteStopData: @retroactive Identifiable {}
extension RouteCardData.Leaf: @retroactive Identifiable {}
extension LeafFormat.Branched.BranchedBranchRow: @retroactive Identifiable {}
