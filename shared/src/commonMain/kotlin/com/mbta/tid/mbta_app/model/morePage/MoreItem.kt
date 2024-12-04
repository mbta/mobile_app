package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.repositories.Settings

sealed class MoreItem {

    data class Toggle(val label: String, val settings: Settings, val value: Boolean) : MoreItem()

    fun id() {
        when (this) {
            is Toggle -> settings.name
        }
    }
}
