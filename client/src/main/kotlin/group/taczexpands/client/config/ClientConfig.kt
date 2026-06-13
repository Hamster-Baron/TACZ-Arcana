package group.taczexpands.client.config

import net.minecraftforge.common.ForgeConfigSpec


object ClientConfig {

    val BUILDER: ForgeConfigSpec.Builder = ForgeConfigSpec.Builder()
    var CLIENT_CONFIG: ForgeConfigSpec

    var enableRenderFunc: ForgeConfigSpec.BooleanValue
    var enableAdvancedRenderingByDefault: ForgeConfigSpec.BooleanValue
    var hudAmmoFadeOut: ForgeConfigSpec.IntValue
    var enableAdvancedBeamRendering: ForgeConfigSpec.BooleanValue
    var showOverheatBarByDefault: ForgeConfigSpec.BooleanValue


    var enableModsCompatibilityCheck: ForgeConfigSpec.BooleanValue

    init {
        BUILDER.push("Visual Settings")

        BUILDER.comment("Enable custom rendering functions")
        enableRenderFunc = BUILDER.define("enableRenderFunc", true)

        BUILDER.comment("Enable advanced rendering functions by default (attachment: advanced_rendering)")
        enableAdvancedRenderingByDefault = BUILDER.define("enableAdvancedRenderingByDefault", false)

        BUILDER.comment("Ammo HUD Fadeout Delay (ms)")
        hudAmmoFadeOut = BUILDER.defineInRange("hudAmmoFadeOut", 0, Int.MIN_VALUE, Int.MAX_VALUE)

        BUILDER.comment("Enable advanced beam rendering function")
        enableAdvancedBeamRendering = BUILDER.define("enableAdvancedBeamRendering", true)

        BUILDER.comment("Show overheat bar by default (gun: show_overheat_bar)")
        showOverheatBarByDefault = BUILDER.define("showOverheatBarByDefault", true)

        BUILDER.pop()


        BUILDER.push("Other Settings")
        BUILDER.comment("Show compatibility message on join")
        enableModsCompatibilityCheck = BUILDER.define("enableModsCompatibilityCheck", true)
        BUILDER.pop()




        CLIENT_CONFIG = BUILDER.build()
    }

}