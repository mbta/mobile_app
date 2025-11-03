package com.mbta.tid.mbta_app.kdTree

import kotlin.math.log2
import kotlin.math.max
import kotlin.math.round
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.maplibre.spatialk.geojson.BoundingBox
import org.maplibre.spatialk.geojson.Feature
import org.maplibre.spatialk.geojson.FeatureCollection
import org.maplibre.spatialk.geojson.Geometry
import org.maplibre.spatialk.geojson.Point
import org.maplibre.spatialk.geojson.Position
import org.maplibre.spatialk.geojson.dsl.buildFeature
import org.maplibre.spatialk.geojson.dsl.lineStringOf
import org.maplibre.spatialk.geojson.toJson
import org.maplibre.spatialk.turf.measurement.toPolygon
import org.maplibre.spatialk.units.extensions.inMiles
import org.maplibre.spatialk.units.extensions.miles

class KdTreeTest {
    private fun point(id: String, latitude: Double, longitude: Double) =
        Pair(id, Position(latitude = latitude, longitude = longitude))

    val commuterRailPoints =
        listOf(
            point("CM-0493-S", 41.758333, -70.714722),
            point("CM-0547-S", 41.744805, -70.616226),
            point("CM-0564-S", 41.746497, -70.588772),
            point("CM-0790-S", 41.660225, -70.276583),
            point("DB-0095", 42.238405, -71.133246),
            point("FB-0095-04", 42.237769, -71.133569),
            point("FB-0095-05", 42.237781, -71.134295),
            point("NEC-2192-02", 42.238831, -71.133281),
            point("NEC-2192-03", 42.238864, -71.133433),
            point("DB-2205", 42.253638, -71.11927),
            point("DB-2205-01", 42.253638, -71.11927),
            point("DB-2205-02", 42.253638, -71.11927),
            point("DB-2222", 42.271466, -71.095782),
            point("DB-2222-01", 42.271466, -71.095782),
            point("DB-2222-02", 42.271466, -71.095782),
            point("DB-2230", 42.280994, -71.085475),
            point("DB-2230-01", 42.280994, -71.085475),
            point("DB-2230-02", 42.280994, -71.085475),
            point("DB-2240", 42.292246, -71.07814),
            point("DB-2240-01", 42.292246, -71.07814),
            point("DB-2240-02", 42.292246, -71.07814),
            point("DB-2249", 42.305037, -71.076833),
            point("DB-2249-01", 42.303955, -71.077979),
            point("DB-2249-02", 42.305692, -71.076096),
            point("DB-2258", 42.319125, -71.068627),
            point("DB-2258-01", 42.319125, -71.068627),
            point("DB-2258-02", 42.319125, -71.068627),
            point("DB-2265", 42.327415, -71.065674),
            point("DB-2265-01", 42.327415, -71.065674),
            point("DB-2265-02", 42.327415, -71.065674),
            point("ER-0099", 42.449927, -70.969848),
            point("ER-0099-01", 42.449927, -70.969848),
            point("ER-0099-02", 42.449927, -70.969848),
            point("ER-0115", 42.462953, -70.945421),
            point("ER-0115-01", 42.462953, -70.945421),
            point("ER-0115-02", 42.462953, -70.945421),
            point("ER-0117-01", 42.465397, -70.940001),
            point("ER-0117-02", 42.465165, -70.940878),
            point("ER-0128", 42.473743, -70.922537),
            point("ER-0128-01", 42.473743, -70.922537),
            point("ER-0128-02", 42.473743, -70.922537),
            point("ER-0168-S", 42.524792, -70.895876),
            point("ER-0183", 42.547276, -70.885432),
            point("ER-0183-01", 42.547276, -70.885432),
            point("ER-0183-02", 42.547276, -70.885432),
            point("ER-0208", 42.583779, -70.883851),
            point("ER-0208-01", 42.583779, -70.883851),
            point("ER-0208-02", 42.583779, -70.883851),
            point("ER-0227-S", 42.609212, -70.874801),
            point("ER-0276-S", 42.676921, -70.840589),
            point("ER-0312-S", 42.726845, -70.859034),
            point("ER-0362", 42.797837, -70.87797),
            point("ER-0362-01", 42.797837, -70.87797),
            point("ER-0362-02", 42.797837, -70.87797),
            point("FB-0109", 42.233249, -71.158647),
            point("FB-0109-01", 42.233249, -71.158647),
            point("FB-0109-02", 42.233249, -71.158647),
            point("FB-0118", 42.227079, -71.174254),
            point("FB-0118-01", 42.227079, -71.174254),
            point("FB-0118-02", 42.227079, -71.174254),
            point("FB-0125", 42.22105, -71.183961),
            point("FB-0125-01", 42.22105, -71.183961),
            point("FB-0125-02", 42.22105, -71.183961),
            point("FB-0143", 42.196857, -71.196688),
            point("FB-0143-01", 42.196857, -71.196688),
            point("FB-0143-02", 42.196857, -71.196688),
            point("FB-0148", 42.188775, -71.199665),
            point("FB-0148-01", 42.188775, -71.199665),
            point("FB-0148-02", 42.188775, -71.199665),
            point("FB-0166-S", 42.172127, -71.219366),
            point("FB-0177-S", 42.159123, -71.236125),
            point("FB-0191-S", 42.145477, -71.25779),
            point("FB-0230-S", 42.120694, -71.325217),
            point("FB-0275-S", 42.083238, -71.396102),
            point("FB-0303-S", 42.089941, -71.43902),
            point("FR-0064", 42.395896, -71.17619),
            point("FR-0064-01", 42.395896, -71.17619),
            point("FR-0064-02", 42.395896, -71.17619),
            point("FR-0074", 42.3876, -71.190744),
            point("FR-0074-01", 42.3876, -71.190744),
            point("FR-0074-02", 42.3876, -71.190744),
            point("FR-0098", 42.374296, -71.235615),
            point("FR-0098-01", 42.373698, -71.238672),
            point("FR-0098-S", 42.374296, -71.235615),
            point("FR-0115", 42.361898, -71.260065),
            point("FR-0115-01", 42.361898, -71.260065),
            point("FR-0115-02", 42.361898, -71.260065),
            point("FR-0132", 42.37897, -71.282411),
            point("FR-0132-01", 42.37897, -71.282411),
            point("FR-0132-02", 42.37897, -71.282411),
            point("FR-0137", 42.385755, -71.289203),
            point("FR-0137-01", 42.385755, -71.289203),
            point("FR-0137-02", 42.385755, -71.289203),
            point("FR-0147", 42.395625, -71.302357),
            point("FR-0147-01", 42.395625, -71.302357),
            point("FR-0147-02", 42.395625, -71.302357),
            point("FR-0167", 42.41342, -71.325404),
            point("FR-0167-01", 42.414983, -71.326462),
            point("FR-0167-02", 42.413641, -71.325512),
            point("FR-0201", 42.456565, -71.357677),
            point("FR-0201-01", 42.456565, -71.357677),
            point("FR-0201-02", 42.456565, -71.357677),
            point("FR-0219", 42.457043, -71.392892),
            point("FR-0219-01", 42.457043, -71.392892),
            point("FR-0219-02", 42.457043, -71.392892),
            point("FR-0253", 42.460375, -71.457744),
            point("FR-0253-01", 42.460375, -71.457744),
            point("FR-0253-02", 42.460375, -71.457744),
            point("FR-0301", 42.519236, -71.502643),
            point("FR-0301-01", 42.519236, -71.502643),
            point("FR-0301-02", 42.519236, -71.502643),
            point("FR-0361", 42.559074, -71.588476),
            point("FR-0361-01", 42.559074, -71.588476),
            point("FR-0361-02", 42.559074, -71.588476),
            point("FR-0394", 42.545089, -71.648004),
            point("FR-0394-01", 42.545089, -71.648004),
            point("FR-0394-02", 42.545089, -71.648004),
            point("FR-0451", 42.539017, -71.739186),
            point("FR-0451-01", 42.539017, -71.739186),
            point("FR-0451-02", 42.539017, -71.739186),
            point("FR-0494-CS", 42.58072, -71.792611),
            point("FR-3338-CS", 42.553477, -71.848488),
            point("FS-0049-S", 42.0951, -71.26151),
            point("GB-0198", 42.562171, -70.869254),
            point("GB-0198-01", 42.562171, -70.869254),
            point("GB-0198-02", 42.562171, -70.869254),
            point("GB-0222", 42.559446, -70.825541),
            point("GB-0222-01", 42.559446, -70.825541),
            point("GB-0222-02", 42.559446, -70.825541),
            point("GB-0229", 42.561651, -70.811405),
            point("GB-0229-01", 42.561651, -70.811405),
            point("GB-0229-02", 42.561651, -70.811405),
            point("GB-0254", 42.573687, -70.77009),
            point("GB-0254-01", 42.573687, -70.77009),
            point("GB-0254-02", 42.573687, -70.77009),
            point("GB-0296", 42.611933, -70.705417),
            point("GB-0296-01", 42.611933, -70.705417),
            point("GB-0296-02", 42.611933, -70.705417),
            point("GB-0316-S", 42.616799, -70.668345),
            point("GB-0353-S", 42.655491, -70.627055),
            point("GRB-0118-S", 42.221503, -70.968152),
            point("GRB-0146-S", 42.2191, -70.9214),
            point("GRB-0162-S", 42.235838, -70.902708),
            point("GRB-0183-S", 42.244959, -70.869205),
            point("GRB-0199-S", 42.24421, -70.837529),
            point("GRB-0233-S", 42.219528, -70.788602),
            point("GRB-0276-S", 42.178776, -70.746641),
            point("KB-0351-S", 41.97762, -70.721709),
            point("MM-0150-S", 42.156343, -71.027371),
            point("MM-0186", 42.106555, -71.022001),
            point("MM-0186-CS", 42.106555, -71.022001),
            point("MM-0186-S", 42.106555, -71.022001),
            point("MM-0200", 42.084659, -71.016534),
            point("MM-0200-CS", 42.084659, -71.016534),
            point("MM-0200-S", 42.084659, -71.016534),
            point("MM-0219-S", 42.060951, -71.011004),
            point("MM-0277-S", 41.984916, -70.96537),
            point("MM-0356-S", 41.87821, -70.918444),
            point("NB-0064-S", 42.287442, -71.130283),
            point("NB-0072-S", 42.286588, -71.145557),
            point("NB-0076-S", 42.284969, -71.153937),
            point("NB-0080-S", 42.281358, -71.160065),
            point("NB-0109-S", 42.275648, -71.215528),
            point("NB-0120-S", 42.273187, -71.235559),
            point("NB-0127-S", 42.280775, -71.237686),
            point("NB-0137-S", 42.293444, -71.236027),
            point("NEC-1659-03", 41.581289, -71.491147),
            point("NEC-1768-03", 41.726599, -71.442453),
            point("NEC-1851", 41.829293, -71.413301),
            point("NEC-1851-01", 41.829293, -71.413301),
            point("NEC-1851-02", 41.829293, -71.413301),
            point("NEC-1851-03", 41.829293, -71.413301),
            point("NEC-1851-05", 41.829293, -71.413301),
            point("NEC-1891", 41.878762, -71.392),
            point("NEC-1891-01", 41.878804, -71.392048),
            point("NEC-1891-02", 41.878721, -71.391959),
            point("NEC-1919", 41.897943, -71.354621),
            point("NEC-1919-01", 41.897943, -71.354621),
            point("NEC-1919-02", 41.897943, -71.354621),
            point("NEC-1969", 41.940739, -71.285094),
            point("NEC-1969-03", 41.940739, -71.285094),
            point("NEC-1969-04", 41.940739, -71.285094),
            point("NEC-2040", 42.032787, -71.219917),
            point("NEC-2040-01", 42.032787, -71.219917),
            point("NEC-2040-02", 42.032787, -71.219917),
            point("NEC-2108", 42.124553, -71.184468),
            point("NEC-2108-01", 42.124553, -71.184468),
            point("NEC-2108-02", 42.124553, -71.184468),
            point("NEC-2139", 42.163204, -71.15376),
            point("NEC-2139-01", 42.162024, -71.153828),
            point("NEC-2139-02", 42.162197, -71.153718),
            point("SB-0150-04", 42.161746, -71.152837),
            point("SB-0150-06", 42.163115, -71.153581),
            point("NEC-2173", 42.210308, -71.147134),
            point("NEC-2173-01", 42.210308, -71.147134),
            point("NEC-2173-02", 42.210308, -71.147134),
            point("NEC-2203", 42.25503, -71.125526),
            point("NEC-2203-02", 42.25503, -71.125526),
            point("NEC-2203-03", 42.25503, -71.125526),
            point("NHRML-0055", 42.421776, -71.133342),
            point("NHRML-0055-01", 42.421776, -71.133342),
            point("NHRML-0055-02", 42.421776, -71.133342),
            point("NHRML-0073", 42.444948, -71.140169),
            point("NHRML-0073-01", 42.444948, -71.140169),
            point("NHRML-0073-02", 42.444948, -71.140169),
            point("NHRML-0078", 42.451088, -71.13783),
            point("NHRML-0078-01", 42.451088, -71.13783),
            point("NHRML-0078-02", 42.451088, -71.13783),
            point("NHRML-0116", 42.504402, -71.137618),
            point("NHRML-0116-01", 42.504402, -71.137618),
            point("NHRML-0116-02", 42.504402, -71.137618),
            point("NHRML-0127", 42.516987, -71.144475),
            point("NHRML-0127-01", 42.516987, -71.144475),
            point("NHRML-0127-02", 42.516987, -71.144475),
            point("NHRML-0152", 42.546624, -71.174334),
            point("NHRML-0152-01", 42.546624, -71.174334),
            point("NHRML-0152-02", 42.546624, -71.174334),
            point("NHRML-0218", 42.593248, -71.280995),
            point("NHRML-0218-01", 42.593248, -71.280995),
            point("NHRML-0218-02", 42.593248, -71.280995),
            point("NHRML-0254", 42.63535, -71.314543),
            point("NHRML-0254-03", 42.63535, -71.314543),
            point("NHRML-0254-04", 42.63535, -71.314543),
            point("PB-0158-S", 42.155025, -70.953302),
            point("PB-0194-S", 42.107156, -70.934405),
            point("PB-0212-S", 42.082749, -70.923411),
            point("PB-0245-S", 42.043967, -70.882438),
            point("PB-0281", 42.014739, -70.824263),
            point("PB-0281-CS", 42.014739, -70.824263),
            point("PB-0281-S", 42.014739, -70.824263),
            point("PB-0356-S", 41.981278, -70.690421),
            point("SB-0156-S", 42.157095, -71.14628),
            point("SB-0189-S", 42.124084, -71.103627),
            point("WML-0025", 42.347581, -71.099974),
            point("WML-0025-05", 42.347581, -71.099974),
            point("WML-0025-07", 42.347581, -71.099974),
            point("WML-0035", 42.357293, -71.139883),
            point("WML-0035-01", 42.357293, -71.139883),
            point("WML-0035-02", 42.357293, -71.139883),
            point("WML-0081-02", 42.351702, -71.205408),
            point("WML-0091-02", 42.347878, -71.230528),
            point("WML-0102-02", 42.345833, -71.250373),
            point("WML-0125", 42.323608, -71.272288),
            point("WML-0125-01", 42.323608, -71.272288),
            point("WML-0125-02", 42.323608, -71.272288),
            point("WML-0135", 42.31037, -71.277044),
            point("WML-0135-01", 42.31037, -71.277044),
            point("WML-0135-02", 42.31037, -71.277044),
            point("WML-0147", 42.297526, -71.294173),
            point("WML-0147-01", 42.297526, -71.294173),
            point("WML-0147-02", 42.297526, -71.294173),
            point("WML-0177", 42.285719, -71.347133),
            point("WML-0177-01", 42.285719, -71.347133),
            point("WML-0177-02", 42.285719, -71.347133),
            point("WML-0199", 42.283064, -71.391797),
            point("WML-0199-01", 42.283064, -71.391797),
            point("WML-0199-02", 42.283064, -71.391797),
            point("WML-0214", 42.276108, -71.420055),
            point("WML-0214-01", 42.276108, -71.420055),
            point("WML-0214-02", 42.276108, -71.420055),
            point("WML-0252", 42.26149, -71.482161),
            point("WML-0252-01", 42.26149, -71.482161),
            point("WML-0252-02", 42.26149, -71.482161),
            point("WML-0274", 42.267024, -71.524371),
            point("WML-0274-01", 42.267024, -71.524371),
            point("WML-0274-02", 42.267024, -71.524371),
            point("WML-0340", 42.269644, -71.647076),
            point("WML-0340-01", 42.269644, -71.647076),
            point("WML-0340-02", 42.269644, -71.647076),
            point("WML-0364", 42.2466, -71.685325),
            point("WML-0364-01", 42.2466, -71.685325),
            point("WML-0364-02", 42.2466, -71.685325),
            point("WML-0442-CS", 42.261835, -71.791806),
            point("WR-0062", 42.451731, -71.069379),
            point("WR-0062-01", 42.451731, -71.069379),
            point("WR-0062-02", 42.451731, -71.069379),
            point("WR-0067", 42.458768, -71.069789),
            point("WR-0067-01", 42.458768, -71.069789),
            point("WR-0067-02", 42.458768, -71.069789),
            point("WR-0075", 42.469464, -71.068297),
            point("WR-0075-01", 42.469464, -71.068297),
            point("WR-0075-02", 42.469464, -71.068297),
            point("WR-0085", 42.483005, -71.067247),
            point("WR-0085-01", 42.483005, -71.067247),
            point("WR-0085-02", 42.483005, -71.067247),
            point("WR-0099", 42.502126, -71.075566),
            point("WR-0099-01", 42.502126, -71.075566),
            point("WR-0099-02", 42.502126, -71.075566),
            point("WR-0120-S", 42.52221, -71.108294),
            point("WR-0163-S", 42.569661, -71.159696),
            point("WR-0205-02", 42.627356, -71.159962),
            point("WR-0228-02", 42.658336, -71.144502),
            point("WR-0264-02", 42.701806, -71.15198),
            point("WR-0325", 42.766912, -71.088411),
            point("WR-0325-01", 42.766912, -71.088411),
            point("WR-0325-02", 42.766912, -71.088411),
            point("WR-0329", 42.773474, -71.086237),
            point("WR-0329-01", 42.773474, -71.086237),
            point("WR-0329-02", 42.773474, -71.086237),
            point("NEC-2276", 42.34735, -71.075727),
            point("NEC-2276-01", 42.347283, -71.075312),
            point("NEC-2276-02", 42.347196, -71.075299),
            point("NEC-2276-03", 42.347283, -71.075312),
            point("WML-0012-05", 42.34759, -71.075393),
            point("WML-0012-07", 42.34759, -71.075393),
            point("MM-0109", 42.209959, -71.001053),
            point("MM-0109-CS", 42.209961, -71.001031),
            point("MM-0109-S", 42.209964, -71.001088),
            point("ER-0042", 42.397222, -71.04229),
            point("ER-0042-01", 42.397222, -71.04229),
            point("ER-0042-02", 42.397336, -71.042409),
            point("NEC-2237", 42.301085, -71.113551),
            point("NEC-2237-03", 42.301065, -71.113491),
            point("NEC-2237-05", 42.301105, -71.113625),
            point("MM-0023-S", 42.320685, -71.052391),
            point("WR-0045-S", 42.426632, -71.07411),
            point("BNT-0000", 42.366417, -71.062326),
            point("BNT-0000-01", 42.366493, -71.062829),
            point("BNT-0000-02", 42.366493, -71.062829),
            point("BNT-0000-03", 42.366535, -71.06273),
            point("BNT-0000-04", 42.366535, -71.06273),
            point("BNT-0000-05", 42.366618, -71.062601),
            point("BNT-0000-06", 42.366618, -71.062601),
            point("BNT-0000-07", 42.366687, -71.062475),
            point("BNT-0000-08", 42.366687, -71.062475),
            point("BNT-0000-09", 42.366761, -71.062365),
            point("BNT-0000-10", 42.366761, -71.062365),
            point("WR-0053-S", 42.437731, -71.070705),
            point("FR-0034", 42.388401, -71.119148),
            point("FR-0034-01", 42.3884, -71.119149),
            point("FR-0034-02", 42.3884, -71.119149),
            point("MM-0079-S", 42.251809, -71.005409),
            point("NEC-2265", 42.336608, -71.089208),
            point("NEC-2265-01", 42.336339, -71.089517),
            point("NEC-2265-02", 42.337659, -71.087737),
            point("NEC-2265-03", 42.336368, -71.089554),
            point("NEC-2287", 42.35141, -71.055417),
            point("NEC-2287-01", 42.351302, -71.05571),
            point("NEC-2287-02", 42.351261, -71.055552),
            point("NEC-2287-03", 42.351261, -71.055552),
            point("NEC-2287-04", 42.35122, -71.055396),
            point("NEC-2287-05", 42.35122, -71.055396),
            point("NEC-2287-06", 42.351178, -71.055238),
            point("NEC-2287-07", 42.351178, -71.055238),
            point("NEC-2287-08", 42.351136, -71.055081),
            point("NEC-2287-09", 42.351136, -71.055081),
            point("NEC-2287-10", 42.351034, -71.054958),
            point("NEC-2287-11", 42.351034, -71.054958),
            point("NEC-2287-12", 42.350742, -71.05493),
            point("NEC-2287-13", 42.350742, -71.05493),
        )

