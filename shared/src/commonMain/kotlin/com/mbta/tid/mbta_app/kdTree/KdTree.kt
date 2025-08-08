package com.mbta.tid.mbta_app.kdTree

import io.github.dellisd.spatialk.geojson.Position

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
        radiusMiles: Double,
        selectPredicate: (id: String, distance: Double) -> Boolean = { _, _ -> true },
    ): List<Pair<String, Double>> {
        val results = mutableListOf<Pair<String, Double>>()
        root?.findNodesWithin(searchFrom, radiusMiles, selectPredicate, results)
        results.sortBy { it.second }
        return results
    }
}
