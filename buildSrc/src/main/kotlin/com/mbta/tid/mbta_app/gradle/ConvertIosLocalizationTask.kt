package com.mbta.tid.mbta_app.gradle

import javax.xml.parsers.DocumentBuilderFactory
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

abstract class ConvertIosLocalizationTask : DefaultTask() {
    @get:InputFile abstract var androidEnglishStrings: RegularFile
    @get:InputFile abstract var xcstrings: RegularFile
    @get:OutputDirectory abstract var resources: Directory

    @TaskAction
    fun run() {
        val iosStrings = readIosStrings()
        val languageTags = iosStrings.values.map { it.keys }.reduce(Set<String>::plus) - "en"
        val androidEnglishStringsById = parseAndroidStrings(androidEnglishStrings)
        val androidIdsByEnglishString =
            androidEnglishStringsById.entries.groupBy({ it.value }, { it.key })

        for (languageTag in languageTags) {
            val iosStringsByEnglishString = iosStrings.mapValues { it.value[languageTag] }
            val translatedStringsById =
                iosStringsByEnglishString
                    .flatMap { (englishString, translations) ->
                        val androidIds = androidIdsByEnglishString[englishString]
                        if (androidIds == null || translations == null) return@flatMap emptyList()
                        androidIds.map { Pair(it, translations) }
                    }
                    .toMap()
            writeAndroidStrings(languageTag, translatedStringsById)
        }
    }

    /** Returns (English text => (BCP 47 tag => translated text)) for non-plural strings. */
    private fun readIosStrings(): Map<String, Map<String, String>> {
        val inputData = xcstrings.asFile.readText()
        val strings = Json.decodeFromString<XcStrings>(inputData)
        return strings.staticStringsByEnglishText()
    }

    @Serializable
    private data class XcStrings(
        val sourceLanguage: String,
        val strings: Map<String, XcStringInfo>,
        val version: String,
    ) {
        fun staticStringsByEnglishText(): Map<String, Map<String, String>> {
            return strings
                .mapNotNull {
                    val translations = it.value.invariantLocalizations() ?: return@mapNotNull null
                    val englishText = translations["en"] ?: convertIosTemplate(it.key)
                    Pair(englishText, translations)
                }
                .toMap()
        }
    }

    @Serializable
    private data class XcStringInfo(
        val comment: String? = null,
        val extractionState: String? = null,
        val localizations: Map<String, Localization>? = null,
    ) {

        fun invariantLocalizations() =
            localizations
                ?.mapNotNull {
                    Pair(
                        convertIosTemplate(it.key),
                        convertIosTemplate(it.value.stringUnit?.value ?: return@mapNotNull null)
                    )
                }
                ?.toMap()
    }

    @Serializable
    private data class Localization(
        val stringUnit: StringUnit? = null,
        val variations: Variations? = null,
    )

    @Serializable private data class StringUnit(val state: String, val value: String)

    @Serializable private data class Variations(val plural: Map<String, Localization>)

    /** @return A map from IDs to text content. */
    private fun parseAndroidStrings(stringsFile: RegularFile): Map<String, String> {
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val xmlDocument = parser.parse(stringsFile.asFile)
        val stringElements = xmlDocument.getElementsByTagName("string")
        val result = mutableMapOf<String, String>()
        for (i in 0 until stringElements.length) {
            val stringElement = stringElements.item(i)
            val id = stringElement.attributes.getNamedItem("name").textContent
            val value = stringElement.textContent
            result[id] = value
        }
        return result
    }

    private fun writeAndroidStrings(languageTag: String, stringsById: Map<String, String>) {
        val outputDir = resources.dir("values-b+${languageTag.replace("-", "+")}")
        outputDir.asFile.mkdirs()
        val overrideFile = outputDir.file("strings.xml")
        val overrideStrings = parseAndroidStrings(overrideFile)
        val entriesToWrite =
            stringsById.filterNot { it.key in overrideStrings.keys }.entries.sortedBy { it.key }
        val outputFile = outputDir.file("strings_ios_converted.xml")
        val result = buildString {
            appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
            appendLine("<resources>")
            for ((id, value) in entriesToWrite) {
                val escapedValue =
                    value
                        .replace("&", "&amp;")
                        .replace("""\*\*([^*]+)\*\*""".toRegex(), "<b>$1</b>")
                        .replace("<", "&lt;")
                appendLine("    <string name=\"$id\">$escapedValue</string>")
            }
            appendLine("</resources>")
        }
        outputFile.asFile.writeText(result)
    }

    companion object {
        private val template = Regex("""%(?:(?<index>\d+)\$)?l?(?<format>[\w@])""")

        private fun replaceTemplate(match: MatchResult, getDefaultIndex: () -> Int): String {
            val index = match.groups["index"]?.value ?: getDefaultIndex().toString()
            val format = match.groups["format"]?.value.takeUnless { it == "@" } ?: "s"
            return "%$index$$format"
        }

        private fun convertIosTemplate(iosTemplate: String): String {
            var unspecifiedIndex = 1
            return iosTemplate.replace(template) {
                replaceTemplate(it) { unspecifiedIndex.also { unspecifiedIndex += 1 } }
            }
        }
    }
}
