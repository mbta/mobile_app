package com.mbta.tid.mbta_app.parametric

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFails

class ParametricTestTest {
    @Test
    fun `does not handle zero booleans`() {
        assertFails("parametricTest has no parameters, don’t use it") { parametricTest {} }
    }

    @Test
    fun `handles one boolean`() {
        val calls = mutableListOf<List<Boolean>>()
        parametricTest { calls.add(listOf(anyBoolean())) }
        assertEquals(listOf(listOf(false), listOf(true)), calls)
    }

    @Test
    fun `handles two booleans`() {
        val calls = mutableListOf<List<Boolean>>()
        parametricTest { calls.add(listOf(anyBoolean(), anyBoolean())) }
        assertEquals(
            listOf(
                listOf(false, false),
                listOf(false, true),
                listOf(true, false),
                listOf(true, true),
            ),
            calls,
        )
    }

    @Test
    fun `does not handle 14 booleans`() {
        assertFails("parametricTest has 16384 iterations, that’s too many") {
            parametricTest { (1..14).map { anyBoolean() }.filter { it }.size }
        }
    }

    enum class TestEnum {
        A,
        B,
        C,
        D,
    }

    @Test
    fun `handles enums`() {
        val calls = mutableListOf<TestEnum>()
        parametricTest { calls.add(anyEnumValue()) }
        assertEquals(listOf(TestEnum.A, TestEnum.B, TestEnum.C, TestEnum.D), calls)
    }

    @Test
    fun `handles enums with filter`() {
        val calls = mutableListOf<TestEnum>()
        parametricTest { calls.add(anyEnumValueExcept(TestEnum.B, TestEnum.D)) }
        assertEquals(listOf(TestEnum.A, TestEnum.C), calls)
    }

    @Test
    fun `handles booleans and enums`() {
        val calls = mutableListOf<List<Any>>()
        parametricTest {
            calls.add(
                listOf<Any>(
                    anyBoolean(),
                    anyEnumValue<TestEnum>(),
                    anyEnumValueExcept(TestEnum.A, TestEnum.B),
                )
            )
        }
        assertContentEquals(
            listOf(
                listOf(false, TestEnum.A, TestEnum.C),
                listOf(false, TestEnum.A, TestEnum.D),
                listOf(false, TestEnum.B, TestEnum.C),
                listOf(false, TestEnum.B, TestEnum.D),
                listOf(false, TestEnum.C, TestEnum.C),
                listOf(false, TestEnum.C, TestEnum.D),
                listOf(false, TestEnum.D, TestEnum.C),
                listOf(false, TestEnum.D, TestEnum.D),
                listOf(true, TestEnum.A, TestEnum.C),
                listOf(true, TestEnum.A, TestEnum.D),
                listOf(true, TestEnum.B, TestEnum.C),
                listOf(true, TestEnum.B, TestEnum.D),
                listOf(true, TestEnum.C, TestEnum.C),
                listOf(true, TestEnum.C, TestEnum.D),
                listOf(true, TestEnum.D, TestEnum.C),
                listOf(true, TestEnum.D, TestEnum.D),
            ),
            calls,
        )
    }
}
