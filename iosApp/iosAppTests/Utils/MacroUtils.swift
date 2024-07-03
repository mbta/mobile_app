//
//  MacroUtils.swift
//  iosAppTests
//
//  Created by Brandon Rodriguez on 7/9/24.
//  Copyright © 2024 MBTA. All rights reserved.
//

@testable import iosApp

func with(macro: (some Macro).Type, block: () -> Void) {
    macro.override = true
    block()
    macro.override = false
}
