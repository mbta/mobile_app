package com.mbta.tid.mbta_app.util

import kotlinx.datetime.Instant

expect fun formatShortTime(instant: Instant): String
