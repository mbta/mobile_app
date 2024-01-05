package com.mbta.tid.mbta_app

import cafe.adriel.lyricist.Lyricist
import com.mbta.tid.mbta_app.localization.Translations

class Greeting {
    private val platform: Platform = getPlatform()

    fun greet(): String {
        val lyricist = Lyricist("en", Translations)
        lyricist.strings.simple
        return "Hello, ${platform.name}!"
    }
}
