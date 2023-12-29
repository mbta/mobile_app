package com.mbta.tid.mbta_app

expect object Strings {
    fun getString(id: String): String

    fun getString(id: String, vararg formatArgs: Any): String
    //    fun getQuantityString(id: String, quantity: Int): String
    //    fun getQuantityString(id: String, quantity: Int, vararg formatArgs: Array<out Any>)
}
