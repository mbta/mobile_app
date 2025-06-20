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
import org.w3c.dom.Node
import org.w3c.dom.NodeList

abstract class ConvertIosLocalizationTask : DefaultTask() {
    @get:InputFile abstract var androidEnglishStrings: RegularFile
    @get:InputFile abstract var xcstrings: RegularFile
    @get:OutputDirectory abstract var resources: Directory

    @TaskAction
    fun run() {
        val iosStrings = readIosStrings()
        val languageTags = iosStrings.values.map { it.keys }.reduce(Set<String>::plus) - "en"
        val androidResourcesById = parseAndroidStrings(androidEnglishStrings)
        val androidIdsByEnglishKey =
            androidResourcesById.entries.groupBy({ it.value.key() }, { it.key })

        for (languageTag in languageTags) {
            val iosResourcesByEnglishKey = iosStrings.mapValues { it.value[languageTag] }
            val translatedStringsById =
                iosResourcesByEnglishKey
                    .flatMap { (key, translations) ->
                        val androidIds =
                            when (key) {
                                is IosKey.AndroidKey -> listOf(key.key)
                                is IosKey.English -> androidIdsByEnglishKey[key.text]
                            }
                        if (androidIds == null || translations == null) return@flatMap emptyList()
                        androidIds.map { Pair(it, translations) }
                    }
                    .toMap()
            writeAndroidStrings(languageTag, translatedStringsById)
        }
    }

    /** Returns (key => (BCP 47 tag => translated resource)). */
    private fun readIosStrings(): Map<IosKey, Map<String, Resource>> {
        val inputData = xcstrings.asFile.readText()
        val strings = Json.decodeFromString<XcStrings>(inputData)

        strings.checkNoMissingTranslations()

        return strings.resourcesByKey()
    }