    @Ignore
    @Test
    fun `print tree info for debugging`() {
        val tree = KdTree(commuterRailPoints)
        println(tree.asGeoJSON())
        println("tree height ${tree.root?.height() ?: 0}")
        println(
            "average depth ${(tree.root?.totalDepth(0) ?: 0).toDouble() / commuterRailPoints.size.toDouble()}"
        )
        println("log₂ tree size ${log2(commuterRailPoints.size.toDouble())}")
    }

    @Test
    fun `performs nearest-neighbor search`() {
        val tree = KdTree(commuterRailPoints)
        val queryPoint = Position(latitude = 42.351370, longitude = -71.066496)
        assertEquals(
            listOf(
                Pair("WML-0012-05", 0.524),
                Pair("WML-0012-07", 0.524),
                Pair("NEC-2276-01", 0.531),
                Pair("NEC-2276-03", 0.531),
                Pair("NEC-2276-02", 0.534),
                Pair("NEC-2276", 0.547),
                Pair("NEC-2287-01", 0.551),
                Pair("NEC-2287-02", 0.559),
                Pair("NEC-2287-03", 0.559),
                Pair("NEC-2287", 0.566),
                Pair("NEC-2287-04", 0.567),
                Pair("NEC-2287-05", 0.567),
                Pair("NEC-2287-06", 0.575),
                Pair("NEC-2287-07", 0.575),
                Pair("NEC-2287-08", 0.583),
                Pair("NEC-2287-09", 0.583),
                Pair("NEC-2287-10", 0.59),
                Pair("NEC-2287-11", 0.59),
                Pair("NEC-2287-12", 0.592),
                Pair("NEC-2287-13", 0.592),
            ),
            tree.findNodesWithin(queryPoint, 1.0.miles).map {
                Pair(it.first, round(it.second.inMiles * 1000) / 1000)
            },
        )
    }

