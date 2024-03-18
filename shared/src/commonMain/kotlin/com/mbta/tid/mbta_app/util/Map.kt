package com.mbta.tid.mbta_app.util

fun <K1, K2, K3, V> Map<Triple<K1, K2, K3>, V>.deepenTripleKey(): Map<K1, Map<K2, Map<K3, V>>> =
    entries
        .groupBy { (key, _) -> key.first }
        .mapValues { (_, byFirst) ->
            byFirst
                .groupBy { (key, _) -> key.second }
                .mapValues { (_, bySecond) ->
                    bySecond.associateBy({ (key, _) -> key.third }, { (_, value) -> value })
                }
        }
