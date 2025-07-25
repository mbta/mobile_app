package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RouteBranchSegment(
    val stops: List<BranchStop>,
    val name: String?,
    @SerialName("typical?") val isTypical: Boolean,
) {
    @Serializable
    enum class Lane {
        @SerialName("left") Left,
        @SerialName("center") Center,
        @SerialName("right") Right,
    }

    @Serializable
    data class BranchStop(
        @SerialName("stop_id") val stopId: String,
        @SerialName("stop_lane") val stopLane: Lane,
        val connections: List<StickConnection>,
    )

    @Serializable
    data class StickConnection(
        @SerialName("from_stop") val fromStop: String,
        @SerialName("to_stop") val toStop: String,
        @SerialName("from_lane") val fromLane: Lane,
        @SerialName("to_lane") val toLane: Lane,
        @SerialName("from_vpos") val fromVPos: VPos,
        @SerialName("to_vpos") val toVPos: VPos,
    ) {
        companion object {
            fun forward(stopBefore: String?, stop: String?, stopAfter: String?, lane: Lane) =
                listOfNotNull(
                    if (stopBefore != null && stop != null)
                        StickConnection(
                            fromStop = stopBefore,
                            toStop = stop,
                            fromLane = lane,
                            toLane = lane,
                            fromVPos = VPos.Top,
                            toVPos = VPos.Center,
                        )
                    else null,
                    if (stop != null && stopAfter != null)
                        StickConnection(
                            fromStop = stop,
                            toStop = stopAfter,
                            fromLane = lane,
                            toLane = lane,
                            fromVPos = VPos.Center,
                            toVPos = VPos.Bottom,
                        )
                    else null,
                    if (stopBefore != null && stop == null && stopAfter != null)
                        StickConnection(
                            fromStop = stopBefore,
                            toStop = stopAfter,
                            fromLane = lane,
                            toLane = lane,
                            fromVPos = VPos.Top,
                            toVPos = VPos.Bottom,
                        )
                    else null,
                )
        }
    }

    enum class VPos {
        @SerialName("top") Top,
        @SerialName("center") Center,
        @SerialName("bottom") Bottom,
    }

    companion object {
        @DefaultArgumentInterop.Enabled
        fun of(
            stopIds: List<String>,
            name: String? = null,
            isTypical: Boolean = true,
            lane: Lane = Lane.Center,
        ) =
            RouteBranchSegment(
                stopIds.mapIndexed { index, stopId ->
                    BranchStop(
                        stopId,
                        lane,
                        StickConnection.Companion.forward(
                            stopIds.elementAtOrNull(index - 1),
                            stopId,
                            stopIds.elementAtOrNull(index + 1),
                            lane,
                        ),
                    )
                },
                name = name,
                isTypical = isTypical,
            )
    }
}