    private fun KdTreeNode.height(): Int =
        max(lowChild?.height() ?: 0, highChild?.height() ?: 0) + 1

    private fun KdTreeNode.totalDepth(currentDepth: Int): Int =
        currentDepth +
            (lowChild?.totalDepth(currentDepth + 1) ?: 0) +
            (highChild?.totalDepth(currentDepth + 1) ?: 0)

    private fun KdTree.asGeoJSON(): String =
        FeatureCollection(
                root
                    ?.asFeatures(
                        BoundingBox(west = -71.858, south = 41.571, east = -70.266, north = 42.807)
                    )
                    .orEmpty()
            )
            .toJson()

    internal fun KdTreeNode.asFeatures(
        boundingBox: BoundingBox
    ): List<Feature<Geometry?, JsonObject?>> {
        val id = ids.first()
        val thisFeature =
            listOf(
                buildFeature {
                    this.id = JsonPrimitive(id)
                    geometry = Point(position)
                    properties = buildJsonObject { put("ids", JsonArray(ids.map(::JsonPrimitive))) }
                },
                buildFeature {
                    this.id = JsonPrimitive("$id-line")
                    geometry =
                        lineStringOf(
                            boundingBox.southwest.butWith(splitAxis, position[splitAxis]),
                            boundingBox.northeast.butWith(splitAxis, position[splitAxis]),
                        )
                },
                buildFeature(boundingBox.toPolygon()) {
                    this.id = JsonPrimitive("$id-box")
                    properties = buildJsonObject {
                        put("fill-opacity", 0.1)
                        put("stroke-opacity", 0)
                    }
                },
            )
        var lowFeatures: List<Feature<Geometry?, JsonObject?>>? = null
        if (lowChild != null) {
            val lowBox =
                BoundingBox(
                    boundingBox.southwest,
                    boundingBox.northeast.butWith(splitAxis, position[splitAxis]),
                )
            lowFeatures = lowChild.asFeatures(lowBox)
        }
        var highFeatures: List<Feature<Geometry?, JsonObject?>>? = null
        if (highChild != null) {
            val highBox =
                BoundingBox(
                    boundingBox.southwest.butWith(splitAxis, position[splitAxis]),
                    boundingBox.northeast,
                )
            highFeatures = highChild.asFeatures(highBox)
        }
        return thisFeature + lowFeatures.orEmpty() + highFeatures.orEmpty()
    }
}
