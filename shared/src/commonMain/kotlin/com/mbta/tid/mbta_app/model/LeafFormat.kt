package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/** Represents a [RouteCardData.Leaf] ready to be displayed. */
sealed class LeafFormat {
    abstract fun tileData(): List<TileData>

    abstract fun noPredictionsStatus(): UpcomingFormat.NoTripsFormat?

    val isAllServiceDisrupted: Boolean
        get() {
            return when (this) {
                is Single -> this.format is UpcomingFormat.Disruption
                is Branched -> false
            }
        }

    /** A [RouteCardData.Leaf] which only has one destination within its direction. */
    data class Single(
        /** The headsign to show next to [format]. Overrides [Direction.destination] if set. */
        val headsign: String?,
        val format: UpcomingFormat
    ) : LeafFormat() {
        override fun tileData(): List<TileData> {
            return if (format is UpcomingFormat.Some) {
                format.trips.map { trip ->
                    TileData(
                        route = null,
                        headsign = null,
                        UpcomingFormat.Some(trip, format.secondaryAlert),
                        trip.trip
                    )
                }
            } else {
                emptyList()
            }
        }

        override fun noPredictionsStatus() = (format as? UpcomingFormat.NoTrips)?.noTripsFormat
    }

    /**
     * A [RouteCardData.Leaf] which has multiple destinations within its direction.
     *
     * @param branchRows The list of rows to display when the route is branched in this direction.
     *   These rows are not distinct by headsign, ex: "Ashmont" may be the headsign of multiple
     *   [branchRows] when it has upcoming service. When a branch has
     *   [UpcomingFormat.NoTripsFormat.PredictionsUnavailable] or [UpcomingFormat.Disruption], it
     *   should only appear once in [branchRows].
     * @param secondaryAlert The [UpcomingFormat.SecondaryAlert] affecting this stop to to display
     *   once for the entire direction. This may be a secondary alert at this stop, or a major alert
     *   affecting a stop downstream of this one on any of the route patterns it serves.
     */
    data class Branched(
        val branchRows: List<BranchRow>,
        val secondaryAlert: UpcomingFormat.SecondaryAlert? = null
    ) : LeafFormat() {
        data class BranchRow
        @OptIn(ExperimentalUuidApi::class)
        constructor(
            /**
             * The route to display next to [headsign] and [format]. Only set if the
             * [RouteCardData.Leaf] comes from a grouped line, and therefore always worth showing if
             * set.
             */
            val route: Route?,
            val headsign: String,
            val format: UpcomingFormat,
            val id: String = "$headsign-$format-${Uuid.random()}"
        )

        override fun tileData(): List<TileData> {
            return branchRows.mapNotNull { branch ->
                if (branch.format is UpcomingFormat.Some) {
                    val trip = branch.format.trips.singleOrNull() ?: return@mapNotNull null
                    TileData(
                        branch.route,
                        branch.headsign,
                        UpcomingFormat.Some(trip, branch.format.secondaryAlert),
                        trip.trip
                    )
                } else null
            }
        }

        override fun noPredictionsStatus(): UpcomingFormat.NoTripsFormat? {
            var result: UpcomingFormat.NoTripsFormat? = null
            for (branch in branchRows) {
                when (branch.format) {
                    is UpcomingFormat.Some -> return null
                    is UpcomingFormat.NoTrips -> {
                        if (result == null) {
                            result = branch.format.noTripsFormat
                        }
                    }
                    else -> {}
                }
            }
            return result
        }
    }

    class BranchedBuilder {
        private val branchRows = mutableListOf<Branched.BranchRow>()
        var secondaryAlert: UpcomingFormat.SecondaryAlert? = null

        fun branchRow(headsign: String, format: UpcomingFormat) = branchRow(null, headsign, format)

        fun branchRow(route: Route?, headsign: String, format: UpcomingFormat) {
            branchRows.add(Branched.BranchRow(route, headsign, format))
        }

        internal fun built() = Branched(branchRows, secondaryAlert)
    }

    companion object {
        fun branched(block: BranchedBuilder.() -> Unit) = BranchedBuilder().apply(block).built()
    }
}
