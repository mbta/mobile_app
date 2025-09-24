package com.mbta.tid.mbta_app.utils

import okio.Path
import okio.Path.Companion.toPath

internal interface SystemPaths {
    val data: Path
    val cache: Path

    enum class Category {
        Data,
        Cache,
    }

    operator fun get(category: Category): Path =
        when (category) {
            Category.Data -> data
            Category.Cache -> cache
        }
}

internal class MockSystemPaths(override val data: Path, override val cache: Path) : SystemPaths {
    constructor(
        data: String = "data",
        cache: String = "cache",
    ) : this(data.toPath(), cache.toPath())
}
