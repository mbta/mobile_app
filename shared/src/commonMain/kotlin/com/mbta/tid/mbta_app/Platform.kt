package com.mbta.tid.mbta_app

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform