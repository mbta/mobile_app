package com.mbta.tid.mbta_app.gradle

import kotlin.io.path.writeText
import org.cyclonedx.Version
import org.cyclonedx.generators.json.BomJsonGenerator
import org.cyclonedx.generators.xml.BomXmlGenerator
import org.cyclonedx.model.Bom
import org.cyclonedx.parsers.JsonParser
import org.cyclonedx.parsers.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class CycloneDxBomTransformTask : DefaultTask() {
    @get:InputFile abstract var inputPath: Provider<RegularFile>
    @get:OutputFile abstract var outputPath: Provider<RegularFile>
    @get:Internal abstract var transform: Bom.() -> Unit

    @TaskAction
    fun run() {
        val inputPath = this.inputPath.get().asFile
        val outputPath = this.outputPath.get().asFile
        val bom =
            (if (inputPath.extension == "json") JsonParser() else XmlParser()).parse(inputPath)
        bom.transform()
        val outputText =
            if (outputPath.extension == "json") {
                BomJsonGenerator(bom, Version.VERSION_16).toJsonString()
            } else {
                BomXmlGenerator(bom, Version.VERSION_16).toXmlString()
            }
        outputPath.toPath().writeText(outputText)
    }
}
