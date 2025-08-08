package com.mbta.tid.mbta_app.gradle

import java.util.Base64
import org.cyclonedx.model.AttachmentText
import org.cyclonedx.parsers.JsonParser
import org.cyclonedx.parsers.XmlParser
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

abstract class DependencyCodegenTask : DefaultTask() {
    @get:InputFile abstract var inputPath: Provider<RegularFile>
    @get:OutputFile abstract var outputPath: RegularFile

    private fun AttachmentText.decoded(): String =
        when (this.encoding) {
            null -> this.text
            "base64" -> String(Base64.getDecoder().decode(this.text))
            else -> throw NotImplementedError("encoding $encoding not supported")
        }

    private val org.cyclonedx.model.Component.displayName: String
        get() =
            if (group == null) {
                name
            } else if (purl.startsWith("pkg:maven/")) {
                "$group:$name"
            } else {
                "${group.removePrefix("github.com/")}/$name"
            }

    private val trailingWhitespace = Regex("[ \t]+$", RegexOption.MULTILINE)

    @TaskAction
    fun run() {
        val inputPath = this.inputPath.get().asFile
        val outputPath = this.outputPath.asFile
        val bom =
            (if (inputPath.extension == "json") JsonParser() else XmlParser()).parse(inputPath)
        val dependencyLines =
            bom.components
                .sortedBy { it.displayName.lowercase() }
                .mapNotNull { component ->
                    val purl = component.purl
                    val name = component.displayName
                    val licenseText =
                        try {
                            component.licenses.licenses.joinToString("\n") {
                                it.attachmentText.decoded().replace(trailingWhitespace, "")
                            }
                        } catch (e: Throwable) {
                            logger.error("bad license for $purl: $e")
                            return@mapNotNull null
                        }
                    val multiLineStringDelimiter = "\"\"\""
                    """
        Dependency(
            "$purl",
            "$name",
            $multiLineStringDelimiter$licenseText$multiLineStringDelimiter
        )
                    """
                        .trim()
                }
        val outputText =
            """
package com.mbta.tid.mbta_app.model

public actual fun Dependency.Companion.getAllDependencies(): List<Dependency> =
    listOf(
        ${dependencyLines.joinToString(",\n        ")}
    )
"""
                .trimStart()
        outputPath.writeText(outputText)
    }
}
