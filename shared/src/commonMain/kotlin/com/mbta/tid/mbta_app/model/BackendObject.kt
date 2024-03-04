package com.mbta.tid.mbta_app.model

/**
 * An object retrieved from the backend and originating in the V3 API.
 *
 * Useful in the [ObjectCollectionBuilder] for maintaining maps from IDs to objects.
 */
interface BackendObject {
    val id: String
}
