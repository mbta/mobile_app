@file:JsExport
@file:OptIn(ExperimentalJsExport::class)

package com.mbta.tid.mbta_app.wrapper

import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.model.RouteBranchSegment
import com.mbta.tid.mbta_app.platformModule
import com.mbta.tid.mbta_app.shape.Path
import com.mbta.tid.mbta_app.shape.StickDiagramShapes
import com.mbta.tid.mbta_app.viewModel.IRouteDetailsViewModel
import com.mbta.tid.mbta_app.viewModel.viewModelModule
import kotlin.getValue
import kotlin.js.Promise
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.promise
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.mp.KoinPlatform

public class RouteDetails(backendRoot: String) : KoinComponent {
    init {
        val modules = listOf(appModule(backendRoot), viewModelModule(), platformModule())
        if (KoinPlatform.getKoinOrNull() == null) startKoin { modules(modules) }
        else loadKoinModules(modules)
    }

    private val vm: IRouteDetailsViewModel by inject()

    public fun setSelection(routeId: String, directionId: Int) {
        vm.setSelection(routeId, directionId)
    }

    public fun onNewState(callback: (State?) -> Unit): Promise<Unit> =
        CoroutineScope(Dispatchers.Default).promise { vm.models.collect { callback(it) } }

    public data class State(val routeColor: String, val segments: List<Segment>) {
        public data class Segment(
            val stops: List<Stop>,
            val isTypical: Boolean,
            val twistedConnections: List<TwistedConnection>?,
        )

        public data class Stop(
            val name: String,
            val stopLane: Lane,
            val stickConnections: List<StickConnection>,
        )

        public data class TwistedConnection(
            val connection: StickConnection,
            val isTwisted: Boolean,
        )

        public data class StickConnection
        internal constructor(private val inner: RouteBranchSegment.StickConnection) {
            public fun twistedShape(proportionClosed: Float): TwistedShape? {
                val shape = StickDiagramShapes.twisted(inner, rect, proportionClosed) ?: return null
                val shadow = buildSvg {
                    moveTo(shape.shadow.start)
                    lineTo(shape.shadow.end)
                }
                val curves = buildSvg {
                    when (val bottom = shape.curves.bottom) {
                        null -> moveTo(shape.curves.bottomCurveStart)
                        else -> {
                            moveTo(bottom)
                            lineTo(shape.curves.bottomCurveStart)
                        }
                    }
                    quadraticTo(shape.curves.bottomCurveControl, shape.curves.shadowStart)
                    lineTo(shape.curves.shadowStart)
                    moveTo(shape.curves.shadowEnd)
                    quadraticTo(shape.curves.topCurveControl, shape.curves.topCurveStart)
                    when (val top = shape.curves.top) {
                        null -> {}
                        else -> lineTo(top)
                    }
                }
                val ends = buildSvg {
                    when (val bottom = shape.ends.bottom) {
                        null -> {}
                        else -> {
                            moveTo(bottom)
                            lineTo(shape.ends.bottomCurveStart)
                        }
                    }
                    when (val top = shape.ends.top) {
                        null -> {}
                        else -> {
                            moveTo(shape.ends.topCurveStart)
                            lineTo(top)
                        }
                    }
                }
                return TwistedShape(shadow, curves, ends, shape.opensToNothing)
            }

            public fun nonTwistedShape(proportionClosed: Float): String? {
                val shape =
                    StickDiagramShapes.nonTwisted(inner, rect, proportionClosed) ?: return null
                return buildSvg {
                    moveTo(shape.start)
                    cubicTo(shape.startControl, shape.endControl, shape.end)
                }
            }

            public fun stickShape(): String {
                val shape = StickDiagramShapes.connection(inner, rect)
                return buildSvg {
                    moveTo(shape.start)
                    cubicTo(shape.startControl, shape.endControl, shape.end)
                }
            }

            private companion object {
                private val rect = Path.Rect(0f, 40f, 0f, 48f)
            }
        }

        public data class TwistedShape(
            val shadow: String,
            val curves: String,
            val ends: String,
            val opensToNothing: Boolean,
        )

        public enum class Lane {
            Left,
            Center,
            Right,
        }
    }
}
