package com.mbta.tid.mbta_app.android.util

import com.mbta.tid.mbta_app.utils.EasternTimeInstant
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.datetime.Month
import org.junit.Test

class EasternTimeInstantExtensionTests {

    @Test
    fun testFormattedTime() {
        val instant = EasternTimeInstant(2026, Month.APRIL, 9, 23, 30)
        assertTrue(Regex("11:30\\sPM").matches(instant.formattedTime()))
    }

    @Test
    fun testFormattedShortDateShortTime() {
        val instant = EasternTimeInstant(2026, Month.APRIL, 9, 23, 30)
        assertTrue(Regex("4/9/26, 11:30\\sPM").matches(instant.formattedShortDateShortTime()))
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
        assertEquals("Sunday, Jan 11", middayInstant.formattedServiceDayAndDate())

        val pastMidnightInstant = EasternTimeInstant(2026, Month.JANUARY, 11, 1, 23)
        assertEquals("Saturday, Jan 10", pastMidnightInstant.formattedServiceDayAndDate())

        val serviceThreshold = EasternTimeInstant(2026, Month.JANUARY, 11, 3, 0)
        assertEquals("Sunday, Jan 11", serviceThreshold.formattedServiceDayAndDate())
        assertEquals(
            "Saturday, Jan 10",
            serviceThreshold.formattedServiceDayAndDate(
                EasternTimeInstant.ServiceDateRounding.BACKWARDS
            ),
        )
    }

    @Test
    fun testFormattedShortServiceDayAndDate() {
        val middayInstant = EasternTimeInstant(2025, Month.OCTOBER, 4, 4, 12)
        assertEquals("Sat, Oct 4", middayInstant.formattedShortServiceDayAndDate())

        val pastMidnightInstant = EasternTimeInstant(2025, Month.OCTOBER, 4, 1, 23)
        assertEquals("Fri, Oct 3", pastMidnightInstant.formattedShortServiceDayAndDate())

        val serviceThreshold = EasternTimeInstant(2025, Month.OCTOBER, 4, 3, 0)
        assertEquals("Sat, Oct 4", serviceThreshold.formattedShortServiceDayAndDate())
        assertEquals(
            "Fri, Oct 3",
            serviceThreshold.formattedShortServiceDayAndDate(
                EasternTimeInstant.ServiceDateRounding.BACKWARDS
            ),
        )
    }

    @Test
    fun testFormattedServiceDate() {
        val middayInstant = EasternTimeInstant(2025, Month.SEPTEMBER, 16, 20, 20)
        assertEquals("September 16", middayInstant.formattedServiceDate())

        val pastMidnightInstant = EasternTimeInstant(2025, Month.SEPTEMBER, 16, 2, 20)
        assertEquals("September 15", pastMidnightInstant.formattedServiceDate())

        val serviceThreshold = EasternTimeInstant(2025, Month.SEPTEMBER, 16, 3, 0)
        assertEquals("September 16", serviceThreshold.formattedServiceDate())
        assertEquals(
            "September 15",
            serviceThreshold.formattedServiceDate(EasternTimeInstant.ServiceDateRounding.BACKWARDS),
        )
    }
}
