package com.mbta.tid.mbta_app.android.util

import com.mbta.tid.mbta_app.android.assertMatches
import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertEquals
import kotlinx.datetime.Month
import org.junit.Test

class EasternTimeInstantExtensionTests {

    @Test
    fun testFormattedTime() {
        val instant = EasternTimeInstant(2026, Month.APRIL, 9, 23, 30)
        assertMatches(Regex("11:30\\sPM", RegexOption.IGNORE_CASE), instant.formattedTime())
    }

    @Test
    fun testFormattedShortDateShortTime() {
        val instant = EasternTimeInstant(2026, Month.APRIL, 9, 23, 30)
        assertMatches(
            Regex("(0?4/0?9|0?9/0?4)/26, 11:30\\sPM", RegexOption.IGNORE_CASE),
            instant.formattedShortDateShortTime(),
        )
    }

    @Test
    fun testFormattedServiceDay() {
        val middayInstant = EasternTimeInstant(2026, Month.JANUARY, 11, 11, 27)
        assertEquals("Sunday", middayInstant.formattedServiceDay())

        val pastMidnightInstant = EasternTimeInstant(2026, Month.JANUARY, 11, 1, 23)
        assertEquals("Saturday", pastMidnightInstant.formattedServiceDay())

        val serviceThreshold = EasternTimeInstant(2026, Month.JANUARY, 11, 3, 0)
        assertEquals("Sunday", serviceThreshold.formattedServiceDay())
        assertEquals(
            "Saturday",
            serviceThreshold.formattedServiceDay(EasternTimeInstant.ServiceDateRounding.BACKWARDS),
        )
    }

    @Test
    fun testFormattedServiceDayAndDate() {
        val middayInstant = EasternTimeInstant(2026, Month.JANUARY, 11, 11, 27)
        assertMatches(Regex("Sunday, (Jan 11|11 Jan)"), middayInstant.formattedServiceDayAndDate())

        val pastMidnightInstant = EasternTimeInstant(2026, Month.JANUARY, 11, 1, 23)
        assertMatches(
            Regex("Saturday, (Jan 10|10 Jan)"),
            pastMidnightInstant.formattedServiceDayAndDate(),
        )

        val serviceThreshold = EasternTimeInstant(2026, Month.JANUARY, 11, 3, 0)
        assertMatches(
            Regex("Sunday, (Jan 11|11 Jan)"),
            serviceThreshold.formattedServiceDayAndDate(),
        )
        assertMatches(
            Regex("Saturday, (Jan 10|10 Jan)"),
            serviceThreshold.formattedServiceDayAndDate(
                EasternTimeInstant.ServiceDateRounding.BACKWARDS
            ),
        )
    }

    @Test
    fun testFormattedShortServiceDayAndDate() {
        val middayInstant = EasternTimeInstant(2025, Month.OCTOBER, 4, 4, 12)
        assertMatches(Regex("Sat, (Oct 4|4 Oct)"), middayInstant.formattedShortServiceDayAndDate())

        val pastMidnightInstant = EasternTimeInstant(2025, Month.OCTOBER, 4, 1, 23)
        assertMatches(
            Regex("Fri, (Oct 3|3 Oct)"),
            pastMidnightInstant.formattedShortServiceDayAndDate(),
        )

        val serviceThreshold = EasternTimeInstant(2025, Month.OCTOBER, 4, 3, 0)
        assertMatches(
            Regex("Sat, (Oct 4|4 Oct)"),
            serviceThreshold.formattedShortServiceDayAndDate(),
        )
        assertMatches(
            Regex("Fri, (Oct 3|3 Oct)"),
            serviceThreshold.formattedShortServiceDayAndDate(
                EasternTimeInstant.ServiceDateRounding.BACKWARDS
            ),
        )
    }

    @Test
    fun testFormattedServiceDate() {
        val middayInstant = EasternTimeInstant(2025, Month.SEPTEMBER, 16, 20, 20)
        assertMatches(Regex("September 16|16 September"), middayInstant.formattedServiceDate())

        val pastMidnightInstant = EasternTimeInstant(2025, Month.SEPTEMBER, 16, 2, 20)
        assertMatches(
            Regex("September 15|15 September"),
            pastMidnightInstant.formattedServiceDate(),
        )

        val serviceThreshold = EasternTimeInstant(2025, Month.SEPTEMBER, 16, 3, 0)
        assertMatches(Regex("September 16|16 September"), serviceThreshold.formattedServiceDate())
        assertMatches(
            Regex("September 15|15 September"),
            serviceThreshold.formattedServiceDate(EasternTimeInstant.ServiceDateRounding.BACKWARDS),
        )
    }
}
