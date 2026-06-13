package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.checkContains
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import net.minecraftforge.fml.ModList
import org.apache.maven.artifact.versioning.VersionRange

@Serializable
@SerialName("HasMod")
data class HasMod(val modId: String, val range: String? = null, val useRegex: Boolean = false) : Condition {

    @Transient
    val versionRange = range?.let { VersionRange.createFromVersionSpec(it) }

    companion object {
        val EXAMPLE = HasMod("tacz", "[1.0, )")
    }

    override fun check(context: Context): Boolean {
        return ModList.get().mods.any { mod ->
            mod.modId.checkContains(modId, useRegex) && (versionRange == null || versionRange.containsVersion(mod.version))
        }
    }
}