package com.mbta.tid.mbta_app.fs

import com.mbta.tid.mbta_app.json
import com.mbta.tid.mbta_app.utils.SystemPaths
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.json.okio.decodeFromBufferedSource
import kotlinx.serialization.json.okio.encodeToBufferedSink
import kotlinx.serialization.serializer
import okio.BufferedSink
import okio.FileSystem
import okio.Path
import org.koin.core.component.KoinComponent

@OptIn(ExperimentalSerializationApi::class)
internal class JsonPersistence(
    private val fileSystem: FileSystem,
    private val systemPaths: SystemPaths,
    private val ioDispatcher: CoroutineDispatcher,
) : KoinComponent {
    private fun dir(category: SystemPaths.Category, group: String?): Path {
        val categoryDir = systemPaths[category]
        return if (group != null) categoryDir / group else categoryDir
    }

    suspend fun write(category: SystemPaths.Category, group: String?, name: String, data: String) =
        write(category, group, name) { writeUtf8(data) }

    suspend inline fun <reified T> write(
        category: SystemPaths.Category,
        group: String?,
        name: String,
        data: T,
    ) = write(category, group, name) { json.encodeToBufferedSink(data, this) }

    suspend fun <T> write(
        category: SystemPaths.Category,
        group: String?,
        name: String,
        serializer: SerializationStrategy<T>,
        data: T,
    ) = write(category, group, name) { json.encodeToBufferedSink(serializer, data, this) }

    private suspend fun write(
        category: SystemPaths.Category,
        group: String?,
        name: String,
        writerAction: BufferedSink.() -> Unit,
    ) =
        withContext(ioDispatcher) {
            val dir = dir(category, group)
            fileSystem.createDirectories(dir)
            val path = dir / "$name.json"
            fileSystem.write(path, writerAction = writerAction)
        }

    suspend inline fun <reified T> read(
        category: SystemPaths.Category,
        group: String?,
        name: String,
    ): T? = read(category, group, name, json.serializersModule.serializer<T>())

    suspend fun <T> read(
        category: SystemPaths.Category,
        group: String?,
        name: String,
        deserializer: DeserializationStrategy<T>,
    ): T? =
        withContext(ioDispatcher) {
            val dir = dir(category, group)
            val path = dir / "$name.json"
            try {
                fileSystem.read(path) { json.decodeFromBufferedSource(deserializer, this) }
            } catch (_: Exception) {
                null
            }
        }
}
