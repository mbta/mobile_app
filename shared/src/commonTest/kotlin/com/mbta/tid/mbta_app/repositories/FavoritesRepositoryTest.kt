package com.mbta.tid.mbta_app.repositories

import androidx.datastore.core.DataStore
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.mocks.MockDatastoreStorage
import com.mbta.tid.mbta_app.mocks.mockJsonPersistence
import com.mbta.tid.mbta_app.model.Route
import com.mbta.tid.mbta_app.model.RouteStopDirection
import com.mbta.tid.mbta_app.utils.SystemPaths
import com.mbta.tid.mbta_app.utils.buildFavorites
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalTime
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.putJsonArray
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

class FavoritesRepositoryTest {
    @AfterTest
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `migrates preferences to JSON file`() = runBlocking {
        val favorites = buildFavorites {
            routeStopDirection(Route.Id("route1"), "stop1", 0) {
                notifications {
                    enabled = true
                    window(LocalTime(8, 0), LocalTime(9, 0), setOf(DayOfWeek.FRIDAY))
                }
            }
            routeStopDirection(Route.Id("route2"), "stop2", 1)
        }
        val storage = MockDatastoreStorage()
        storage.preferences =
            preferencesOf(stringPreferencesKey("favorites") to json.encodeToString(favorites))
        val dataStore = DataStoreFactory.create(storage)
        val jsonPersistence = mockJsonPersistence()
        startKoin {
            modules(
                module {
                    single<DataStore<Preferences>> { dataStore }
                    single { jsonPersistence }
                }
            )
        }
        val repo = FavoritesRepository()

        assertEquals(favorites, repo.getFavorites())
        assertEquals(
            favorites,
            jsonPersistence.read(SystemPaths.Category.Data, group = null, "favorites"),
        )
    }

    @Test
    fun `loads pre-notifications favorites from preferences`() = runBlocking {
        val rsd1 = RouteStopDirection(Route.Id("route1"), "stop1", 0)
        val rsd2 = RouteStopDirection(Route.Id("route2"), "stop2", 1)

        val storage = MockDatastoreStorage()
        storage.preferences =
            preferencesOf(
                stringPreferencesKey("favorites") to
                    json.encodeToString(
                        buildJsonObject {
                            putJsonArray("routeStopDirection") {
                                add(json.encodeToJsonElement(rsd1))
                                add(json.encodeToJsonElement(rsd2))
                            }
                        }
                    )
            )

        val dataStore = DataStoreFactory.create(storage)
        val jsonPersistence = mockJsonPersistence()
        startKoin {
            modules(
                module {
                    single<DataStore<Preferences>> { dataStore }
                    single { jsonPersistence }
                }
            )
        }
        val repo = FavoritesRepository()

        assertEquals(
            buildFavorites {
                routeStopDirection(rsd1)
                routeStopDirection(rsd2)
            },
            repo.getFavorites(),
        )
    }

    @Test
    fun `loads from JSON file`() = runBlocking {
        val favorites = buildFavorites {
            routeStopDirection(Route.Id("route1"), "stop1", 0) {
                notifications {
                    enabled = true
                    window(LocalTime(8, 0), LocalTime(9, 0), setOf(DayOfWeek.FRIDAY))
                }
            }
            routeStopDirection(Route.Id("route2"), "stop2", 1)
        }
        val jsonPersistence = mockJsonPersistence()
        jsonPersistence.write(SystemPaths.Category.Data, group = null, "favorites", favorites)
        startKoin { modules(module { single { jsonPersistence } }) }
        val repo = FavoritesRepository()
        assertEquals(favorites, repo.getFavorites())
    }

    @Test
    fun `saves updated favorites`() = runBlocking {
        val jsonPersistence = mockJsonPersistence()
        startKoin {
            modules(
                module {
                    single { DataStoreFactory.create(MockDatastoreStorage()) }
                    single { jsonPersistence }
                }
            )
        }
        val repo = FavoritesRepository()
        val favorites = buildFavorites {
            routeStopDirection(Route.Id("route1"), "stop1", 0) {
                notifications {
                    enabled = true
                    window(LocalTime(8, 0), LocalTime(9, 0), setOf(DayOfWeek.FRIDAY))
                }
            }
            routeStopDirection(Route.Id("route2"), "stop2", 1)
        }
        repo.setFavorites(favorites)
        assertEquals(favorites, repo.getFavorites())
        assertEquals(
            favorites,
            jsonPersistence.read(SystemPaths.Category.Data, group = null, "favorites"),
        )
    }
}
