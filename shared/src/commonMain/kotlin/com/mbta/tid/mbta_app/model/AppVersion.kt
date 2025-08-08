package com.mbta.tid.mbta_app.model

public data class AppVersion(val major: UInt, val minor: UInt, val patch: UInt) {
    public operator fun compareTo(otherVersion: AppVersion): Int {
        return major.compareTo(otherVersion.major).takeUnless { it == 0 }
            ?: minor.compareTo(otherVersion.minor).takeUnless { it == 0 }
            ?: patch.compareTo(otherVersion.patch)
    }

    override fun toString(): String = "$major.$minor.$patch"

    public companion object {
        public fun parse(version: String): AppVersion {
            val numbers = version.split('.', limit = 3).map(String::toUInt)
            return AppVersion(numbers[0], numbers[1], numbers[2])
        }
    }
}
