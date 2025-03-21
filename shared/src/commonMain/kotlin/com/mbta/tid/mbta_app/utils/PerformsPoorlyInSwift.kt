package com.mbta.tid.mbta_app.utils

import kotlin.experimental.ExperimentalObjCRefinement
import kotlin.native.HidesFromObjC

/**
 * Hides a declaration from Swift because it will perform poorly and must be abstracted around in
 * some way.
 *
 * Reasons things might perform poorly in Swift:
 * - Returning a large [Map] which will be bridged element-by-element to a Swift `Dictionary`
 */
@OptIn(ExperimentalObjCRefinement::class)
@HidesFromObjC
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.BINARY)
annotation class PerformsPoorlyInSwift
