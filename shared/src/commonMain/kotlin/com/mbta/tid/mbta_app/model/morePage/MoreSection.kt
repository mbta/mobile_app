package com.mbta.tid.mbta_app.model.morePage

public class MoreSection(
    public var id: Category,
    public var items: List<MoreItem>,
    public var noteAbove: String? = null,
    public var noteBelow: String? = null,
) {

    public enum class Category {
        Feedback,
        FeatureFlags,
        Settings,
        Resources,
        Other,
        Support,
    }

    public val hiddenOnProd: Boolean =
        when (this.id) {
            Category.FeatureFlags -> true
            else -> false
        }
}
