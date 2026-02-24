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
    internal data class Destination(
        val category: SystemPaths.Category,
        val group: String?,
        val name: String,
    )

    private fun dir(category: SystemPaths.Category, group: String?): Path {
        val categoryDir = systemPaths[category]
        return if (group != null) categoryDir / group else categoryDir
    }

    private fun path(destination: Destination): Path {
        val dir = dir(destination.category, destination.group)
        return dir / "${destination.name}.json"
    }

    suspend fun write(category: SystemPaths.Category, group: String?, name: String, data: String) =
        write(category, group, name) { writeUtf8(data) }

    suspend fun write(destination: Destination, data: String) =
        write(destination.category, destination.group, destination.name, data)

    suspend inline fun <reified T> write(
        category: SystemPaths.Category,
        group: String?,
        name: String,
        data: T,
    ) = write(category, group, name) { json.encodeToBufferedSink(data, this) }

    suspend inline fun <reified T> write(destination: Destination, data: T) =
        write(destination.category, destination.group, destination.name, data)

    suspend fun <T> write(
        category: SystemPaths.Category,
        group: String?,
        name: String,
        serializer: SerializationStrategy<T>,
        data: T,
    ) = write(category, group, name) { json.encodeToBufferedSink(serializer, data, this) }

    suspend fun <T> write(destination: Destination, serializer: SerializationStrategy<T>, data: T) =
        write(destination.category, destination.group, destination.name, serializer, data)

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

    suspend fun <T> read(
        category: SystemPaths.Category,
        group: String?,
        name: String,
        deserializer: DeserializationStrategy<T>,
    ): T? = read(Destination(category, group, name), deserializer)

    suspend inline fun <reified T> read(
        category: SystemPaths.Category,
        group: String?,
        name: String,
    ): T? = read(Destination(category, group, name))

    suspend fun <T> read(destination: Destination, deserializer: DeserializationStrategy<T>): T? =
        read(path(destination), deserializer)

    suspend inline fun <reified T> read(destination: Destination): T? =
        read(destination, json.serializersModule.serializer<T>())

    private suspend fun <T> read(path: Path, deserializer: DeserializationStrategy<T>): T? =
        withContext(ioDispatcher) {
            try {
                fileSystem.read(path) { json.decodeFromBufferedSource(deserializer, this) }
            } catch (_: Exception) {
                null
            }
        }

    suspend fun delete(category: SystemPaths.Category, group: String?, name: String) =
        delete(Destination(category, group, name))

    suspend fun delete(destination: Destination) = delete(path(destination))

    private suspend fun delete(path: Path) =
        withContext(ioDispatcher) { fileSystem.delete(path, true) }

    suspend inline fun <reified T> deleteStale(
        category: SystemPaths.Category,
        group: String?,
        keyPrefix: String? = null,
        noinline isStale: (T) -> Boolean,
    ) = deleteStale(category, group, keyPrefix, json.serializersModule.serializer<T>(), isStale)

    suspend fun <T> deleteStale(
        category: SystemPaths.Category,
        group: String?,
        keyPrefix: String? = null,
        deserializer: DeserializationStrategy<T>,
        isStale: (T) -> Boolean,
    ) =
        withContext(ioDispatcher) {
            val dir = dir(category, group)
            val paths = fileSystem.list(dir)
            paths.forEach { path ->
                withContext(ioDispatcher) {
                    if (keyPrefix?.let { !path.name.startsWith(it) } ?: false) return@withContext
                    val data = read(path, deserializer)
                    if (data?.let { isStale(it) } ?: false) {
                        delete(path)
                        println("~~~ STALE DEL $path")
                    }
                }
            }
        }
}
