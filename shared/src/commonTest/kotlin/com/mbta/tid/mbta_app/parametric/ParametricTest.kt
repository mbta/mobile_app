package com.mbta.tid.mbta_app.parametric

import kotlin.enums.enumEntries
import kotlin.test.assertEquals

/**
 * Sometimes there are values that don't matter in your individual test, and you want to run the
 * test with all those values without having to write for loops yourself.
 */
class ParametricTest(val block: ParametricTest.() -> Unit) {
    var state: State = State.Gathering
    var parameterSpace = mutableListOf<List<Any>>()

    sealed interface State {
        data object Gathering : State

        class Executing(val parameters: List<Any>, var parameterIndex: Int = 0) : State {
            inline fun <reified T> nextParameter(): T {
                val result = parameters[parameterIndex]
                parameterIndex++
                check(result is T) {
                    "Parameter mismatch: expected ${T::class}, got ${result::class}"
                }
                return result
            }
        }
    }

    fun anyBoolean(): Boolean = anyOf(false, true)

    inline fun <reified T : Enum<T>> anyEnumValue(): T = anyEnumValueExcept()

    inline fun <reified T : Enum<T>> anyEnumValueExcept(vararg omitted: T) =
        anyOfList(enumEntries<T>().filterNot(omitted::contains))

    inline fun <reified T : Any> anyOf(vararg options: T) = anyOfList(options.asList())

    inline fun <reified T : Any> anyOfList(options: List<T>): T =
        when (val state = this.state) {
            is State.Gathering -> {
                parameterSpace.add(options)
                options.first()
            }
            is State.Executing -> state.nextParameter()
        }

    internal fun gatherParameters() {
        state = State.Gathering
        parameterSpace.clear()
        block()
    }

    internal fun executeAll() {
        // constructing all these partial lists will be inefficient for more than a handful of
        // parameters
        val parameterSets =
            parameterSpace.fold(listOf(emptyList<Any>())) { acc, newObjects ->
                acc.flatMap { prevObjects ->
                    newObjects.map { newObject ->
                        buildList {
                            addAll(prevObjects)
                            add(newObject)
                        }
                    }
                }
            }
        // the first parameter set should be the one used in gathering, with the first of every
        // parameter
        for (parameters in parameterSets.drop(1)) {
            assertEquals(parameterSpace.size, parameters.size)
            state = State.Executing(parameters)
            block()
        }
    }
}

fun parametricTest(block: ParametricTest.() -> Unit) {
    val test = ParametricTest(block)
    test.gatherParameters()
    test.executeAll()
}
