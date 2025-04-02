package com.mbta.tid.mbta_app.model

/** Represents a [RouteCardData.Leaf] ready to be displayed. */
sealed class LeafFormat {
    /** A [RouteCardData.Leaf] which only has one destination within its direction. */
    data class Single(
        /** The headsign to show next to [format]. Overrides [Direction.destination] if set. */
        val headsign: String?,
        val format: UpcomingFormat
    ) : LeafFormat()

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
            val format: UpcomingFormat
        )
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
