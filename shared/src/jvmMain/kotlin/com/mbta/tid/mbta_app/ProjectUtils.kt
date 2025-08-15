package com.mbta.tid.mbta_app

import com.mbta.tid.mbta_app.dependencyInjection.appModule
import com.mbta.tid.mbta_app.model.ObjectCollectionBuilder
import com.mbta.tid.mbta_app.repositories.GlobalRepository
import com.mbta.tid.mbta_app.utils.SystemPaths
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import kotlin.io.path.Path
import kotlinx.coroutines.runBlocking
import okio.Path
import okio.Path.Companion.toOkioPath
import org.koin.core.context.startKoin
import org.koin.dsl.module

internal val platformModule = module {
    single<SystemPaths> {
        object : SystemPaths {
            override val data: Path
                get() = Path("build/projectUtils/data").toOkioPath()

            override val cache: Path
                get() = Path("build/projectUtils/cache").toOkioPath()
        }
    }
}

/**
 * Utilities that we may need to run from time to time that need access to the real business logic.
 * Run with `gradlew jvmRun`.
 */
@Suppress("unused")
internal object ProjectUtils {
    object TestDataFilters {
        val lines = setOf("line-Green", "line-SLWaterfront")
        val routes =
            setOf(
                "15",
                "67",
                "87",
                "CR-Fitchburg",
                "CR-Haverhill",
                "CR-Lowell",
                "CR-Newburyport",
                "CR-Providence",
                "Orange",
                "Red",
            )
        val allStopsOnRoutes = setOf("Green-B", "Green-C", "Green-D", "Green-E", "Red")
        val stops =
            setOf(
                "1432",
                "14320",
                "2595",
                "26131",
                "place-astao",
                "place-aqucl",
                "place-rugg",
                "place-sull",
            )
    }

