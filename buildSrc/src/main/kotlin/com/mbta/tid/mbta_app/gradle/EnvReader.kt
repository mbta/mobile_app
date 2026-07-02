package com.mbta.tid.mbta_app.gradle

import java.io.BufferedReader
import java.io.File
import java.io.StringReader
import java.util.Properties

class EnvReader {
    val envFile = File(".envrc")
    val props = Properties()

    init {
        if (envFile.exists()) {
            val bufferedReader: BufferedReader = envFile.bufferedReader()
            bufferedReader.use {
                it.readLines()
                    .filter { line -> line.startsWith("export") }
                    .map { line ->
                        val cleanLine = line.replace("export", "")
                        props.load(StringReader(cleanLine))
                    }
            }
        } else {
            println(".envrc file not configured, reading from system env instead")
        }
    }

    operator fun get(key: String): String? = props.getProperty(key) ?: System.getenv(key)
}
