package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public data class Facility
internal constructor(
    override val id: String,
    @SerialName("long_name") internal val longName: String? = null,
    @SerialName("short_name") val shortName: String? = null,
    val type: Type = Type.Other,
) : BackendObject<String> {
    @Serializable
    public enum class Type {
        @SerialName("bike_storage") BikeStorage,
        @SerialName("bridge_plate") BridgePlate,
        @SerialName("electric_car_chargers") ElectricCarChargers,
        @SerialName("elevated_subplatform") ElevatedSubplatform,
        @SerialName("elevator") Elevator,
        @SerialName("escalator") Escalator,
        @SerialName("fare_media_assistance_facility") FareMediaAssistanceFacility,
        @SerialName("fare_media_assistant") FareMediaAssistant,
        @SerialName("fare_vending_machine") FareVendingMachine,
        @SerialName("fare_vending_retailer") FareVendingRetailer,
        @SerialName("fully_elevated_platform") FullyElevatedPlatform,
        @SerialName("other") Other,
        @SerialName("parking_area") ParkingArea,
        @SerialName("pick_drop") PickDrop,
        @SerialName("portable_boarding_lift") PortableBoardingLift,
        @SerialName("ramp") Ramp,
        @SerialName("taxi_stand") TaxiStand,
        @SerialName("ticket_window") TicketWindow,
    }
}
