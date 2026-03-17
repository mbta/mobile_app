package com.mbta.tid.mbta_app.utils

import kotlin.math.absoluteValue

public class Targeting {
    public enum class Target {
        NotificationsBeta
    }

    public companion object {
        // The threshold is the percentage of users who will get a truthy result for that target
        private val targetThresholds = mapOf(Target.NotificationsBeta to 0.05f)

        public fun get(target: Target, instanceId: String?): Boolean {
            val threshold = targetThresholds[target] ?: return false
            if (threshold == 0.0f) return false
            if (threshold == 1.0f) return true
            if (instanceId.isNullOrBlank()) return false

            // Add the target name to the ID so users don't get the same threshold for every target
            val hash = "${target.name}+$instanceId".hashCode().absoluteValue
            // Normalize the hashed value to a float between 0 and 1
            val normalizedHash = hash.toFloat() / (Int.MAX_VALUE.toFloat() + 1)
            return normalizedHash < threshold
        }
    }
}
