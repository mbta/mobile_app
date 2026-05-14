package com.mbta.tid.mbta_app.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction

abstract class CheckMapboxBridgeTask : DefaultTask() {
    @get:InputFile abstract var mapboxBridgePath: RegularFile
    @get:InputDirectory abstract var sharedMapStylePackage: Directory

    @TaskAction
    fun run() {
        val mapboxBridgeSource = mapboxBridgePath.asFile.readText()

        // \s is whitespace, \S is non-whitespace, (?=\S) is positive-lookahead non-whitespace
        // so this splits on blank lines that are not followed by indentation
        val mapboxBridgePieces = mapboxBridgeSource.split(Regex("\n\n(?=\\S)"))

        val lineLayerBridge =
            checkNotNull(
                mapboxBridgePieces.find { it.startsWith("suspend fun LineLayer.toMapbox()") }
            )
        val lineLayerIdentifiers = identifiersInCode(lineLayerBridge)
        val symbolLayerBridge =
            checkNotNull(
                mapboxBridgePieces.find { it.startsWith("suspend fun SymbolLayer.toMapbox()") }
            )
        val symbolLayerIdentifiers = identifiersInCode(symbolLayerBridge)

        val sharedLayerPath = sharedMapStylePackage.file("Layer.kt")
        val layerProperties = getPropertiesDeclaredInFile(sharedLayerPath)

        val sharedLineLayerPath = sharedMapStylePackage.file("LineLayer.kt")
        val lineLayerProperties = getPropertiesDeclaredInFile(sharedLineLayerPath)

        val sharedSymbolLayerPath = sharedMapStylePackage.file("SymbolLayer.kt")
        val symbolLayerProperties = getPropertiesDeclaredInFile(sharedSymbolLayerPath)

        val badProperties = mutableListOf<Pair<String, Int>>()

        for (property in layerProperties + lineLayerProperties) {
            val references = lineLayerIdentifiers.count { it == property }
            if (references != 2) {
                badProperties.add("LineLayer.$property" to references)
            }
        }

        for (property in layerProperties + symbolLayerProperties) {
            val references = symbolLayerIdentifiers.count { it == property }
            if (references != 2) {
                badProperties.add("SymbolLayer.$property" to references)
            }
        }

        if (badProperties.isNotEmpty()) {
            throw IllegalStateException("Layer properties not referenced 2 times: $badProperties")
        }
    }

    private val layerProperty = Regex("va[lr] (?<name>\\w+): ")
    private val identifier = Regex("\\w+")

    private fun getPropertiesDeclaredInFile(file: RegularFile) =
        layerProperty
            .findAll(file.asFile.readText())
            .map { checkNotNull(it.groups["name"]).value }
            .filter { it !in setOf("id", "type", "source") }

    private fun identifiersInCode(code: String) = identifier.findAll(code).map { it.value }
}
