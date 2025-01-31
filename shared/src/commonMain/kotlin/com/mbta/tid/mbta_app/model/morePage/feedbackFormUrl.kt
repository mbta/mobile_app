package com.mbta.tid.mbta_app.model.morePage

fun feedbackFormUrl(translation: String): String {
    val formLink = "https://mbta.com/androidappfeedback"
    return when (translation) {
        "es" -> "${formLink}?lang=es-US"
        "ht" -> formLink
        "pt-BR" -> "${formLink}?lang=pt-BR"
        "vi" -> "${formLink}?lang=vi"
        "zh-Hans-CN" -> "${formLink}?lang=zh-Hans"
        "zh-Hant-TW" -> "${formLink}?lang=zh-Hant"
        else -> formLink
    }
}
