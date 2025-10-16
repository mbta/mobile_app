package com.mbta.tid.mbta_app.model.morePage

import com.mbta.tid.mbta_app.utils.SharedString

public class MoreSection(
    public var id: Category,
    public var label: SharedString? = null,
    public var items: List<MoreItem>,
    public var noteAbove: SharedString? = null,
    public var noteBelow: SharedString? = null,
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
