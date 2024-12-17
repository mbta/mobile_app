package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.repositories.Settings

sealed class MoreItem {

    data class Toggle(val label: String, val settings: Settings, val value: Boolean) : MoreItem()

    data class Link(val label: String, val url: String, val note: String? = null) : MoreItem()

    data class Phone(val label: String, val phoneNumber: String) : MoreItem()

    fun id() {
        when (this) {
            is Toggle -> settings.name
            is Link -> url
            is Phone -> phoneNumber
        }
    }
}
