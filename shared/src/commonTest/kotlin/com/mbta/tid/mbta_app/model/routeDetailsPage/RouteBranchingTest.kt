package com.mbta.tid.mbta_app.model.routeDetailsPage

import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.model.RoutePattern
import com.mbta.tid.mbta_app.model.response.GlobalResponse
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.BranchSegment
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.BranchStop
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.StickSideState
import com.mbta.tid.mbta_app.model.routeDetailsPage.RouteBranching.StickState
import com.mbta.tid.mbta_app.repositories.RouteStopsResult
import com.mbta.tid.mbta_app.utils.TestData
import kotlin.test.Test
import kotlin.test.assertEquals

class RouteBranchingTest {
    @Test
    fun `dropSkippingEdges handles skipped case`() {
        val graph = RouteBranching.MutableGraph<String, Unit>()
        graph.putEdge("A" to "B")
        graph.putEdge("B" to "C")
        graph.putEdge("A" to "C")
        assertEquals(mapOf("A" to setOf("B", "C"), "B" to setOf("C")), graph.forwardAdjList)
        assertEquals(mapOf("C" to setOf("B", "A"), "B" to setOf("A")), graph.reverseAdjList)
        graph.dropSkippingEdges()
        assertEquals(mapOf("A" to setOf("B"), "B" to setOf("C")), graph.forwardAdjList)
        assertEquals(mapOf("C" to setOf("B"), "B" to setOf("A")), graph.reverseAdjList)
    }

