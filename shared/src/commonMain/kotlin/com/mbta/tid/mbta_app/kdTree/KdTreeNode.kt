package com.mbta.tid.mbta_app.kdTree

import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.turf.measurement.distance
import org.maplibre.spatialk.units.Length

internal data class KdTreeNode(
    val ids: List<String>,
    val position: Position,
    val splitAxis: Axis,
    val lowChild: KdTreeNode?,
    val highChild: KdTreeNode?,
) {
    fun findNodesWithin(
        searchFrom: Position,
        radius: Length,
        selectPredicate: (String, Length) -> Boolean,
        results: MutableList<Pair<String, Length>>,
    ) {
        val distanceHere = distance(searchFrom, position)
        if (distanceHere <= radius) {
            ids.filter { selectPredicate(it, distanceHere) }
                .forEach { results.add(Pair(it, distanceHere)) }
        }
        val (nearChild, farChild) =
            if (searchFrom[splitAxis] <= position[splitAxis]) {
                Pair(lowChild, highChild)
            } else {
                Pair(highChild, lowChild)
            }
        nearChild?.findNodesWithin(searchFrom, radius, selectPredicate, results)
        val considerFar =
            distance(searchFrom, searchFrom.butWith(splitAxis, position[splitAxis])) <= radius
        if (considerFar) {
            farChild?.findNodesWithin(searchFrom, radius, selectPredicate, results)
        }
    }

    companion object {
        fun build(elements: List<Pair<String, Position>>, splitAxis: Axis): KdTreeNode? {
            if (elements.isEmpty()) return null
            // like with quicksort, we want to approximate the median, so use median-of-three
            val firstPosition = elements.first().second
            val middlePosition = elements[elements.size / 2].second
            val lastPosition = elements.last().second
            val splitPosition =
                listOf(firstPosition, middlePosition, lastPosition).sortedBy { it[splitAxis] }[1]
            val (here, notHere) = elements.partition { it.second == splitPosition }
            val (lowChildren, highChildren) =
                notHere.partition { it.second[splitAxis] < splitPosition[splitAxis] }
            val nextSplitAxis = splitAxis.next()
            return KdTreeNode(
                here.map { it.first },
                splitPosition,
                splitAxis,
                build(lowChildren, nextSplitAxis),
                build(highChildren, nextSplitAxis),
            )
        }
    }
}
