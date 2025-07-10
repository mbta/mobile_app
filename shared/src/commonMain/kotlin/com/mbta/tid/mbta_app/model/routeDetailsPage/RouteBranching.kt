package com.mbta.tid.mbta_app.model.routeDetailsPage

import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.RoutePattern.Typicality
import com.mbta.tid.mbta_app.model.RouteType
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.DisambiguatedStopId
import com.mbta.tid.mbta_app.repositories.RouteStopsResult
import kotlin.collections.component1
import kotlin.collections.component2
import kotlin.collections.iterator

private object Workarounds {
    fun rewriteRouteStopsResult(routeStopsResult: RouteStopsResult): RouteStopsResult {
        return when {
            routeStopsResult.routeId == "64" && routeStopsResult.directionId == 0 -> {
                // as of 2025-07-11, the 64 outbound has an A->C->E B->C->E D->E, which would be
                // manageable if the RouteStopsResult said ABCDE, but it says DABCE, causing three
                // parallel segments
                val a = listOf("730", "2755")
                val b = listOf("1060", "72")
                val c = listOf("1123")
                val d = listOf("2231", "12232", "24486", "24487", "24488", "24489", "2442", "2443")
                val e = listOf("2444")
                val bad = d + a + b + c + e
                val good = a + b + c + d + e
                if (routeStopsResult.stopIds.take(bad.size) == d + a + b + c + e) {
                    routeStopsResult.copy(stopIds = good + routeStopsResult.stopIds.drop(good.size))
                } else {
                    routeStopsResult
                }
            }
            else -> routeStopsResult
        }
    }

