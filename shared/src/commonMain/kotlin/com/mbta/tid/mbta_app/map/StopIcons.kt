package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.map.style.Exp
import com.mbta.tid.mbta_app.map.style.ResolvedImage
import com.mbta.tid.mbta_app.map.style.downcastToResolvedImage
import com.mbta.tid.mbta_app.model.MapStopRoute

object StopIcons {
    val stopIconPrefix = "map-stop-"
    val stopZoomClosePrefix = "close-"
    val stopZoomWidePrefix = "wide-"

    val stopContainerPrefix = "${stopIconPrefix}container-"
    val stopTransferSuffix = "-transfer"
    val stopTerminalSuffix = "-terminal"

    // This is a single transparent pixel, used to create a layer just for tap target padding
    val stopDummyIcon = "${stopIconPrefix}dummy-tap-pixel"

    val stopPinIcon = "${stopIconPrefix}pin"

    val basicStopIcons =
        MapStopRoute.entries.flatMap { routeType -> atZooms(stopIconPrefix, routeType.name) }
    val stopContainerIcons =
        listOf("2", "3").flatMap { memberCount -> atZooms(stopContainerPrefix, memberCount) }

    val terminalIcons: List<String> =
        MapStopRoute.entries
            .filter { !it.hasBranchingTerminals }
            .map { routeType ->
                "${stopIconPrefix}${stopZoomWidePrefix}${routeType.name}${stopTerminalSuffix}"
            } +
            MapStopRoute.entries
                .filter { it.hasBranchingTerminals }
                .flatMap { routeType ->
                    routeType.branchingRoutes.map { routeId ->
                        "${stopIconPrefix}${stopZoomWidePrefix}${routeType.name}${stopTerminalSuffix}-${routeId}"
                    }
                } +
            listOf(
                "${stopIconPrefix}${stopZoomWidePrefix}${MapStopRoute.SILVER.name}${stopTerminalSuffix}"
            )

    val branchIcons: List<String> =
        MapStopRoute.entries
            .filter { it.hasBranchingTerminals }
            .flatMap { routeType ->
                routeType.branchingRoutes.map { routeId ->
                    "${stopIconPrefix}${stopZoomClosePrefix}${routeType.name}-${routeId}"
                }
            }

    val specialCaseStopIcons =
        terminalIcons +
            branchIcons +
            atZooms(stopIconPrefix, MapStopRoute.BUS.name + stopTransferSuffix)

    val all: List<String> =
        basicStopIcons +
            specialCaseStopIcons +
            stopContainerIcons +
            listOf(stopDummyIcon, stopPinIcon)

    // If this is a branching route, return a suffix to specify a distinct icon for it
    private val branchingRouteSuffixExp =
        Exp.case(
            MapExp.branchedRouteExp to
                Exp.concat(
                    Exp("-"),
                    Exp.at(
                        Exp(0),
                        Exp.get(
                            MapExp.topRouteExp,
                            Exp.get(Exp(StopFeaturesBuilder.propRouteIdsKey))
                        )
                    )
                ),
            Exp("")
        )

    fun atZooms(pre: String, post: String): List<String> {
        return listOf(stopZoomClosePrefix, stopZoomWidePrefix).map { zoom ->
            "${pre}${zoom}${post}"
        }
    }

    // Combine prefix and suffix strings to get the icon to display for the given zoom and index
    private fun getRouteIconName(zoomPrefix: String, index: Int): Exp<String> {
        return Exp.concat(
            Exp(stopIconPrefix),
            Exp(zoomPrefix),
            MapExp.routeAt(index = (index)),
            Exp.case(
                // If at wide zoom, give terminal stops with no transfers distinct icons
                Exp.all(
                    Exp.get(Exp(StopFeaturesBuilder.propIsTerminalKey)),
                    MapExp.singleRouteTypeExp,
                    Exp.boolean(Exp(zoomPrefix == stopZoomWidePrefix))
                ) to Exp.concat(Exp(stopTerminalSuffix), branchingRouteSuffixExp),
                // If at close zoom, give stops on a branch their own distinct icons
                Exp.boolean(Exp(zoomPrefix == stopZoomClosePrefix)) to branchingRouteSuffixExp,
                fallback = Exp("")
            )
        )
    }

    // If the stop serves a single type of route, display a regular stop icon,
    // if it serves 2 or 3, display a container icon, and the stop icons in it
    // will be displayed by the transfer layers.
    fun getStopIconName(zoomPrefix: String, forBus: Boolean): Exp<ResolvedImage> {
        return MapExp.busSwitchExp(
                forBus = forBus,
                Exp.step(
                    Exp.length(MapExp.routesExp),
                    getRouteIconName(zoomPrefix, 0),
                    Exp(2) to Exp(stopContainerPrefix + zoomPrefix + "2"),
                    Exp(3) to Exp(stopContainerPrefix + zoomPrefix + "3")
                )
            )
            .downcastToResolvedImage()
    }

    fun getStopLayerIcon(forBus: Boolean = false): Exp<ResolvedImage> {
        return Exp.step(
            Exp.zoom(),
            getStopIconName(stopZoomWidePrefix, forBus = forBus),
            Exp(MapDefaults.closeZoomThreshold) to
                getStopIconName(stopZoomClosePrefix, forBus = forBus)
        )
    }

    fun getTransferIconName(zoomPrefix: String, index: Int): Exp<ResolvedImage> {
        return Exp.step(
                Exp.length(MapExp.routesExp),
                Exp(""),
                Exp(2) to
                    Exp.concat(
                        getRouteIconName(zoomPrefix, index),
                        Exp.case(
                            // Regular non-transfer bus icons are different than the other modes,
                            // and don't fit in the transfer stop containers, this adds a suffix
                            // to use a different icon when a bus is included in a transfer stack.
                            Exp.eq(MapExp.routeAt(index), Exp(MapStopRoute.BUS.name)) to
                                Exp(stopTransferSuffix),
                            Exp("")
                        )
                    )
            )
            .downcastToResolvedImage()
    }

    fun getTransferLayerIcon(index: Int): Exp<ResolvedImage> {
        return Exp.step(
            Exp.zoom(),
            getTransferIconName(stopZoomWidePrefix, index),
            Exp(MapDefaults.closeZoomThreshold) to getTransferIconName(stopZoomClosePrefix, index)
        )
    }
}
