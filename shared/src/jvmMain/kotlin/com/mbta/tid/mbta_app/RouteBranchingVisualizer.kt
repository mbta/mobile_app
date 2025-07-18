package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.Stop
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.BranchSegment
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.BranchSegmentInGraph
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.DisambiguatedStopId
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.Graph
import com.mbta.tid.mbta_app.repositories.GlobalRepository
import com.mbta.tid.mbta_app.repositories.RouteStopsRepository
import java.io.IOException
import java.nio.file.Path
import kotlin.collections.iterator
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.extension
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeLines
import kotlin.io.path.writeText
import kotlin.system.exitProcess
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.measureTimedValue
import org.koin.core.context.startKoin

object RouteBranchingVisualizer {
    fun <I, V, I2, V2> Graph<I, V>.map(
        idTransform: (I) -> I2,
        vertexTransform: (I, V) -> V2,
    ): Graph<I2, V2> {
        return Graph(
            vertices =
                vertices.map { idTransform(it.key) to vertexTransform(it.key, it.value) }.toMap(),
            forwardAdjList =
                forwardAdjList
                    .map { idTransform(it.key) to it.value.map(idTransform).toSet() }
                    .toMap(),
            reverseAdjList =
                reverseAdjList
                    .map { idTransform(it.key) to it.value.map(idTransform).toSet() }
                    .toMap(),
        )
    }

    fun Graph<String, String>.toDot(): String = buildString {
        fun String.clean() = replace(Regex("\\W"), "_")
        appendLine("digraph G {")
        for (vertex in vertices) {
            appendLine("${vertex.key.clean()} [label=\"${vertex.value}\"];")
        }
        for (edge in forwardAdjList.flatMap { (from, neighbors) -> neighbors.map { from to it } }) {
            appendLine("${edge.first.clean()} -> ${edge.second.clean()};")
        }
        appendLine("}")
    }

    fun Graph<String, String>.renderDot(path: Path) {
        val dotSource = toDot()
        val dotPath = path.resolveSibling("${path.nameWithoutExtension}.dot")
        dotPath.writeText(dotSource)
        try {
            Runtime.getRuntime()
                .exec(arrayOf("dot", "-T${path.extension}", "-o$path", dotPath.toString()))
        } catch (e: IOException) {
            println("Failed to render .dot with GraphViz: $e")
        }
    }

    fun Graph<DisambiguatedStopId, Pair<Stop, Set<RoutePattern.Typicality>>>.visualize():
        Graph<String, String> =
        map(
            { "s${it.stopId}-i${it.index}" },
            { id, label ->
                buildString {
                    append(label.first.name)
                    append(" (")
                    append(label.first.id)
                    append(")")
                    if (id.index > 0) {
                        append(" (index=")
                        append(id.index)
                        append(")")
                    }
                    append(" [")
                    append(label.second.minBy { it.ordinal })
                    append("]")
                }
            },
        )

    fun Graph<DisambiguatedStopId, BranchSegmentInGraph>.visualize(
        segmentNames: List<String>
    ): Graph<String, String> =
        map(
            { "s${it.stopId}-i${it.index}" },
            { id, label ->
                val names = label.stops.map { it.name }
                val stopNames =
                    if (names.sumOf { it.length } > 50) {
                            listOf(names.first(), "...${names.size - 2} stops...", names.last())
                        } else {
                            names
                        }
                        .toString()
                        .trim('[', ']')
                val name = label.getName(segmentNames)
                if (name != null) {
                    "$name branch ($stopNames)"
                } else {
                    stopNames
                } + " [${label.typicalities.minBy { it.ordinal }}]"
            },
        )

    enum class BoxState {
        None,
        Light,
        Heavy,
    }

    data class BoxDrawing(
        val left: BoxState,
        val right: BoxState,
        val up: BoxState,
        val down: BoxState,
    ) {
        override fun toString(): String {
            return when (Pair(left.ordinal, right.ordinal) to Pair(up.ordinal, down.ordinal)) {
                ((0 to 0) to (0 to 0)) -> " "
                ((0 to 0) to (0 to 1)) -> "╷"
                ((0 to 0) to (1 to 0)) -> "╵"
                ((0 to 0) to (1 to 1)) -> "│"
                ((0 to 1) to (0 to 0)) -> "╶"
                ((0 to 1) to (0 to 1)) -> "┌"
                ((0 to 1) to (1 to 0)) -> "└"
                ((0 to 1) to (1 to 1)) -> "├"
                ((0 to 2) to (0 to 0)) -> "╺"
                ((0 to 2) to (0 to 1)) -> "┍"
                ((0 to 2) to (1 to 0)) -> "┕"
                ((0 to 2) to (1 to 1)) -> "┝"
                ((1 to 0) to (0 to 0)) -> "╴"
                ((1 to 0) to (0 to 1)) -> "┐"
                ((1 to 0) to (1 to 0)) -> "┘"
                ((1 to 0) to (1 to 1)) -> "┤"
                ((1 to 1) to (0 to 0)) -> "─"
                ((1 to 1) to (0 to 1)) -> "┬"
                ((1 to 1) to (1 to 0)) -> "┴"
                ((1 to 1) to (1 to 1)) -> "┼"
                ((1 to 2) to (0 to 0)) -> "╼"
                ((1 to 2) to (0 to 1)) -> "┮"
                ((1 to 2) to (1 to 0)) -> "┶"
                ((1 to 2) to (1 to 1)) -> "┾"
                ((2 to 0) to (0 to 0)) -> "╸"
                ((2 to 0) to (0 to 1)) -> "┑"
                ((2 to 0) to (1 to 0)) -> "┙"
                ((2 to 0) to (1 to 1)) -> "┥"
                ((2 to 1) to (0 to 0)) -> "╾"
                ((2 to 1) to (0 to 1)) -> "┭"
                ((2 to 1) to (1 to 0)) -> "┵"
                ((2 to 1) to (1 to 1)) -> "┽"
                ((2 to 2) to (0 to 0)) -> "━"
                ((2 to 2) to (0 to 1)) -> "┯"
                ((2 to 2) to (1 to 0)) -> "┷"
                ((2 to 2) to (1 to 1)) -> "┿"
                else -> throw IllegalStateException(this.toString())
            }
        }
    }

