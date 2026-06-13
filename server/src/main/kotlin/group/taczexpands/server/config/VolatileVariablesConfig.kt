package group.taczexpands.server.config

import group.taczexpands.server.util.YAML
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString

var GLOBAL_VOLATILE_VARIABLES = VolatileVariablesConfig()

@Serializable
data class VolatileVariablesConfig(val variables: MutableList<String> = mutableListOf("default")) {
    companion object {
        @JvmStatic
        fun clear() {
            GLOBAL_VOLATILE_VARIABLES = VolatileVariablesConfig()
        }

        @JvmStatic
        fun appendData(data: ByteArray) {
            try {
                val new = YAML.decodeFromString<VolatileVariablesConfig>(String(data, Charsets.UTF_8))
                GLOBAL_VOLATILE_VARIABLES.variables.addAll(new.variables)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}