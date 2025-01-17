package com.mbta.tid.mbta_app.model

/**
 * | [AlertSignificance]     | [Major] | [Secondary] | [Accessibility] | [Minor] | [None] |
 * |-------------------------|---------|-------------|-----------------|---------|--------|
 * | Shown on map            | Yes     | No          | No              | No      | No     |
 * | Replaces upcoming trips | Yes     | No          | No              | No      | No     |
 * | Shown in nearby transit | Yes     | Yes         | Yes             | No      | No     |
 * | Shown in stop details   | Yes     | Yes         | Yes             | Yes     | No     |
 * | Shown in trip details   | Yes     | No          | No              | No      | No     |
 */
enum class AlertSignificance : Comparable<AlertSignificance> {
    // defined in ascending order so that Major > Secondary > Minor > None
    None,
    Minor,
    Accessibility,
    Secondary,
    Major
}