    fun rewriteStopGraph(
        routeStopsResult: RouteStopsResult,
        stopGraph:
            RouteBranching.MutableGraph<DisambiguatedStopId, Pair<Stop, MutableSet<Typicality>>>,
    ) {
        when (routeStopsResult.routeId to routeStopsResult.directionId) {
            Pair("33", 0) -> {
                // as of 2025-07-11 the 33 outbound has A->C B->C B->D, and that’s not technically
                // three parallel segments, but it is three parallel lines, which is not better for
                // us, so drop the atypical A->C
                val a = DisambiguatedStopId("89414", 0)
                val b = DisambiguatedStopId("8955", 0)
                val c = DisambiguatedStopId("89413", 0)
                val d = DisambiguatedStopId("8970", 0)
                if (stopGraph.containsAllEdges(listOf(a to c, b to c, b to d))) {
                    stopGraph.removeEdge(a to c)
                }
            }
            Pair("33", 1) -> {
                // as of 2025-07-14 the 33 inbound has A->C A->D B->C B->D, and A->D and B->D are
                // atypical but the RouteStopsResult says ADBC so we erase the deviation A->C and
                // the atypical B->D
                val a = DisambiguatedStopId("8335", 0)
                val b = DisambiguatedStopId("6515", 0)
                val c = DisambiguatedStopId("6516", 0)
                val d = DisambiguatedStopId("8337", 0)
                if (stopGraph.containsAllEdges(listOf(a to c, a to d, b to c, b to d))) {
                    stopGraph.removeEdge(a to c)
                    stopGraph.removeEdge(b to d)
                }
            }
            Pair("70", 0) -> {
                // as of 2025-07-14 the 70 outbound has A->B typical B->A deviation, so drop the
                // deviation B->A
                val a = DisambiguatedStopId("88333", 0)
                val b = DisambiguatedStopId("883321", 0)
                if (stopGraph.containsAllEdges(listOf(a to b, b to a))) {
                    stopGraph.removeEdge(b to a)
                }
            }
            Pair("238", 0) -> {
                // as of 2025-07-14 the 238 outbound has A->B->... typical A->C deviation A->D->...
                // atypical so connect C->D
                val a = DisambiguatedStopId("4058", 0)
                val b = DisambiguatedStopId("4252", 0)
                val c = DisambiguatedStopId("4277", 0)
                val d = DisambiguatedStopId("4214", 0)
                if (stopGraph.containsAllEdges(listOf(a to b, a to c, a to d))) {
                    stopGraph.putEdge(c to d)
                }
            }
            Pair("350", 0) -> {
                // as of 2025-07-14 the 350 outbound has A->...->B->C->...->F B->D A->...->E->F, so
                // connect D->C
                val b = DisambiguatedStopId("50940", 0)
                val c = DisambiguatedStopId("49807", 0)
                val d = DisambiguatedStopId("49805", 0)
                val e = DisambiguatedStopId("1691", 0)
                val f = DisambiguatedStopId("1692", 0)
                if (stopGraph.containsAllEdges(listOf(b to c, b to d, e to f))) {
                    stopGraph.putEdge(d to c)
                }
            }
            Pair("Boat-F1", 1) -> {
                // as of 2025-07-14 the Hingham/Hull Ferry inbound has atypical A->B typical A->C
                // A->D and the RouteStopsResult has ACBD so connect C->B
                val a = DisambiguatedStopId("Boat-Hingham", 0)
                val b = DisambiguatedStopId("Boat-George", 0)
                val c = DisambiguatedStopId("Boat-Rowes", 0)
                val d = DisambiguatedStopId("Boat-Hull", 0)
                if (stopGraph.containsAllEdges(listOf(a to b, a to c, a to d))) {
                    stopGraph.putEdge(c to b)
                }
            }
            Pair("Boat-F6", 1) -> {
                // as of 2025-07-14 morning routes visit Logan only after Seaport/Fan and afternoon
                // routes visit Logan only before Central Wharf so Logan never gets disambiguated
                val a = DisambiguatedStopId("Boat-Winthrop", 0)
                val b = DisambiguatedStopId("Boat-Logan", 0)
                val c = DisambiguatedStopId("Boat-Aquarium", 0)
                val d = DisambiguatedStopId("Boat-Fan", 0)
                val e = DisambiguatedStopId("Boat-Logan", 1)
                val f = DisambiguatedStopId("Boat-Winthrop", 1)
                if (stopGraph.vertices.keys == setOf(a, b, c, d, f)) {
                    stopGraph.putVertex(e, checkNotNull(stopGraph.vertices[b]))
                    stopGraph.putEdge(d to e)
                    stopGraph.removeEdge(d to b)
                    stopGraph.putEdge(e to f)
                    stopGraph.removeEdge(b to f)
                    stopGraph.removeEdge(b to a)
                }
            }
            Pair("Boat-F7", 1) -> {
                // as of 2025-07-14 morning routes visit Logan only after Central Wharf and
                // afternoon routes visit Logan only before Seaport/Fan so Logan never gets
                // disambiguated
                val a = DisambiguatedStopId("Boat-Quincy", 0)
                val b = DisambiguatedStopId("Boat-Logan", 0)
                val c = DisambiguatedStopId("Boat-Fan", 0)
                val d = DisambiguatedStopId("Boat-Aquarium", 0)
                val e = DisambiguatedStopId("Boat-Logan", 1)
                val f = DisambiguatedStopId("Boat-Quincy", 1)
                if (stopGraph.vertices.keys == setOf(a, b, c, d, f)) {
                    stopGraph.putVertex(e, checkNotNull(stopGraph.vertices[b]))
                    stopGraph.putEdge(d to e)
                    stopGraph.removeEdge(d to b)
                    stopGraph.putEdge(e to f)
                    stopGraph.removeEdge(b to f)
                    stopGraph.removeEdge(b to a)
                }
            }
        }
    }
}

/**
 * Encapsulates the logic that turns A->B->D->E->F typical B->C->E atypical into
 *
 * ```text
 *    ┍ A
 *   ┌┾ B
 * ® │┝ C
 *   ┥│ D
 *   └┾ E
 *    ┕ F
 * ```
 *
 * (although the actual Unicode graph rendering is in ProjectUtils).
 *
 * A _segment_ in this context is a list of stops visited consecutively that are either all typical
 * or all non-typical. A segment boundary is drawn where
 * - stops change from typical to non-typical or non-typical to typical
 * - there are multiple next stops
 * - the next stop has multiple previous stops
 *
 * It is worth noting that a segment boundary is specifically not drawn if
 * - stops change from one non-typical typicality to another
 * - some trips are truncated versions of other similarly typical trips
 */
