package com.mbta.tid.mbta_app.gradle

import java.nio.file.Path
import javax.inject.Inject
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.getLastModifiedTime
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.writeText
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.process.ProcessExecutionException

abstract class ConvertIosMapIconsTask @Inject constructor(private val exec: ExecOperations) :
    DefaultTask() {
    /** Globs to search within Icons.xcassets and render to PNGs. */
    @get:Input abstract var assetsToRender: List<String>
    /**
     * Globs to search within Icons.xcassets and not render to PNGs but include in drawableByName.
     */
    @get:Input abstract var assetsToReturnByName: List<String>
    private val projectDir = this.project.projectDir
    private val json = Json { ignoreUnknownKeys = true }

    @TaskAction
    fun convertIcons() {
        checkRsvgConvert()

        val iconsXcassets = Path("${projectDir.parent}/iosApp/iosApp/Icons.xcassets")

        val resourceSets =
            listOf(ColorSchemeFilter.DARK, null).associateWith { colorScheme ->
                dpi.keys.associateWith { dpiQualifier -> ResourceSet(colorScheme, dpiQualifier) }
            }

        val resourceNameMap = mutableMapOf<String, String>()

        val imagesets =
            assetsToRender.flatMap { glob ->
                iconsXcassets.listDirectoryEntries("$glob.imageset").map { Pair(it, true) }
            } +
                assetsToReturnByName.flatMap { glob ->
                    iconsXcassets.listDirectoryEntries("$glob.imageset").map { Pair(it, false) }
                }
        for ((imageset, shouldRender) in imagesets) {
            val metadata = metadataFrom(imageset)
            val svgs = metadata.images.filter { it.filename.endsWith(".svg") }
            if (metadata.images.size != svgs.size) {
                continue
            }
            val imagesetName = imageset.fileName.nameWithoutExtension
            val drawableName = imagesetName.lowercase().replace("-", "_")

            resourceNameMap[imagesetName] = drawableName

            if (shouldRender) {
                convertImageset(imageset, resourceSets, drawableName)
            }
        }

        generateDrawableByName(resourceNameMap)
    }

    @Serializable data class ImagesetMetadata(val images: List<ImageMetadata>)

    @Serializable
    data class ImageMetadata(
        val appearances: List<AppearanceFilter> = emptyList(),
        val filename: String,
    )

    @Serializable data class AppearanceFilter(val appearance: String, val value: String)

    enum class ColorSchemeFilter {
        DARK
    }

    private val darkModeFilter = AppearanceFilter("luminosity", "dark")

    private val dpi =
        mapOf("ldpi" to 120, "mdpi" to 160, "hdpi" to 240, "xhdpi" to 320, "xxhdpi" to 480)

    inner class ResourceSet(colorScheme: ColorSchemeFilter?, dpiQualifier: String) {
        val path =
            Path(
                projectDir.toString(),
                "src/main/res",
                listOfNotNull(
                        "drawable",
                        when (colorScheme) {
                            ColorSchemeFilter.DARK -> "night"
                            null -> null
                        },
                        dpiQualifier,
                    )
                    .joinToString("-"),
            )

        init {
            path.createDirectories()
        }
    }

    private fun checkRsvgConvert() {
        try {
            exec.exec { commandLine("rsvg-convert", "--version") }
        } catch (ex: ProcessExecutionException) {
            throw IllegalStateException(
                "rsvg-convert not found, ${when {
                    Os.isFamily(Os.FAMILY_MAC) -> "`brew install librsvg`"
                    Os.isFamily(Os.FAMILY_UNIX) -> "`apt-get install librsvg2-bin`"
                    else -> "install it"
                } } or see Gotchas in README",
                ex,
            )
        }
    }

    private fun convert(
        sourcePath: Path,
        destinationSet: ResourceSet,
        destinationBaseName: String,
        dpi: Int,
    ) {
        val destinationFilename = "$destinationBaseName.png"
        val destinationPath = destinationSet.path.resolve(destinationFilename)

        if (
            !destinationPath.exists() ||
                sourcePath.getLastModifiedTime() > destinationPath.getLastModifiedTime()
        ) {
            val result =
                exec.exec {
                    commandLine(
                        "rsvg-convert",
                        sourcePath,
                        "--dpi-x",
                        dpi,
                        "--dpi-y",
                        dpi,
                        "-z",
                        dpi / 160.0,
                        "-o",
                        destinationPath,
                    )
                }
            result.assertNormalExitValue()
        }
    }

    private fun convertImageset(
        imageset: Path,
        resourceSets: Map<ColorSchemeFilter?, Map<String, ResourceSet>>,
        drawableName: String,
    ) {
        val metadata = metadataFrom(imageset)
        val images = metadata.images.filter { it.filename.endsWith(".svg") }

        if (images.size == 2 && images.count { it.appearances.contains(darkModeFilter) } == 1) {
            val lightModeImage =
                checkNotNull(images.find { !it.appearances.contains(darkModeFilter) })
            val darkModeImage =
                checkNotNull(images.find { it.appearances.contains(darkModeFilter) })

            for ((dpiQualifier, dpiValue) in dpi) {
                convert(
                    imageset.resolve(lightModeImage.filename),
                    resourceSets.getValue(null).getValue(dpiQualifier),
                    drawableName,
                    dpiValue,
                )
                convert(
                    imageset.resolve(darkModeImage.filename),
                    resourceSets.getValue(ColorSchemeFilter.DARK).getValue(dpiQualifier),
                    drawableName,
                    dpiValue,
                )
            }
        } else if (images.size == 1) {
            val image = images.single()

            for ((dpiQualifier, dpiValue) in dpi) {
                convert(
                    imageset.resolve(image.filename),
                    resourceSets.getValue(null).getValue(dpiQualifier),
                    drawableName,
                    dpiValue,
                )
            }
        } else {
            if (metadata.images.size == images.size) {
                throw IllegalStateException("Imageset $imageset is neither light/dark nor unsplit")
            }
        }
    }

    private fun generateDrawableByName(resourceNameMap: MutableMap<String, String>) {
        val targetPath =
            Path("$projectDir/src/main/java/com/mbta/tid/mbta_app/android/map/drawableByName.kt")

        val drawableByName = buildString {
            appendLine("package com.mbta.tid.mbta_app.android.generated")
            appendLine()
            appendLine("import com.mbta.tid.mbta_app.android.R")
            appendLine()
            appendLine("fun drawableByName(name: String): Int =")
            appendLine("    when (name) {")
            for ((name, drawable) in resourceNameMap.toSortedMap()) {
                appendLine("        \"$name\" -> R.drawable.$drawable")
            }
            appendLine(
                "        else -> throw IllegalArgumentException(\"Invalid drawable name \$name\")"
            )
            appendLine("    }")
        }

        targetPath.writeText(drawableByName)
    }

    private fun metadataFrom(imageset: Path): ImagesetMetadata {
        @OptIn(ExperimentalSerializationApi::class)
        return json.decodeFromStream(imageset.resolve("Contents.json").inputStream())
    }
}
