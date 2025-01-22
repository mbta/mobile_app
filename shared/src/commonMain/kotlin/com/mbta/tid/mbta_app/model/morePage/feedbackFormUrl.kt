package com.mbta.tid.mbta_app.model.morePage

fun feedbackFormUrl(translation: String) =
    when (translation) {
        "es" -> "https://mbta.com/appfeedback?lang=es-US"
        "ht" -> "https://mbta.com/appfeedback-ht"
        "pt-BR" -> "https://mbta.com/appfeedback?lang=pt-BR"
        "vi" -> "https://mbta.com/appfeedback?lang=vi"
        "zh-Hans-CN" -> "https://mbta.com/appfeedback?lang=zh-Hans"
        "zh-Hant-TW" -> "https://mbta.com/appfeedback?lang=zh-Hant"
        else -> "https://mbta.com/appfeedback"
    }
