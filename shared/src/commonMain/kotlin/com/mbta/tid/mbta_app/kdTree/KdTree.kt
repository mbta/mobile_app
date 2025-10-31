package com.mbta.tid.mbta_app.kdTree

import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.units.Length

/**
 * A [k-d tree](https://en.wikipedia.org/wiki/K-d_tree) in two dimensions, containing IDs and
 * geographic coordinates.
 *
 * Handles the specific condition where many points coincide - this is frequently true for parent
 * and child stations, for example.
 */
internal class KdTree(elements: List<Pair<String, Position>>) {
    internal val root: KdTreeNode? = KdTreeNode.build(elements, Axis.Longitude)

    /**
     * Finds all elements within [radiusMiles] of [searchFrom] satisfying [selectPredicate], as (id,
     * distance in miles) pairs.
     */
    fun findNodesWithin(
        searchFrom: Position,
        radius: Length,
        selectPredicate: (id: String, distance: Length) -> Boolean = { _, _ -> true },
    ): List<Pair<String, Length>> {
        val results = mutableListOf<Pair<String, Length>>()
        root?.findNodesWithin(searchFrom, radius, selectPredicate, results)
        results.sortBy { it.second }
        return results
    }
}
