package com.mbta.tid.mbta_app.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
public enum class WheelchairBoardingStatus {
    @SerialName("accessible") ACCESSIBLE,
    @SerialName("inaccessible") INACCESSIBLE,
}
