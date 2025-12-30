package com.mbta.tid.mbta_app.model

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.maplibre.spatialk.geojson.Position

@Serializable
public data class Vehicle(
    override val id: String,
    val bearing: Double?,
    val carriages: List<Carriage>?,
    @SerialName("current_status") val currentStatus: CurrentStatus,
    @SerialName("current_stop_sequence") val currentStopSequence: Int?,
    @SerialName("direction_id") val directionId: Int,
    val latitude: Double,
    val longitude: Double,
    @SerialName("occupancy_status") val occupancyStatus: OccupancyStatus,
    @SerialName("updated_at") val updatedAt: EasternTimeInstant,
    @SerialName("route_id") val routeId: Route.Id?,
    @SerialName("stop_id") val stopId: String?,
    @SerialName("trip_id") val tripId: String?,
    val decoration: Decoration? = null,
) : BackendObject<String> {
    val position: Position = Position(latitude = latitude, longitude = longitude)

    @Serializable
    public enum class CurrentStatus {
        @SerialName("incoming_at") IncomingAt,
        @SerialName("stopped_at") StoppedAt,
        @SerialName("in_transit_to") InTransitTo,
    }

    @Serializable
    public enum class Decoration {
        @SerialName("pride") Pride,
        @SerialName("winter_holiday") WinterHoliday,
        @SerialName("googly_eyes") GooglyEyes,
    }

    @Serializable
    public enum class OccupancyStatus {
        @SerialName("many_seats_available") ManySeatsAvailable,
        @SerialName("few_seats_available") FewSeatsAvailable,
        @SerialName("standing_room_only") StandingRoomOnly,
        @SerialName("crushed_standing_room_only") CrushedStandingRoomOnly,
        @SerialName("full") Full,
        @SerialName("not_accepting_passengers") NotAcceptingPassengers,
        @SerialName("no_data_available") NoDataAvailable;

        public val crowdingLevel: CrowdingLevel?
            get() =
                when (this) {
                    CrushedStandingRoomOnly,
                    Full,
                    NotAcceptingPassengers,
                    StandingRoomOnly -> CrowdingLevel.Crowded
                    FewSeatsAvailable -> CrowdingLevel.SomeCrowding
                    ManySeatsAvailable -> CrowdingLevel.NotCrowded
                    else -> null
                }
    }

    /* This is the user facing 1-3 crowding scale that is determined from the occupancy status, it
     * does not exist as a concept in the API or GTFS, we use it to display crowding icon and text
     */
    @Serializable
    public enum class CrowdingLevel {
        Crowded,
        NotCrowded,
        SomeCrowding,
    }

    @Serializable
    public data class Carriage(
        @SerialName("occupancy_status") val occupancyStatus: OccupancyStatus,
        @SerialName("occupancy_percentage") val occupancyPercentage: Int?,
        val label: String?,
    )

    override fun toString(): String = "Vehicle(id=$id)"
}
