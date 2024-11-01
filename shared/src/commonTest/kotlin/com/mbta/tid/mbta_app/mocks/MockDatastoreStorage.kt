package com.mbta.tid.mbta_app.mocks

import androidx.datastore.core.ReadScope
import androidx.datastore.core.Storage
import androidx.datastore.core.StorageConnection
import androidx.datastore.core.WriteScope
import androidx.datastore.core.okio.createSingleProcessCoordinator
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import okio.fakefilesystem.FakeFileSystem

class MockDatastoreStorage : Storage<Preferences> {
    var preferences: Preferences = emptyPreferences()

    override fun createConnection() = MockStorageConnection()

    inner class MockStorageConnection : StorageConnection<Preferences> {
        override val coordinator = createSingleProcessCoordinator(FakeFileSystem().workingDirectory)

        override fun close() {}

        override suspend fun writeScope(block: suspend WriteScope<Preferences>.() -> Unit) {
            MockWriteScope().block()
        }

        override suspend fun <R> readScope(
            block: suspend ReadScope<Preferences>.(locked: Boolean) -> R
        ): R = MockReadScope().block(true)
    }

    inner class MockReadScope : ReadScope<Preferences> {
        override fun close() {}

        override suspend fun readData(): Preferences {
            return preferences
        }
    }

    inner class MockWriteScope : WriteScope<Preferences> {
        override fun close() {}

        override suspend fun readData(): Preferences {
            return preferences
        }

        override suspend fun writeData(value: Preferences) {
            preferences = value
        }
    }
}
