import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.readText

val layerProperty = Regex("va[lr] (?<name>\\w+): ")
val identifier = Regex("\\w+")

fun getPropertiesDeclaredInFile(file: Path) =
    layerProperty
        .findAll(file.readText())
        .map { checkNotNull(it.groups["name"]).value }
        .filter { it !in setOf("id", "type", "source") }

fun identifiersInCode(code: String) = identifier.findAll(code).map { it.value }

tasks.register("checkMapboxBridge") {
    val mapboxBridgePath =
        Path("$projectDir/src/main/java/com/mbta/tid/mbta_app/android/map/MapboxBridge.kt")
    val mapboxBridgeSource = mapboxBridgePath.readText()

    // \s is whitespace, \S is non-whitespace, (?=\S) is positive-lookahead non-whitespace
    // so this splits on blank lines that are not followed by indentation
    val mapboxBridgePieces = mapboxBridgeSource.split(Regex("\n\n(?=\\S)"))

    val lineLayerBridge =
        checkNotNull(mapboxBridgePieces.find { it.startsWith("suspend fun LineLayer.toMapbox()") })
    val lineLayerIdentifiers = identifiersInCode(lineLayerBridge)
    val symbolLayerBridge =
        checkNotNull(
            mapboxBridgePieces.find { it.startsWith("suspend fun SymbolLayer.toMapbox()") }
        )
    val symbolLayerIdentifiers = identifiersInCode(symbolLayerBridge)

    val sharedMapStylePackage =
        Path("$projectDir/../shared/src/commonMain/kotlin/com/mbta/tid/mbta_app/map/style")

    val sharedLayerPath = sharedMapStylePackage.resolve("Layer.kt")
    val layerProperties = getPropertiesDeclaredInFile(sharedLayerPath)

    val sharedLineLayerPath = sharedMapStylePackage.resolve("LineLayer.kt")
    val lineLayerProperties = getPropertiesDeclaredInFile(sharedLineLayerPath)

    val sharedSymbolLayerPath = sharedMapStylePackage.resolve("SymbolLayer.kt")
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
