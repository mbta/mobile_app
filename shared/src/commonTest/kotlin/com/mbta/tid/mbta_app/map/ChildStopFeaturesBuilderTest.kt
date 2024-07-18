package com.mbta.tid.mbta_app.map

import com.mbta.tid.mbta_app.model.LocationType
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class ChildStopFeaturesBuilderTest {
    @Test
    fun `contains correct data`() {
        val objects = ObjectCollectionBuilder()
        val parent =
            objects.stop {
                id = "1"
                locationType = LocationType.STATION
                name = "Stop"
            }
        val platform =
            objects.stop {
                id = "2"
                locationType = LocationType.STOP
                platformName = "Headsign"
                parentStationId = parent.id
            }
        val entrance =
            objects.stop {
                id = "3"
                locationType = LocationType.ENTRANCE_EXIT
                name = "Stop - Entrance"
                parentStationId = parent.id
            }
        val boardingArea =
            objects.stop {
                id = "4"
                locationType = LocationType.BOARDING_AREA
                platformName = "Other Headsign"
                parentStationId = parent.id
            }
        val node =
            objects.stop {
                id = "5"
                locationType = LocationType.GENERIC_NODE
                parentStationId = parent.id
            }

        val stops =
            mapOf(
                platform.id to platform,
                entrance.id to entrance,
                boardingArea.id to boardingArea,
                node.id to node
            )

        val collection = ChildStopFeaturesBuilder.generateChildStopFeatures(stops)

        if (true) {
            assertEquals(3, collection.features.size)

            assertEquals(platform.id, collection.features[0].id)
            assertEquals(
                buildJsonObject {
                    put(ChildStopFeaturesBuilder.propNameKey, "Headsign")
                    put(ChildStopFeaturesBuilder.propLocationTypeKey, LocationType.STOP.name)
                    put(ChildStopFeaturesBuilder.propSortOrderKey, 0)
                },
                collection.features[0].properties
            )

            assertEquals(entrance.id, collection.features[1].id)
            assertEquals(
                buildJsonObject {
                    put(ChildStopFeaturesBuilder.propNameKey, ("Entrance"))
                    put(
                        ChildStopFeaturesBuilder.propLocationTypeKey,
                        LocationType.ENTRANCE_EXIT.name
                    )
                    put(ChildStopFeaturesBuilder.propSortOrderKey, 1)
                },
                collection.features[1].properties
            )

            assertEquals(boardingArea.id, collection.features[2].id)
            assertEquals(
                buildJsonObject {
                    put(ChildStopFeaturesBuilder.propNameKey, "Other Headsign")
                    put(
                        ChildStopFeaturesBuilder.propLocationTypeKey,
                        LocationType.BOARDING_AREA.name
                    )
                    put(ChildStopFeaturesBuilder.propSortOrderKey, 2)
                },
                collection.features[2].properties
            )
        }
    }
}