    @Test
    fun `parallel segments work`() {
        val objects = ObjectCollectionBuilder()
        val route = objects.route()
        val a = objects.stop()
        val b = objects.stop()
        val c = objects.stop()
        val d = objects.stop()
        objects.routePattern(route) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(a.id, b.id, d.id) }
        }
        objects.routePattern(route) {
            typicality = RoutePattern.Typicality.Typical
            representativeTrip { stopIds = listOf(a.id, c.id, d.id) }
        }
        val branching =
            RouteBranching.calculate(
                RouteStopsResult(route.id, 0, listOf(a.id, b.id, c.id, d.id)),
                GlobalResponse(objects),
            )
        assertEquals(
            listOf(
                    BranchSegment(
                        listOf(
                            BranchStop(
                                a,
                                StickState(
                                    left = crossTo.copy(before = false),
                                    right = crossFrom.copy(before = false),
                                ),
                            )
                        ),
                        name = null,
                        isTypical = true,
                    ),
                    BranchSegment(
                        listOf(BranchStop(b, StickState(left = skip, right = forward))),
                        name = null,
                        isTypical = true,
                    ),
                    BranchSegment(
                        listOf(BranchStop(c, StickState(left = forward, right = skip))),
                        name = null,
                        isTypical = true,
                    ),
                    BranchSegment(
                        listOf(
                            BranchStop(
                                d,
                                StickState(
                                    left = crossTo.copy(after = false),
                                    right = crossFrom.copy(after = false),
                                ),
                            )
                        ),
                        name = null,
                        isTypical = true,
                    ),
                )
                .toSimpleString(),
            branching.segments?.toSimpleString(),
        )
    }

    @Test
    fun `Red Line works`() {
        val alewife = TestData.getStop("place-alfcl")
        val davis = TestData.getStop("place-davis")
        val porter = TestData.getStop("place-portr")
        val harvard = TestData.getStop("place-harsq")
        val central = TestData.getStop("place-cntsq")
        val kendall = TestData.getStop("place-knncl")
        val charles = TestData.getStop("place-chmnl")
        val park = TestData.getStop("place-pktrm")
        val downtown = TestData.getStop("place-dwnxg")
        val south = TestData.getStop("place-sstat")
        val broadway = TestData.getStop("place-brdwy")
        val andrew = TestData.getStop("place-andrw")
        val jfk = TestData.getStop("place-jfk")
        val savin = TestData.getStop("place-shmnl")
        val fields = TestData.getStop("place-fldcr")
        val shawmut = TestData.getStop("place-smmnl")
        val ashmont = TestData.getStop("place-asmnl")
        val north = TestData.getStop("place-nqncy")
        val wollaston = TestData.getStop("place-wlsta")
        val quincyCenter = TestData.getStop("place-qnctr")
        val quincyAdams = TestData.getStop("place-qamnl")
        val braintree = TestData.getStop("place-brntn")
        val branching =
            RouteBranching.calculate(
                RouteStopsResult(
                    "Red",
                    0,
                    listOf(
                        "place-alfcl",
                        "place-davis",
                        "place-portr",
                        "place-harsq",
                        "place-cntsq",
                        "place-knncl",
                        "place-chmnl",
                        "place-pktrm",
                        "place-dwnxg",
                        "place-sstat",
                        "place-brdwy",
                        "place-andrw",
                        "place-jfk",
                        "place-shmnl",
                        "place-fldcr",
                        "place-smmnl",
                        "place-asmnl",
                        "place-nqncy",
                        "place-wlsta",
                        "place-qnctr",
                        "place-qamnl",
                        "place-brntn",
                    ),
                ),
                GlobalResponse(TestData),
            )

        assertEquals(
            listOf(
                BranchSegment(
                    listOf(
                        BranchStop(
                            alewife,
                            StickState(left = empty, right = forward.copy(before = false)),
                        ),
                        BranchStop(davis, StickState(left = empty, right = forward)),
                        BranchStop(porter, StickState(left = empty, right = forward)),
                        BranchStop(harvard, StickState(left = empty, right = forward)),
                        BranchStop(central, StickState(left = empty, right = forward)),
                        BranchStop(kendall, StickState(left = empty, right = forward)),
                        BranchStop(charles, StickState(left = empty, right = forward)),
                        BranchStop(park, StickState(left = empty, right = forward)),
                        BranchStop(downtown, StickState(left = empty, right = forward)),
                        BranchStop(south, StickState(left = empty, right = forward)),
                        BranchStop(broadway, StickState(left = empty, right = forward)),
                        BranchStop(andrew, StickState(left = empty, right = forward)),
                        BranchStop(
                            jfk,
                            StickState(left = crossTo.copy(before = false), right = crossFrom),
                        ),
                    ),
                    name = null,
                    isTypical = true,
                ),
                BranchSegment(
                    listOf(
                        BranchStop(savin, StickState(left = skip, right = forward)),
                        BranchStop(fields, StickState(left = skip, right = forward)),
                        BranchStop(shawmut, StickState(left = skip, right = forward)),
                        BranchStop(
                            ashmont,
                            StickState(left = skip, right = forward.copy(after = false)),
                        ),
                    ),
                    name = "Ashmont",
                    isTypical = true,
                ),
                BranchSegment(
                    listOf(
                        BranchStop(north, StickState(left = forward, right = empty)),
                        BranchStop(wollaston, StickState(left = forward, right = empty)),
                        BranchStop(quincyCenter, StickState(left = forward, right = empty)),
                        BranchStop(quincyAdams, StickState(left = forward, right = empty)),
                        BranchStop(
                            braintree,
                            StickState(left = forward.copy(after = false), right = empty),
                        ),
                    ),
                    name = "Braintree",
                    isTypical = true,
                ),
            ),
            branching.segments,
        )
    }

    @Test
    fun `Providence Stoughton Line works`() {
        val south = TestData.getStop("place-sstat")
        val backBay = TestData.getStop("place-bbsta")
        val ruggles = TestData.getStop("place-rugg")
        val hydePark = TestData.getStop("place-NEC-2203")
        val readville = TestData.getStop("place-DB-0095")
        val route128 = TestData.getStop("place-NEC-2173")
        val cantonJunction = TestData.getStop("place-NEC-2139")
        val cantonCenter = TestData.getStop("place-SB-0156")
        val stoughton = TestData.getStop("place-SB-0189")
        val sharon = TestData.getStop("place-NEC-2108")
        val mansfield = TestData.getStop("place-NEC-2040")
        val attleboro = TestData.getStop("place-NEC-1969")
        val southAttleboro = TestData.getStop("place-NEC-1919")
        val pawtucket = TestData.getStop("place-NEC-1891")
        val providence = TestData.getStop("place-NEC-1851")
        val tfGreenAirport = TestData.getStop("place-NEC-1768")
        val wickfordJunction = TestData.getStop("place-NEC-1659")
        val branching =
            RouteBranching.calculate(
                RouteStopsResult(
                    "CR-Providence",
                    0,
                    listOf(
                        south.id,
                        backBay.id,
                        ruggles.id,
                        hydePark.id,
                        readville.id,
                        route128.id,
                        cantonJunction.id,
                        cantonCenter.id,
                        stoughton.id,
                        sharon.id,
                        mansfield.id,
                        attleboro.id,
                        southAttleboro.id,
                        pawtucket.id,
                        providence.id,
                        tfGreenAirport.id,
                        wickfordJunction.id,
                    ),
                ),
                GlobalResponse(TestData),
            )
        assertEquals(
            listOf(
                BranchSegment(
                    listOf(
                        BranchStop(
                            south,
                            StickState(left = empty, right = forward.copy(before = false)),
                        ),
                        BranchStop(backBay, StickState(left = empty, right = forward)),
                        BranchStop(ruggles, StickState(left = empty, right = forward)),
                    ),
                    name = null,
                    isTypical = true,
                ),
                BranchSegment(
                    listOf(
                        BranchStop(hydePark, StickState(left = empty, right = forward)),
                        BranchStop(readville, StickState(left = empty, right = forward)),
                    ),
                    name = null,
                    isTypical = false,
                ),
                BranchSegment(
                    listOf(
                        BranchStop(route128, StickState(left = empty, right = forward)),
                        BranchStop(
                            cantonJunction,
                            StickState(left = crossTo.copy(before = false), right = crossFrom),
                        ),
                    ),
                    name = null,
                    isTypical = true,
                ),
                BranchSegment(
                    listOf(
                        BranchStop(cantonCenter, StickState(left = skip, right = forward)),
                        BranchStop(
                            stoughton,
                            StickState(left = skip, right = forward.copy(after = false)),
                        ),
                    ),
                    name = "Stoughton",
                    isTypical = true,
                ),
                BranchSegment(
                    listOf(
                        BranchStop(sharon, StickState(left = forward, right = empty)),
                        BranchStop(mansfield, StickState(left = forward, right = empty)),
                        BranchStop(attleboro, StickState(left = forward, right = empty)),
                    ),
                    name = null,
                    isTypical = true,
                ),
                BranchSegment(
                    listOf(BranchStop(southAttleboro, StickState(left = forward, right = empty))),
                    name = null,
                    isTypical = false,
                ),
                BranchSegment(
                    listOf(
                        BranchStop(pawtucket, StickState(left = forward, right = empty)),
                        BranchStop(providence, StickState(left = forward, right = empty)),
                        BranchStop(tfGreenAirport, StickState(left = forward, right = empty)),
                        BranchStop(
                            wickfordJunction,
                            StickState(left = forward.copy(after = false), right = empty),
                        ),
                    ),
                    name = "Providence",
                    isTypical = true,
                ),
            ),
            branching.segments,
        )
    }

    companion object {
        val empty =
            StickSideState(before = false, currentStop = false, currentCross = false, after = false)
        val forward =
            StickSideState(before = true, currentStop = true, currentCross = false, after = true)
        val skip = forward.copy(currentStop = false)
        val crossFrom =
            StickSideState(before = true, currentStop = true, currentCross = true, after = true)
        val crossTo = crossFrom.copy(currentStop = false)

        // if something is failing, try assertEquals(a.toSimpleString(), b.toSimpleString()) for a
        // more legible diff
        @Suppress("unused")
        fun List<BranchSegment>.toSimpleString() = buildString {
            for (segment in this@toSimpleString) {
                appendLine("BranchSegment(name=${segment.name}, isTypical=${segment.isTypical})")
                for (stop in segment.stops) {
                    appendLine("  ${stop.stop.id} ${stop.stickState}")
                }
            }
        }
    }
}
