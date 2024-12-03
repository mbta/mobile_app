package com.mbta.tid.mbta_app.model.morePage

class MoreSection(var id: Category, var items: List<MoreItem>) {

    enum class Category {
        FeatureFlags,
        Settings
    }

    val requiresStaging: Boolean =
        when (this.id) {
            Category.FeatureFlags -> true
            else -> false
        }
}