    fun List<BranchSegment>.visualize(): List<String> = flatMap { segment ->
        val nonTypical = if (segment.isTypical) " " else "®"
        segment.stops.map { stop ->
            val left =
                with(stop.stickState.left) {
                    BoxDrawing(
                        left = if (currentStop) BoxState.Heavy else BoxState.None,
                        right = if (currentCross) BoxState.Light else BoxState.None,
                        up = if (before) BoxState.Light else BoxState.None,
                        down = if (after) BoxState.Light else BoxState.None,
                    )
                }
            val right =
                with(stop.stickState.right) {
                    BoxDrawing(
                        left = if (currentCross) BoxState.Light else BoxState.None,
                        right = if (currentStop) BoxState.Heavy else BoxState.None,
                        up = if (before) BoxState.Light else BoxState.None,
                        down = if (after) BoxState.Light else BoxState.None,
                    )
                }
            "$nonTypical $left$right ${stop.stop.name}"
        }
    }

    suspend fun checkRouteBranching() {
        startKoin { modules(appModule(AppVariant.Staging), platformModule) }

        val globalData = GlobalRepository().getGlobalData().dataOrThrow()
        val routeStopsRepository = RouteStopsRepository()

        var seriousIssue = false
        for (routeId in globalData.routes.keys.sorted()) {
            val route = globalData.routes[routeId] ?: continue
            if (route.isShuttle) continue
            for (direction in listOf(0, 1)) {
                val caption =
                    "${route.label} (${route.id}) ${route.directionNames[direction]} ($direction) to ${route.directionDestinations[direction]}"
                val routeStopsResult =
                    routeStopsRepository.getRouteStops(route.id, direction).dataOrThrow()
                val branching =
                    try {
                        val (branching, duration) =
                            measureTimedValue {
                                RouteBranching.calculate(routeStopsResult, globalData)
                            }
                        if (duration >= 2.milliseconds) {
                            println(caption)
                            println(
                                buildString {
                                    append("  ")
                                    append(duration)
                                    append(" stopV=")
                                    append(branching.stopGraph?.vertices?.size)
                                    append(" stopE=")
                                    append(
                                        branching.stopGraph?.forwardAdjList?.values?.sumOf {
                                            it.size
                                        }
                                    )
                                    append(" segmentV=")
                                    append(branching.segmentGraph?.vertices?.size)
                                    append(" segmentE=")
                                    append(
                                        branching.segmentGraph?.forwardAdjList?.values?.sumOf {
                                            it.size
                                        }
                                    )
                                }
                            )
                            if (duration >= 20.milliseconds) {
                                seriousIssue = true
                            }
                        }
                        branching
                    } catch (e: RouteBranching.RouteBranchingException) {
                        seriousIssue = true
                        println(caption)
                        println(e)
                        continue
                    }
                if (
                    branching.stopGraph?.vertices?.isEmpty() == true ||
                        branching.segmentGraph?.vertices?.size == 1
                ) {
                    continue
                }
                val outputPath = Path("route-branching/${route.id}/$direction")
                outputPath.createDirectories()
                if (branching.stopGraph == null) {
                    continue
                }
                branching.stopGraph.visualize().renderDot(outputPath.resolve("stopGraph.png"))
                if (branching.segmentGraph == null) {
                    continue
                }
                val segmentNames = RouteBranching.getNameCandidates(route, direction)
                branching.segmentGraph
                    .visualize(segmentNames)
                    .renderDot(outputPath.resolve("segmentGraph.png"))
                if (branching.segments == null) {
                    continue
                }
                outputPath
                    .resolve("out.txt")
                    .writeLines(listOf(caption) + branching.segments.visualize())
            }
        }
        if (seriousIssue) {
            exitProcess(1)
        }
    }
}
