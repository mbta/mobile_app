package com.mbta.tid.mbta_app.model.morePage

public fun localizedFeedbackFormUrl(
    baseUrl: String,
    translation: String,
    separateHTForm: Boolean = false,
): String {
    return when (translation) {
        "es" -> "${baseUrl}?lang=es-US"
        "fr" -> "${baseUrl}?lang=fr"
        "ht" -> if (separateHTForm) "${baseUrl}-ht" else baseUrl
        "pt-BR" -> "${baseUrl}?lang=pt-BR"
        "vi" -> "${baseUrl}?lang=vi"
        "zh-Hans-CN" -> "${baseUrl}?lang=zh-Hans"
        "zh-Hant-TW" -> "${baseUrl}?lang=zh-Hant"
        else -> baseUrl
    }
}
