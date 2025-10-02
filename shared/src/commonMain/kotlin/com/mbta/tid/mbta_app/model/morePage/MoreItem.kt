package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.repositories.Settings
import com.mbta.tid.mbta_app.utils.SharedString

public sealed class MoreItem {

    public data class Action internal constructor(val label: SharedString, val action: () -> Unit) :
        MoreItem()

    public data class Link(
        val label: SharedString,
        val url: String,
        val note: SharedString? = null,
    ) : MoreItem()

    public data class NavLink(
        val label: SharedString,
        val callback: () -> Unit,
        val note: String? = null,
    ) : MoreItem()

    public data class Phone(val label: String, val phoneNumber: String) : MoreItem()

    public data class Toggle(val label: SharedString, val settings: Settings) : MoreItem()

    internal fun id() {
        when (this) {
            is Action -> label
            is Link -> url
            is NavLink -> label
            is Phone -> phoneNumber
            is Toggle -> settings.name
        }
    }
}
