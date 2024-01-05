package com.mbta.tid.mbta_app.localization

import cafe.adriel.lyricist.LyricistStrings

data class Strings(
    val simple: String,
    val parameter: (locale: String) -> String,
    val plural: (count: Int) -> String,
    val list: List<String>
)

object Locales {
    const val EN = "en"
    const val ES = "es"
}

@LyricistStrings(languageTag = Locales.EN, default = true)
val EnStrings = Strings(
    simple = "Hello KMP!",
    parameter = { locale ->
        "Current locale: $locale"
    },

    plural = { count ->
        val value = when (count) {
            0 -> "no"
            1, 2 -> "a few"
            in 3..10 -> "a bunch of"
            else -> "a lot of"
        }
        "I have $value apples"
    },

    list = listOf("Avocado", "Pineapple", "Plum")
)

@LyricistStrings(languageTag = Locales.ES)
val EsStrings = Strings(
    simple = "¡Hola KMP!",
    parameter = { locale ->
        "Ubicación actual: $locale"
    },
    plural = { count ->

        val value = when (count) {
            0 -> "No tengo manzanas"
            1, 2 -> "algunas"
            in 3..10 -> "un montón de"
            else -> "muchas"
        }
        if (count == 0) value else "Tengo $value manzanas"
    },
    list = listOf("Palta", "Piña", "Ciruela")
)
val F = "f"

val Translations = mapOf(
    Locales.EN to EnStrings,
    Locales.ES to EsStrings
)
