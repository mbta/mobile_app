package com.mbta.tid.mbta_app.model.morePage

fun localizedFeedbackFormUrl(baseUrl: String, translation: String): String {
    return when (translation) {
        "es" -> "${baseUrl}?lang=es-US"
        "ht" -> baseUrl
        "pt-BR" -> "${baseUrl}?lang=pt-BR"
        "vi" -> "${baseUrl}?lang=vi"
        "zh-Hans-CN" -> "${baseUrl}?lang=zh-Hans"
        "zh-Hant-TW" -> "${baseUrl}?lang=zh-Hant"
        else -> baseUrl
    }
}
