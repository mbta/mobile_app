package com.mbta.tid.mbta_app

import android.annotation.SuppressLint
import android.content.Context

@SuppressLint("StaticFieldLeak", "DiscouragedApi")
actual object Strings {

    lateinit var context: Context

    actual fun getString(id: String): String {
        val resourceId = context.resources.getIdentifier(id, "string", context.packageName)
        if (resourceId == 0) return id
        return context.getString(resourceId)
    }

    actual fun getString(id: String, vararg formatArgs: Any): String {
        val resourceId = context.resources.getIdentifier(id, "string", context.packageName)
        if (resourceId == 0) return id
        return context.getString(resourceId, *formatArgs)
    }
}
