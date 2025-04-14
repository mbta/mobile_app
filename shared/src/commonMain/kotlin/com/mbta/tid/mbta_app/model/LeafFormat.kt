package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.model.stopDetailsPage.TileData

/** Represents a [RouteCardData.Leaf] ready to be displayed. */
sealed class LeafFormat {
    abstract fun tileData(): List<TileData>

    abstract fun noPredictionsStatus(): UpcomingFormat.NoTripsFormat?

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

    /** A [RouteCardData.Leaf] which has multiple destinations within its direction. */
    data class Branched(val branches: List<Branch>) : LeafFormat() {
        data class Branch(
            /**
             * The route to display next to [headsign] and [format]. Only set if the
             * [RouteCardData.Leaf] comes from a grouped line, and therefore always worth showing if
             * set.
             */
            val route: Route?,
            val headsign: String,
            val format: UpcomingFormat,
            val id: String = "$headsign-$format"
        )

        override fun tileData(): List<TileData> {
            return branches.mapNotNull { branch ->
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
            for (branch in branches) {
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
        private val branches = mutableListOf<Branched.Branch>()

        fun branch(headsign: String, format: UpcomingFormat) = branch(null, headsign, format)

        fun branch(route: Route?, headsign: String, format: UpcomingFormat) {
            branches.add(Branched.Branch(route, headsign, format))
        }

        internal fun built() = Branched(branches)
    }

    companion object {
        fun branched(block: BranchedBuilder.() -> Unit) = BranchedBuilder().apply(block).built()
    }
}