data class RouteBranching(
    /**
     * Records the connections between individual stops, preserving how typical stops and
     * connections are.
     */
    val stopGraph: Graph<DisambiguatedStopId, Pair<Stop, Set<Typicality>>>?,
    /** Records the connections between segments. */
    val segmentGraph: Graph<DisambiguatedStopId, BranchSegmentInGraph>?,
    /** The final list of segments to draw in order. */
    val segments: List<BranchSegment>?,
) {
    enum class StickSide {
        Left,
        Right;

        operator fun unaryMinus() =
            when (this) {
                Left -> Right
                Right -> Left
            }
    }

    data class StickSideState(
        val before: Boolean,
        val currentStop: Boolean,
        /**
         * If [currentCross] is true, then this represents either a fork or merge, depending on the
         * [before] and [after] of the side without the [currentStop]. The current stop is always on
         * both branches, i.e. forks are after the current stop and merges are before it.
         */
        val currentCross: Boolean,
        val after: Boolean,
    )

    data class StickState(val left: StickSideState, val right: StickSideState)

    data class BranchStop(val stop: Stop, val stickState: StickState)

    data class BranchSegmentInGraph(val stops: List<Stop>, val typicalities: Set<Typicality>) {
        val isTypical = Typicality.Typical in typicalities

        fun getName(candidates: List<String>): String? =
            candidates.find { candidate ->
                hasStopMatching(Regex("(\\b|^)" + Regex.escape(candidate) + "(\\b|$)"))
            }

        private fun hasStopMatching(regex: Regex) = stops.any { regex.containsMatchIn(it.name) }
    }

    data class BranchSegment(
        val stops: List<BranchStop>,
        val name: String?,
        val isTypical: Boolean,
    )

    data class DisambiguatedStopId(val stopId: String, val index: Int)

    /** A directed graph which may or may not be acyclic. */
    abstract class IGraph<VertexId> {
        abstract val forwardAdjList: Map<VertexId, Set<VertexId>>
        abstract val reverseAdjList: Map<VertexId, Set<VertexId>>

        /** Gets the vertices with outgoing edges but no incoming edges. */
        fun sources(): Set<VertexId> {
            return forwardAdjList.filterValues { it.isNotEmpty() }.keys -
                reverseAdjList.filterValues { it.isNotEmpty() }.keys
        }

        fun containsAllEdges(edges: List<Pair<VertexId, VertexId>>) =
            edges.all { (from, to) -> forwardAdjList[from]?.contains(to) == true }

        fun containsCycle(): Boolean {
            // if any vertex is reachable from itself, that’s a cycle
            val reachable = mutableMapOf<VertexId, MutableSet<VertexId>>()
            for (vertex in forwardAdjList.keys) {
                for (neighbor in forwardAdjList[vertex].orEmpty()) {
                    val vertexReachable = reachable.getOrPut(vertex) { mutableSetOf() }
                    vertexReachable.add(neighbor)
                    vertexReachable.addAll(reachable[neighbor].orEmpty())
                    for (s in reachable.values) {
                        if (vertex in s) {
                            s.addAll(vertexReachable)
                        }
                    }
                }
            }
            return reachable.any { it.key in it.value }
        }
    }

    data class Graph<VertexId, out VertexLabel>(
        val vertices: Map<VertexId, VertexLabel>,
        override val forwardAdjList: Map<VertexId, Set<VertexId>>,
        override val reverseAdjList: Map<VertexId, Set<VertexId>>,
    ) : IGraph<VertexId>() {
        fun neighborsOut(vertexId: VertexId): Set<VertexId> {
            return forwardAdjList[vertexId].orEmpty()
        }

        fun neighborsIn(vertexId: VertexId): Set<VertexId> {
            return reverseAdjList[vertexId].orEmpty()
        }
    }

    internal class MutableGraph<VertexId, VertexLabel> : IGraph<VertexId>() {
        val vertices = mutableMapOf<VertexId, VertexLabel>()
        override val forwardAdjList = mutableMapOf<VertexId, MutableSet<VertexId>>()
        override val reverseAdjList = mutableMapOf<VertexId, MutableSet<VertexId>>()

        fun putVertex(vertex: VertexId, label: VertexLabel) {
            vertices.put(vertex, label)
        }

        inline fun getOrPutVertex(vertex: VertexId, defaultLabel: () -> VertexLabel): VertexLabel {
            return vertices.getOrPut(vertex, defaultLabel)
        }

        fun putEdge(edge: Pair<VertexId, VertexId>) {
            forwardAdjList.getOrPut(edge.first) { mutableSetOf() }.add(edge.second)
            reverseAdjList.getOrPut(edge.second) { mutableSetOf() }.add(edge.first)
        }

        // useful in workarounds but probably not elsewhere
        fun removeEdge(edge: Pair<VertexId, VertexId>) {
            forwardAdjList.getOrPut(edge.first) { mutableSetOf() }.remove(edge.second)
            reverseAdjList.getOrPut(edge.second) { mutableSetOf() }.remove(edge.first)
        }

        fun anyReachableReverse(from: VertexId, to: Set<VertexId>): Boolean {
            if (from in to) return true
            val frontier = mutableListOf(from)
            val seen = mutableSetOf<VertexId>()
            while (!frontier.isEmpty()) {
                val vertex = frontier.removeLast()
                if (vertex in seen) {
                    continue
                }
                seen.add(vertex)
                val neighborsIn = reverseAdjList[vertex].orEmpty()
                for (neighbor in neighborsIn) {
                    if (neighbor in to) return true
                    frontier.add(neighbor)
                }
            }
            return false
        }

        /**
         * If we have A->B->C and also A->C, we want to drop A->C so neither A nor C thinks it has
         * an extra neighbor.
         */
        fun dropSkippingEdges() {
            val edgesToDelete = mutableListOf<Pair<VertexId, VertexId>>()
            for ((fromVertex, toVertices) in forwardAdjList) {
                if (toVertices.size > 1) {
                    for (thisNeighbor in toVertices) {
                        if (anyReachableReverse(thisNeighbor, toVertices - thisNeighbor)) {
                            edgesToDelete.add(fromVertex to thisNeighbor)
                        }
                    }
                }
            }
            for ((from, to) in edgesToDelete) {
                forwardAdjList.getOrPut(from) { mutableSetOf() }.remove(to)
                reverseAdjList.getOrPut(to) { mutableSetOf() }.remove(from)
            }
        }

        fun toGraph(): Graph<VertexId, VertexLabel> {
            return Graph(vertices, forwardAdjList, reverseAdjList)
        }
    }

    companion object {
        @Throws(RouteBranchingException::class)
        fun calculate(
            routeStopsResult: RouteStopsResult,
            globalData: GlobalResponse,
        ): RouteBranching {
            val routeStopsResult = Workarounds.rewriteRouteStopsResult(routeStopsResult)
            val route = checkNotNull(globalData.getRoute(routeStopsResult.routeId))
            val patterns =
                globalData.routePatterns.values.filter {
                    it.routeId == route.id && it.directionId == routeStopsResult.directionId
                }

            val segmentNameCandidates = getNameCandidates(route, routeStopsResult.directionId)
            val stopGraph = buildStopGraph(routeStopsResult, patterns, globalData)
            val segmentGraph =
                stopGraph
                    .takeUnless { it.containsCycle() }
                    ?.let { collapseStopGraphToSegmentGraph(it) }
            val segmentOrder =
                segmentGraph
                    ?.takeUnless { it.containsCycle() }
                    ?.let { getSegmentOrder(segmentGraph, routeStopsResult.stopIds, globalData) }
            val segments =
                if (segmentGraph != null && segmentOrder != null)
                    getSegmentsFromGraph(
                        segmentOrder,
                        segmentGraph,
                        getSegmentName = { getName(segmentNameCandidates) },
                    )
                else null
            return RouteBranching(stopGraph, segmentGraph, segments)
        }

        private fun buildStopGraph(
            routeStopsResult: RouteStopsResult,
            patterns: List<RoutePattern>,
            globalData: GlobalResponse,
        ): Graph<DisambiguatedStopId, Pair<Stop, Set<Typicality>>> {
            val canonStops = routeStopsResult.stopIds.toSet()
            val result = MutableGraph<DisambiguatedStopId, Pair<Stop, MutableSet<Typicality>>>()
            for (pattern in patterns) {
                val trip = globalData.trips[pattern.representativeTripId] ?: continue
                val stopIds =
                    trip.stopIds?.mapNotNull { stopId ->
                        if (canonStops.contains(stopId)) {
                            return@mapNotNull stopId
                        }
                        val parent = globalData.getStop(stopId)?.resolveParent(globalData)?.id
                        parent.takeIf { canonStops.contains(it) }
                    }
                if (stopIds == null) continue
                val stopIdCounts = mutableMapOf<String, Int>()
                val typicality = pattern.typicality ?: Typicality.Typical
                stopIds
                    .map { stopId ->
                        val index = stopIdCounts[stopId] ?: 0
                        stopIdCounts.put(stopId, index + 1)
                        DisambiguatedStopId(stopId, index)
                    }
                    .windowed(size = 2, step = 1) { (firstStop, secondStop) ->
                        result
                            .getOrPutVertex(firstStop) {
                                (globalData.getStop(firstStop.stopId) ?: return@windowed) to
                                    mutableSetOf()
                            }
                            .second
                            .add(typicality)
                        result
                            .getOrPutVertex(secondStop) {
                                (globalData.getStop(secondStop.stopId) ?: return@windowed) to
                                    mutableSetOf()
                            }
                            .second
                            .add(typicality)
                        result.putEdge(firstStop to secondStop)
                    }
            }

            Workarounds.rewriteStopGraph(routeStopsResult, result)

            if (result.containsCycle()) {
                // if you wind up debugging this later, print instead of throwing here
                throw StopGraphContainsCycleException()
            } else {
                result.dropSkippingEdges()
            }

            return result.toGraph()
        }

        private fun collapseStopGraphToSegmentGraph(
            stopGraph: Graph<DisambiguatedStopId, Pair<Stop, Set<Typicality>>>
        ): Graph<DisambiguatedStopId, BranchSegmentInGraph>? {
            // segments are identified by their first stop
            val result = MutableGraph<DisambiguatedStopId, BranchSegmentInGraph>()
            val frontier = stopGraph.sources().toMutableList()
            while (!frontier.isEmpty()) {
                var stopId = frontier.removeFirst()
                if (result.vertices.contains(stopId)) {
                    // this is not a cycle if you have A->B->C A->C and you hit A C B
                    continue
                }
                val newSegmentStopIds = mutableListOf(stopId)
                val newSegmentTypicalities = mutableSetOf<Typicality>()
                newSegmentTypicalities.addAll(stopGraph.vertices[stopId]?.second.orEmpty())

                // the segment ends if
                // - there are no next stops
                // - there are multiple next stops
                // - the single next stop has multiple previous stops
                // - the set of typicalities has started or stopped including Typical
                var neighborsOut = stopGraph.neighborsOut(stopId)
                while (neighborsOut.size == 1) {
                    val neighborId = neighborsOut.single()
                    val nextMultiplePrevious = stopGraph.neighborsIn(neighborId) != setOf(stopId)
                    val neighborPatterns = stopGraph.vertices[neighborId]?.second
                    val alreadyTypical =
                        newSegmentTypicalities
                            .takeUnless { it.isEmpty() }
                            ?.contains(Typicality.Typical)
                    val neighborTypical = neighborPatterns?.contains(Typicality.Typical)
                    val typicalChanged =
                        alreadyTypical != null &&
                            neighborTypical != null &&
                            alreadyTypical != neighborTypical
                    if (nextMultiplePrevious || typicalChanged) {
                        break
                    }
                    stopId = neighborId
                    neighborsOut = stopGraph.neighborsOut(stopId)
                    newSegmentStopIds.add(stopId)
                    newSegmentTypicalities.addAll(neighborPatterns.orEmpty())
                }

                frontier.addAll(neighborsOut)
                val newSegmentId = newSegmentStopIds.first()
                for (outNeighbor in neighborsOut) {
                    result.putEdge(newSegmentId to outNeighbor)
                }
                val newSegmentStopsWithTypicality =
                    newSegmentStopIds.mapNotNull { stopGraph.vertices[it] }
                val newSegmentStops = newSegmentStopsWithTypicality.map { it.first }
                newSegmentTypicalities.addAll(newSegmentStopsWithTypicality.flatMap { it.second })
                val newSegment = BranchSegmentInGraph(newSegmentStops, newSegmentTypicalities)
                result.putVertex(newSegmentId, newSegment)
            }

            if (result.containsCycle()) {
                // if you wind up debugging this later, print instead of throwing here
                throw SegmentGraphContainsCycleException()
            }

            return result.toGraph()
        }

        private fun getSegmentOrder(
            segmentGraph: Graph<DisambiguatedStopId, BranchSegmentInGraph>,
            stopIds: List<String>,
            globalData: GlobalResponse,
        ): List<DisambiguatedStopId>? {
            val segmentsFromGraph =
                stopIds.mapNotNull { stopId ->
                    val stop = globalData.getStop(stopId)
                    DisambiguatedStopId(stopId, 0).takeIf { segmentGraph.vertices.contains(it) }
                        ?: segmentGraph.vertices.keys.firstNotNullOfOrNull { segmentId ->
                            segmentId.takeIf {
                                segmentId.stopId in stop?.childStopIds.orEmpty() &&
                                    segmentId.index == 0
                            }
                        }
                }
            if (segmentGraph.vertices.size != segmentsFromGraph.size) {
                throw SegmentsLostException(
                    segmentGraph.vertices.size - segmentsFromGraph.size,
                    segmentGraph.vertices.size,
                )
            }
            return segmentsFromGraph
        }

        private fun getSegmentsFromGraph(
            segmentOrder: List<DisambiguatedStopId>,
            segmentGraph: Graph<DisambiguatedStopId, BranchSegmentInGraph>,
            getSegmentName: BranchSegmentInGraph.() -> String?,
        ): List<BranchSegment>? {
            // unfortunately, to know which sides a segment connects to on the way out, we need a
            // first pass to assign sides
            val segmentSides = mutableMapOf<DisambiguatedStopId, StickSide>()
            for (vertexId in segmentOrder) {
                val parents = segmentGraph.reverseAdjList[vertexId].orEmpty()
                if (parents.any { it !in segmentSides }) {
                    throw ResponseSequenceOutOfOrderException(
                        parents.filter { it !in segmentSides },
                        vertexId,
                    )
                }
                val parentSides = parents.mapNotNull { segmentSides[it] }.toSet()
                val children = segmentGraph.forwardAdjList[vertexId].orEmpty()
                val siblings =
                    parents
                        .flatMap { segmentGraph.forwardAdjList[it].orEmpty() - vertexId }
                        .toSet() +
                        children
                            .flatMap { segmentGraph.reverseAdjList[it].orEmpty() - vertexId }
                            .toSet()
                val siblingSides = siblings.mapNotNull { segmentSides[it] }.toSet()

                segmentSides[vertexId] =
                    when {
                        siblingSides.size == 1 -> -siblingSides.single()
                        siblingSides.size == 2 -> {
                            throw SiblingOnBothSidesException(vertexId, siblings)
                        }
                        parentSides.size == 1 -> parentSides.single()

                        else -> StickSide.Right
                    }
            }
            val result = mutableListOf<BranchSegment>()
            val segmentsSkippingCurrent = mutableSetOf<DisambiguatedStopId>()
            for ((segmentIndex, vertexId) in segmentOrder.withIndex()) {
                val segment = segmentGraph.vertices[vertexId] ?: continue
                segmentsSkippingCurrent.remove(vertexId)
                if (segmentsSkippingCurrent.size > 1) {
                    throw MultipleSegmentsSkippingCurrentException()
                }
                val hasSegmentSkipping = segmentsSkippingCurrent.isNotEmpty()
                val parents = segmentGraph.reverseAdjList[vertexId].orEmpty()
                val parentSides = parents.mapNotNull { segmentSides[it] }.toSet()
                val children = segmentGraph.forwardAdjList[vertexId].orEmpty()
                val childrenSides = children.mapNotNull { segmentSides[it] }.toSet()

                val segmentSide = segmentSides[vertexId] ?: continue

                val nextSegmentId = segmentOrder.elementAtOrNull(segmentIndex + 1)
                val subsequentSegmentConnections =
                    if (nextSegmentId != null) children - nextSegmentId else emptySet()
                val segmentStops = mutableListOf<BranchStop>()
                for ((stopIndex, stop) in segment.stops.withIndex()) {
                    val isFirstStop = stopIndex == 0
                    val isLastStop = stopIndex == segment.stops.lastIndex
                    val currentCross =
                        (isFirstStop && parentSides.size > 1) ||
                            (isLastStop && childrenSides.size > 1)
                    val segmentSideState =
                        StickSideState(
                            before = !isFirstStop || !parentSides.isEmpty(),
                            currentStop = true,
                            currentCross = currentCross,
                            after = !isLastStop || !childrenSides.isEmpty(),
                        )
                    val oppositeSideState =
                        StickSideState(
                            before =
                                (isFirstStop &&
                                    segmentSide in parentSides &&
                                    -segmentSide in parentSides) || hasSegmentSkipping,
                            currentStop = false,
                            currentCross = currentCross,
                            after =
                                (isLastStop && -segmentSide in childrenSides) || hasSegmentSkipping,
                        )
                    val states =
                        mapOf(segmentSide to segmentSideState, -segmentSide to oppositeSideState)
                    segmentStops.add(
                        BranchStop(
                            stop,
                            StickState(
                                left = states[StickSide.Left] ?: continue,
                                right = states[StickSide.Right] ?: continue,
                            ),
                        )
                    )
                }
                segmentsSkippingCurrent.addAll(subsequentSegmentConnections)
                result.add(BranchSegment(segmentStops, segment.getSegmentName(), segment.isTypical))
            }
            return result
        }

        /**
         * Branch names can come from route names (the “Providence/Stoughton Line” implies that a
         * Providence branch and a Stoughton branch may exist) or from direction destinations
         * (“Ashmont/Braintree” implies that an Ashmont branch and a Braintree branch may exist, and
         * “Foxboro or Providence” implies that a Foxboro branch and a Providence branch may exist).
         *
         * Branch names are only calculated for subway and commuter rail routes, because they are
         * much less useful on ferry (where branches only have one or two stops) and bus (where
         * service patterns may vary widely).
         */
        fun getNameCandidates(route: Route, directionId: Int): List<String> {
            if (route.type in setOf(RouteType.FERRY, RouteType.BUS)) return emptyList()
            fun MatchResult?.names() = this?.groupValues.orEmpty().drop(1)
            fun Regex.matchEntire(string: String?) = string?.let { this.matchEntire(it) }
            val branchingRouteRegex = Regex("""([\w\s]+)/([\w\s]+) Line""")
            val line = branchingRouteRegex.matchEntire(route.longName).names()
            val branchingDirectionRegexes =
                listOf(Regex("""([\w\s]+)/([\w\s]+)"""), Regex("""([\w\s]+) or ([\w\s]+)"""))
            val thisDirection =
                branchingDirectionRegexes
                    .firstNotNullOfOrNull {
                        it.matchEntire(route.directionDestinations[directionId])
                    }
                    .names()
            val oppositeDirection =
                branchingDirectionRegexes
                    .firstNotNullOfOrNull {
                        it.matchEntire(route.directionDestinations[1 - directionId])
                    }
                    .names()
            return line + thisDirection + oppositeDirection
        }
    }

    abstract class RouteBranchingException(message: String) : IllegalStateException(message)

    class StopGraphContainsCycleException : RouteBranchingException("stop graph contains cycle")

    class SegmentGraphContainsCycleException :
        RouteBranchingException("segment graph contains cycle")

    class SegmentsLostException(lost: Int, total: Int) :
        RouteBranchingException("lost $lost of $total segments when getting order")

    class ResponseSequenceOutOfOrderException(
        expectedBefore: List<DisambiguatedStopId>,
        expectedAfter: DisambiguatedStopId,
    ) :
        RouteBranchingException(
            "segments returned from V3 API in non-topological order: expected $expectedBefore before $expectedAfter"
        )

    class SiblingOnBothSidesException(
        segmentId: DisambiguatedStopId,
        siblings: Set<DisambiguatedStopId>,
    ) : RouteBranchingException("segment $segmentId has siblings $siblings on both sides")

    class MultipleSegmentsSkippingCurrentException :
        RouteBranchingException("multiple segments skipping current")
}