    fun fetchTestData(): Unit = runBlocking {
        startKoin { modules(appModule(AppVariant.Prod), platformModule) }

        val globalData = GlobalRepository().getGlobalData().dataOrThrow()

        val lines = globalData.lines.filterKeys { it in TestDataFilters.lines }
        val routes =
            globalData.routes.filterValues {
                (it.lineId in TestDataFilters.lines && !it.isShuttle) ||
                    it.id in TestDataFilters.routes
            }
        val patterns =
            globalData.routePatterns.filterValues {
                routes.containsKey(it.routeId) && it.isTypical()
            }
        val stopIdsForRoutes =
            patterns.values
                .filter { it.routeId in TestDataFilters.allStopsOnRoutes }
                .flatMap { globalData.trips[it.representativeTripId]?.stopIds.orEmpty() }
                .map { globalData.stops[it]?.resolveParent(globalData)?.id }
                .distinct()
        val allStopIds = TestDataFilters.stops + stopIdsForRoutes
        val stops =
            globalData.stops.filterValues {
                (listOf(it.id) + it.childStopIds + listOfNotNull(it.parentStationId)).any(
                    allStopIds::contains
                )
            }
        val tripIds = patterns.map { (_, pattern) -> pattern.representativeTripId }.toSet()
        val trips = globalData.trips.filterKeys { tripIds.contains(it) }

        val testData =
            PropertySpec.builder("TestData", ObjectCollectionBuilder::class)
                .delegate(
                    CodeBlock.builder()
                        .beginControlFlow("lazy")
                        .addStatement("val objects = ObjectCollectionBuilder()")
                        .addStatement("")
                        .apply {
                            for (line in lines.values.sortedBy { it.sortOrder }) {
                                addStatement("objects.put(Line(")
                                addStatement("id = %S,", line.id)
                                addStatement("color = %S,", line.color)
                                addStatement("longName = %S,", line.longName)
                                addStatement("shortName = %S,", line.shortName)
                                addStatement("sortOrder = %L,", line.sortOrder)
                                addStatement("textColor = %S,", line.textColor)
                                addStatement("))")
                            }
                        }
                        .addStatement("")
                        .apply {
                            for (route in routes.values.sorted()) {
                                addStatement("objects.put(Route(")
                                addStatement("id = %S,", route.id)
                                addStatement("type = RouteType.%L,", route.type)
                                addStatement("color = %S,", route.color)
                                addStatement(
                                    "directionNames = listOf(%S, %S),",
                                    route.directionNames[0],
                                    route.directionNames[1],
                                )
                                addStatement(
                                    "directionDestinations = listOf(%S, %S),",
                                    route.directionDestinations[0],
                                    route.directionDestinations[1],
                                )
                                addStatement("isListedRoute = %L,", route.isListedRoute)
                                addStatement("longName = %S,", route.longName)
                                addStatement("shortName = %S,", route.shortName)
                                addStatement("sortOrder = %L,", route.sortOrder)
                                addStatement("textColor = %S,", route.textColor)
                                addStatement("lineId = %S,", route.lineId)
                                addStatement("routePatternIds = %L", route.routePatternIds)
                                addStatement("))")
                            }
                        }
                        .addStatement("")
                        .apply {
                            for (pattern in patterns.values.sorted()) {
                                addStatement("objects.put(RoutePattern(")
                                addStatement("id = %S,", pattern.id)
                                addStatement("directionId = %L,", pattern.directionId)
                                addStatement("name = %S,", pattern.name)
                                addStatement("sortOrder = %L,", pattern.sortOrder)
                                addStatement(
                                    "typicality = RoutePattern.Typicality.%L,",
                                    pattern.typicality,
                                )
                                addStatement(
                                    "representativeTripId = %S,",
                                    pattern.representativeTripId,
                                )
                                addStatement("routeId = %S", pattern.routeId)
                                addStatement("))")
                            }
                        }
                        .addStatement("")
                        .apply {
                            for (stop in stops.values.sortedBy { it.id }) {
                                addStatement("objects.put(Stop(")
                                addStatement("id = %S,", stop.id)
                                addStatement("latitude = %L,", stop.latitude)
                                addStatement("longitude = %L,", stop.longitude)
                                addStatement("name = %S,", stop.name)
                                addStatement("locationType = LocationType.%L,", stop.locationType)
                                addStatement("description = %S,", stop.description)
                                addStatement("platformCode = %S,", stop.platformCode)
                                addStatement("platformName = %S,", stop.platformName)
                                addStatement("vehicleType = %L,", stop.vehicleType.encode())
                                addStatement("childStopIds = %L,", stop.childStopIds.encode())
                                addStatement(
                                    "connectingStopIds = %L,",
                                    stop.connectingStopIds.encode(),
                                )
                                addStatement("parentStationId = %S,", stop.parentStationId)
                                addStatement(
                                    "wheelchairBoarding = WheelchairBoardingStatus.%L,",
                                    stop.wheelchairBoarding,
                                )
                                addStatement("))")
                            }
                        }
                        .addStatement("")
                        .apply {
                            for (trip in trips.values.sortedBy { it.id }) {
                                addStatement("objects.put(Trip(")
                                addStatement("id = %S,", trip.id)
                                addStatement("directionId = %L,", trip.directionId)
                                addStatement("headsign = %S,", trip.headsign)
                                addStatement("routeId = %S,", trip.routeId)
                                addStatement("routePatternId = %S,", trip.routePatternId)
                                addStatement("shapeId = %S,", trip.shapeId)
                                addStatement("stopIds = %L,", trip.stopIds?.encode())
                                addStatement("))")
                            }
                        }
                        .addStatement("")
                        .addStatement("objects")
                        .endControlFlow()
                        .build()
                )
                .addKdoc(
                    "A snapshot of some real system data, to be referenced in tests and previews " +
                        "that need real data. Owned by the jvmRun project utils.\n" +
                        "\nDo not manually edit this file to make changes; if something in here " +
                        "doesn’t compile and it’s blocking the ProjectUtils themselves from " +
                        "running to recreate it, delete all the [ObjectCollectionBuilder.put] " +
                        "calls to unblock ProjectUtils.\n" +
                        "\n@see com.mbta.tid.mbta_app.ProjectUtils"
                )
                .build()

        val testDataFile =
            FileSpec.builder("com.mbta.tid.mbta_app.utils", "TestData")
                .indent(" ".repeat(4))
                .addImport(
                    "com.mbta.tid.mbta_app.model",
                    "Line",
                    "Route",
                    "RoutePattern",
                    "Stop",
                    "Trip",
                    "RouteType",
                    "LocationType",
                    "WheelchairBoardingStatus",
                )
                .addProperty(testData)
                .build()

        testDataFile.writeTo(Path("shared/src/commonMain/kotlin"))
    }
}

private inline fun <reified T : Enum<T>> T?.encode() =
    if (this == null) {
        CodeBlock.of("null")
    } else {
        CodeBlock.of("%L.%L", T::class.simpleName, this)
    }

private fun List<String>.encode() =
    CodeBlock.builder()
        .add("listOf<String>(")
        .apply {
            for ((index, item) in this@encode.withIndex()) {
                add("%S", item)
                if (index != lastIndex) {
                    add(", ")
                }
            }
        }
        .add(")")
        .build()

public fun main() {
    println("MBTA Go Project Utilities")
    println("-------------------------")
    println()
    val methods = ProjectUtils::class.members.filter { it.returnType.toString() == "kotlin.Unit" }
    for ((index, method) in methods.withIndex()) {
        println("${index + 1}. ${method.name}")
    }
    print("> ")
    val selectedIndex =
        if (methods.size > 1) {
            readln().toInt() - 1
        } else {
            println("1")
            0
        }
    methods[selectedIndex].call(ProjectUtils)
}
