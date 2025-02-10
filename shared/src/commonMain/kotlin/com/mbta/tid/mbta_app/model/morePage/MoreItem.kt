package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.repositories.Settings

sealed class MoreItem {

    data class Action(val label: String, val action: () -> Unit) : MoreItem()

    data class Link(val label: String, val url: String, val note: String? = null) : MoreItem()

    data class Phone(val label: String, val phoneNumber: String) : MoreItem()

    data class Toggle(val label: String, val settings: Settings, val value: Boolean) : MoreItem()

    fun id() {
        when (this) {
            is Action -> label
            is Link -> url
            is Phone -> phoneNumber
            is Toggle -> settings.name
        }
    }
}
