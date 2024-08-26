package com.mbta.tid.mbta_app.kdTree

import io.github.dellisd.spatialk.geojson.BoundingBox
import io.github.dellisd.spatialk.geojson.Feature
import io.github.dellisd.spatialk.geojson.Position
import io.github.dellisd.spatialk.geojson.dsl.feature
import io.github.dellisd.spatialk.geojson.dsl.lineString
import io.github.dellisd.spatialk.geojson.dsl.point
import io.github.dellisd.spatialk.turf.ExperimentalTurfApi
import io.github.dellisd.spatialk.turf.Units
import io.github.dellisd.spatialk.turf.distance
import io.github.dellisd.spatialk.turf.toPolygon
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive

internal data class KdTreeNode(
    val ids: List<String>,
    val position: Position,
    val splitAxis: Axis,
    val lowChild: KdTreeNode?,
    val highChild: KdTreeNode?
) {
    @OptIn(ExperimentalTurfApi::class)
    fun findNodesWithin(
        searchFrom: Position,
        radiusMiles: Double,
        selectPredicate: (String, Double) -> Boolean,
        results: MutableList<Pair<String, Double>>
    ) {
        val distanceHere = distance(searchFrom, position, Units.Miles)
        if (distanceHere <= radiusMiles) {
            ids.filter { selectPredicate(it, distanceHere) }
                .forEach { results.add(Pair(it, distanceHere)) }
        }
        val (nearChild, farChild) =
            if (searchFrom[splitAxis] <= position[splitAxis]) {
                Pair(lowChild, highChild)
            } else {
                Pair(highChild, lowChild)
            }
        nearChild?.findNodesWithin(searchFrom, radiusMiles, selectPredicate, results)
        val considerFar =
            distance(searchFrom, searchFrom.butWith(splitAxis, position[splitAxis]), Units.Miles) <=
                radiusMiles
        if (considerFar) {
            farChild?.findNodesWithin(searchFrom, radiusMiles, selectPredicate, results)
        }
    }

    @OptIn(ExperimentalTurfApi::class)
    fun asFeatures(boundingBox: BoundingBox): List<Feature> {
        val id = ids.first()
        val thisFeature =
            listOf(
                feature(point(latitude = position.latitude, longitude = position.longitude), id) {
                    put("ids", JsonArray(ids.map(::JsonPrimitive)))
                },
                feature(
                    lineString {
                        +boundingBox.southwest.butWith(splitAxis, position[splitAxis])
                        +boundingBox.northeast.butWith(splitAxis, position[splitAxis])
                    },
                    id = "$id-line"
                ),
                feature(boundingBox.toPolygon(), id = "$id-box") {
                    put("fill-opacity", 0.1)
                    put("stroke-opacity", 0)
                }
            )
        var lowFeatures: List<Feature>? = null
        if (lowChild != null) {
            val lowBox =
                BoundingBox(
                    boundingBox.southwest,
                    boundingBox.northeast.butWith(splitAxis, position[splitAxis])
                )
            lowFeatures = lowChild.asFeatures(lowBox)
        }
        var highFeatures: List<Feature>? = null
        if (highChild != null) {
            val highBox =
                BoundingBox(
                    boundingBox.southwest.butWith(splitAxis, position[splitAxis]),
                    boundingBox.northeast
                )
            highFeatures = highChild.asFeatures(highBox)
        }
        return thisFeature + lowFeatures.orEmpty() + highFeatures.orEmpty()
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
                build(highChildren, nextSplitAxis)
            )
        }
    }
}
