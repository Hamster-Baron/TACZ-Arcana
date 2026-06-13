package group.taczexpands.server.config

import com.charleskorn.kaml.YamlScalar
import group.taczexpands.server.util.YAML
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

var GLOBAL_VALUES = ValuesConfig()

@Serializable
data class ValuesConfig(val values: MutableMap<String, YamlScalar> = mutableMapOf()) {
    companion object {
        @JvmStatic
        fun clear() {
            GLOBAL_VALUES = ValuesConfig()
        }

        @JvmStatic
        fun appendData(data: ByteArray) {
            try {
                val new = YAML.decodeFromString<ValuesConfig>(String(data, Charsets.UTF_8))
                GLOBAL_VALUES.values.putAll(new.values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}