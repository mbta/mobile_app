package com.mbta.tid.mbta_app.model.morePage

class MoreSection(var id: Category, var items: List<MoreItem>, var note: String? = null) {

    enum class Category {
        Feedback,
        FeatureFlags,
        Settings,
        Resources,
        Other,
        Support
    }

    val requiresStaging: Boolean =
        when (this.id) {
            Category.FeatureFlags -> true
            else -> false
        }
}