    @Serializable
    private data class XcStrings(
        val sourceLanguage: String,
        val strings: Map<String, XcStringInfo>,
        val version: String,
    ) {
        fun checkNoMissingTranslations() {
            for ((stringKey, stringInfo) in strings) {
                stringInfo.checkNoMissingTranslations(stringKey)
            }
        }

        fun resourcesByKey(): Map<IosKey, Map<String, Resource>> {
            return strings
                .mapNotNull {
                    val translations = it.value.resources() ?: return@mapNotNull null
                    val englishKey =
                        when {
                            it.key.startsWith("key/") ->
                                IosKey.AndroidKey(it.key.removePrefix("key/"))
                            translations["en"] != null -> IosKey.English(translations["en"]!!.key())
                            else -> IosKey.English(convertIosTemplate(it.key))
                        }
                    Pair(englishKey, translations)
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
        fun checkNoMissingTranslations(stringKey: String) {
            for ((languageTag, localization) in localizations.orEmpty()) {
                if (languageTag != "en") {
                    localization.checkNoMissingTranslations(stringKey, languageTag)
                }
            }
        }

        fun resources() =
            localizations
                ?.mapNotNull {
                    Pair(
                        convertIosTemplate(it.key),
                        it.value.resource()?.convertIosTemplate() ?: return@mapNotNull null,
                    )
                }
                ?.toMap()
    }

    @Serializable
    private data class Localization(
        val stringUnit: StringUnit? = null,
        val variations: Variations? = null,
    ) {
        fun checkNoMissingTranslations(
            stringKey: String,
            languageTag: String,
            quantity: Quantity? = null,
        ) {
            if (stringUnit != null) {
                check(stringUnit.state != "new") {
                    buildString {
                        append("iOS string \"")
                        append(stringKey)
                        append("\" language \"")
                        append(languageTag)
                        if (quantity != null) {
                            append("\" quantity \"")
                            append(quantity)
                        }
                        append("\" missing translation")
                    }
                }
            }
            if (variations != null) {
                for ((thisQuantity, localization) in variations.plural) {
                    localization.checkNoMissingTranslations(stringKey, languageTag, thisQuantity)
                }
            }
        }

        fun resource(): Resource? = stringUnit?.resource() ?: variations?.resource()
    }

    @Serializable
    private data class StringUnit(val state: String, val value: String) {
        fun resource() = Resource.StaticString(value)
    }

    @Serializable
    private data class Variations(val plural: Map<Quantity, Localization>) {
        fun resource() =
            Resource.Plural(plural.mapValues { checkNotNull(it.value.stringUnit).value })
    }

    private sealed interface Resource {
        fun key(): String

        fun convertIosTemplate(): Resource

        data class StaticString(val text: String) : Resource {
            override fun key() = text

            override fun convertIosTemplate() = StaticString(convertIosTemplate(text))
        }

        data class Plural(val items: Map<Quantity, String>) : Resource {
            override fun key() = checkNotNull(items[Quantity.other])

            override fun convertIosTemplate() =
                Plural(items.mapValues { convertIosTemplate(it.value) })

            fun itemsPotentiallyExtended(languageTag: String): Map<Quantity, String> {
                // For Spanish, French, and Portuguese, decimals are formatted under the `many` case
                // rather than `other` (in Spanish, "2 days" is "2 días" but "0.5 days" is "0.5 de
                // días"), and so Android complains that we have not given "many" even if it's not
                // actually going to happen.
                return if (languageTag in setOf("es", "fr", "pt-BR")) {
                    val otherTranslation = items.getValue(Quantity.other)
                    // only patch this if the quantity is actually formatted as an integer
                    if (otherTranslation.contains("%1\$d")) {
                        items + mapOf(Quantity.many to otherTranslation)
                    } else items
                } else items
            }
        }
    }

    private sealed interface IosKey {
        data class English(val text: String) : IosKey

        data class AndroidKey(val key: String) : IosKey
    }

    @Suppress("EnumEntryName")
    @Serializable
    private enum class Quantity {
        zero,
        one,
        two,
        few,
        many,
        other,
    }

    /** @return A map from IDs to resources. */
    private fun parseAndroidStrings(stringsFile: RegularFile): Map<String, Resource> {
        val parser = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val xmlDocument = parser.parse(stringsFile.asFile)
        val result = mutableMapOf<String, Resource>()

        val stringElements = xmlDocument.getElementsByTagName("string")
        for (stringElement in stringElements) {
            val id = stringElement.attributes.getNamedItem("name").textContent
            val value = stringElement.textContent
            result[id] = Resource.StaticString(value)
        }

        val pluralElements = xmlDocument.getElementsByTagName("plurals")
        for (pluralElement in pluralElements) {
            val id = pluralElement.attributes.getNamedItem("name").textContent
            val values = buildMap {
                for (child in pluralElement.childNodes) {
                    if (child.nodeName == "item") {
                        val quantity =
                            Quantity.valueOf(child.attributes.getNamedItem("quantity").textContent)
                        set(quantity, child.textContent)
                    }
                }
            }
            result[id] = Resource.Plural(values)
        }

        return result
    }

    private fun writeAndroidStrings(languageTag: String, stringsById: Map<String, Resource>) {
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
            for ((id, resource) in entriesToWrite) {
                when (resource) {
                    is Resource.StaticString -> {
                        val escapedValue = escapeXML(resource.text)
                        appendLine("    <string name=\"$id\">$escapedValue</string>")
                    }
                    is Resource.Plural -> {
                        appendLine("    <plurals name=\"$id\">")
                        val itemsToWrite =
                            resource.itemsPotentiallyExtended(languageTag).entries.sortedBy {
                                it.key
                            }
                        for ((quantity, value) in itemsToWrite) {
                            appendLine(
                                "        <item quantity=\"${quantity.name}\">${escapeXML(value)}</item>"
                            )
                        }
                        appendLine("    </plurals>")
                    }
                }
            }
            appendLine("</resources>")
        }
        outputFile.asFile.writeText(result)
    }

    operator fun NodeList.iterator() =
        object : Iterator<Node> {
            var index = 0

            override fun hasNext() = index < this@iterator.length

            override fun next() = this@iterator.item(index).also { index++ }
        }

    companion object {
        private fun escapeXML(text: String) = text.replace("&", "&amp;").replace("<", "&lt;")

        private fun escapeLeadingSpace(text: String) =
            if (text.startsWith(" ")) text.replaceRange(0, 1, "\\u0020") else text

        private val template = Regex("""%(?:(?<index>\d+)\$)?l?(?<format>[\w@])""")

        private fun replaceTemplate(match: MatchResult, getDefaultIndex: () -> Int): String {
            val index = match.groups["index"]?.value ?: getDefaultIndex().toString()
            val format = match.groups["format"]?.value.takeUnless { it == "@" } ?: "s"
            return "%$index$$format"
        }

        private fun convertIosTemplate(iosTemplate: String): String {
            var unspecifiedIndex = 1
            return escapeLeadingSpace(iosTemplate)
                .replace(template) {
                    replaceTemplate(it) { unspecifiedIndex.also { unspecifiedIndex += 1 } }
                }
                /*
                In order to preserve style tags while simultaneously formatting the string with parameters, we
                must escape the html styling tags
                        https://developer.android.com/guide/topics/resources/string-resource.html#StylingWithHTML
                */
                .replace("""\*\*([^*]+)\*\*""".toRegex(), "<b>$1</b>")
        }
    }
}
