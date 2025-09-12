package com.mbta.tid.mbta_app.wrapper

import com.mbta.tid.mbta_app.shape.Path

internal class SvgPathBuilder {
    private val builder = StringBuilder()

    private fun append(char: Char) {
        builder.append(char)
    }

    private fun append(point: Path.Point) {
        builder.append(point.x)
        builder.append(',')
        builder.append(point.y)
    }

    fun moveTo(point: Path.Point) {
        append('M')
        append(point)
    }

    fun lineTo(point: Path.Point) {
        append('L')
        append(point)
    }

    fun quadraticTo(control: Path.Point, to: Path.Point) {
        append('Q')
        append(control)
        append(' ')
        append(to)
    }

    fun cubicTo(control1: Path.Point, control2: Path.Point, to: Path.Point) {
        append('C')
        append(control1)
        append(' ')
        append(control2)
        append(' ')
        append(to)
    }

    override fun toString() = builder.toString()
}

internal fun buildSvg(block: SvgPathBuilder.() -> Unit) = SvgPathBuilder().apply(block).toString()
