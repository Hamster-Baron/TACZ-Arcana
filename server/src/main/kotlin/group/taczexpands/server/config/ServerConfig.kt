package group.taczexpands.server.config

import group.taczexpands.common.nbt.GunKeys
import net.minecraftforge.common.ForgeConfigSpec
import java.util.function.Predicate

object ServerConfig {

    val BUILDER: ForgeConfigSpec.Builder = ForgeConfigSpec.Builder()
    var Server_CONFIG: ForgeConfigSpec

    var shouldDestroyBlock: ForgeConfigSpec.ConfigValue<Boolean>
    var blockResistanceTable: ForgeConfigSpec.ConfigValue<List<String>>

    var jammingCheckDelay: ForgeConfigSpec.ConfigValue<Int>
    var jammingEntities: ForgeConfigSpec.ConfigValue<List<String>>


    var radarCheckDelay: ForgeConfigSpec.ConfigValue<Int>

    var rayTraceDistance: ForgeConfigSpec.ConfigValue<Int>

    var defaultLaserState: ForgeConfigSpec.ConfigValue<Boolean>

    var defaultBlockAimWhileReloading: ForgeConfigSpec.ConfigValue<Boolean>

    var returnAmmoAllTypeCreativeAmmoBox: ForgeConfigSpec.ConfigValue<Boolean>

    init {
        BUILDER.push("Bullet Settings")

        BUILDER.comment("Bullets can destroy blocks if a block resistance table is configured")
        shouldDestroyBlock = BUILDER.define<Boolean>("shouldDestroyBlock", true)

        BUILDER.comment("Global block resistance table for bullet (bullet: block_table)")
        blockResistanceTable = BUILDER.defineList<String>("blockResistanceTable", mutableListOf<String>(), Predicate {
            if (it !is String) return@Predicate false

            val line = it.replace(" ", "").split(",")
            if (line.size != 6) return@Predicate false

            val key = line[0]
            val resistance = line[1].toIntOrNull() ?: return@Predicate false
            val shouldDestroyBlock = line[2].toBooleanStrictOrNull() ?: return@Predicate false
            val particle = line[3].toBooleanStrictOrNull() ?: return@Predicate false
            val damage = line[4].toBooleanStrictOrNull() ?: return@Predicate false
            val deflectable = line[5].toBooleanStrictOrNull() ?: return@Predicate false

            return@Predicate true
        })
        BUILDER.pop()

        BUILDER.push("Missile Settings")

        BUILDER.comment("Checks missile jamming every X ticks")
        jammingCheckDelay = BUILDER.define("jammingCheckDelay", 20) {
            it is Int && it >= 0
        }

        BUILDER.comment("Missile jamming entities table")
        jammingEntities = BUILDER.defineList<String>("jammingEntities", mutableListOf<String>(), Predicate {
            if (it !is String) return@Predicate false

            val line = it.replace(" ", "").split(",")
            if (line.size != 3) return@Predicate false

            val entity = line[0]
            val efficiency = line[1].toDoubleOrNull() ?: return@Predicate false
            val sphereRadius = line[2].toDoubleOrNull() ?: return@Predicate false

            return@Predicate true
        })

        BUILDER.comment("Checks missile active radar target every X ticks")
        radarCheckDelay = BUILDER.define("radarCheckDelay", 5) {
            it is Int && it >= 0
        }

        BUILDER.pop()

        BUILDER.push("Other Settings")

        BUILDER.comment("Default max raytrace distance")
        rayTraceDistance = BUILDER.define("rayTraceDistance", 128)

        BUILDER.comment("Enable laser by default")
        defaultLaserState = BUILDER.define("defaultLaserState", false)

        BUILDER.comment("Block aim while reloading by default")
        defaultBlockAimWhileReloading = BUILDER.define("defaultBlockAimWhileReloading", false)

        BUILDER.comment("Whether to return the original ammo when switching ammo types while the ammo source is the All Type Creative Ammo Box")
        returnAmmoAllTypeCreativeAmmoBox = BUILDER.define("returnAmmoAllTypeCreativeAmmoBox", false)

        BUILDER.pop()
        Server_CONFIG = BUILDER.build()

        initBinding()
    }

    fun initBinding() {
        GunKeys.defaultLaserStateDelegate = defaultLaserState::get
    }

}