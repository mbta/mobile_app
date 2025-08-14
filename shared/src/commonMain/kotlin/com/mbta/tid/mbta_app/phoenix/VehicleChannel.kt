import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.model.response.VehicleStreamDataResponse

internal class VehicleChannel {
    companion object {
        fun topic(vehicleId: String): String = "vehicle:id:${vehicleId}"

        val newDataEvent = "stream_data"

        @Throws(IllegalArgumentException::class)
        fun parseMessage(payload: String): VehicleStreamDataResponse {
            return json.decodeFromString(payload)
        }
    }
}
