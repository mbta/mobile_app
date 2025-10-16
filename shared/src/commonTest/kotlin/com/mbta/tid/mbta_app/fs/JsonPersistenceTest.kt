package com.mbta.tid.mbta_app.fs

import com.mbta.tid.mbta_app.utils.MockSystemPaths
import com.mbta.tid.mbta_app.utils.SystemPaths
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import okio.fakefilesystem.FakeFileSystem

class JsonPersistenceTest {
    @Test
    fun `can write text`() = runBlocking {
        val fs = FakeFileSystem()
        val paths = MockSystemPaths()
        val json = JsonPersistence(fs, paths, Dispatchers.IO)
        json.write(SystemPaths.Category.Data, "foo", "bar", "baz")
        assertEquals("baz", fs.read(paths.data / "foo" / "bar.json") { readUtf8() })
    }

    @Test
    fun `can write JSON`() = runBlocking {
        val fs = FakeFileSystem()
        val paths = MockSystemPaths()
        val json = JsonPersistence(fs, paths, Dispatchers.IO)
        json.write(
            SystemPaths.Category.Data,
            group = null,
            "test",
            buildJsonArray {
                add(false)
                add(true)
                add(false)
            },
        )
        assertEquals("[false,true,false]", fs.read(paths.data / "test.json") { readUtf8() })
    }

    @Test
    fun `can read JSON`() = runBlocking {
        val fs = FakeFileSystem()
        val paths = MockSystemPaths()
        fs.createDirectories(paths.data)
        fs.write(paths.data / "test.json") { writeUtf8("[false,true,false]") }
        val json = JsonPersistence(fs, paths, Dispatchers.IO)
        assertEquals(
            buildJsonArray {
                add(false)
                add(true)
                add(false)
            },
            json.read<JsonArray>(SystemPaths.Category.Data, group = null, "test"),
        )
    }
}
