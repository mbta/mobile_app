package com.mbta.tid.mbta_app.model

import co.touchlab.skie.configuration.annotations.DefaultArgumentInterop
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class RouteBranchSegment(
    internal val stops: List<BranchStop>,
    internal val name: String?,
    @SerialName("typical?") internal val isTypical: Boolean,
) {
    @Serializable
    public enum class Lane {
        @SerialName("left") Left,
        @SerialName("center") Center,
        @SerialName("right") Right,
    }

    @Serializable
    public data class BranchStop(
        @SerialName("stop_id") internal val stopId: String,
        @SerialName("stop_lane") internal val stopLane: Lane,
        internal val connections: List<StickConnection>,
    )

    @Serializable
    public data class StickConnection(
        @SerialName("from_stop") val fromStop: String,
        @SerialName("to_stop") val toStop: String,
        @SerialName("from_lane") internal val fromLane: Lane,
        @SerialName("to_lane") internal val toLane: Lane,
        @SerialName("from_vpos") val fromVPos: VPos,
        @SerialName("to_vpos") val toVPos: VPos,
    ) {
        public companion object {
            public fun forward(
                stopBefore: String?,
                stop: String?,
                stopAfter: String?,
                lane: Lane,
            ): List<StickConnection> =
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

    public enum class VPos {
        @SerialName("top") Top,
        @SerialName("center") Center,
        @SerialName("bottom") Bottom,
    }

    public companion object {
        @DefaultArgumentInterop.Enabled
        public fun of(
            stopIds: List<String>,
            name: String? = null,
            isTypical: Boolean = true,
            lane: Lane = Lane.Center,
        ): RouteBranchSegment =
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

public val List<Pair<RouteBranchSegment.StickConnection, Boolean>>.terminatedTwist: Boolean
    get() = isTerminatedTwist(this)

public fun isTerminatedTwist(
    stickConnections: List<Pair<RouteBranchSegment.StickConnection, Boolean>>
): Boolean =
    stickConnections.any { (connection, twisted) ->
        twisted &&
            (connection.fromVPos != RouteBranchSegment.VPos.Top ||
                connection.toVPos != RouteBranchSegment.VPos.Bottom)
    }
