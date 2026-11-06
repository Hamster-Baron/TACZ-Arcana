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
    var coplanarFaceSeparation: ForgeConfigSpec.DoubleValue
    var coplanarFaceSeparationNamespaces: ForgeConfigSpec.ConfigValue<List<String>>


    var enableModsCompatibilityCheck: ForgeConfigSpec.BooleanValue

    private var defaultFovControl: ForgeConfigSpec.BooleanValue

    fun shouldSkipDefaultModelFov(): Boolean = enableRenderFunc.get() &&
        enableAdvancedRenderingByDefault.get() && !defaultFovControl.get()

    init {
        BUILDER.push("Visual Settings")

        BUILDER.comment("Enable custom rendering functions")
        enableRenderFunc = BUILDER.define("enableRenderFunc", true)

        BUILDER.comment("Enable advanced rendering functions by default (attachment: advanced_rendering)")
        enableAdvancedRenderingByDefault = BUILDER.define("enableAdvancedRenderingByDefault", false)
        defaultFovControl = BUILDER.comment("Apply model FOV scaling to scopes enabled by the default advanced rendering setting.")
            .define("defaultFovControl", true)

        BUILDER.comment("Ammo HUD Fadeout Delay (ms)")
        hudAmmoFadeOut = BUILDER.defineInRange("hudAmmoFadeOut", 0, Int.MIN_VALUE, Int.MAX_VALUE)

        BUILDER.comment("Enable advanced beam rendering function")
        enableAdvancedBeamRendering = BUILDER.define("enableAdvancedBeamRendering", true)

        BUILDER.comment("Show overheat bar by default (gun: show_overheat_bar)")
        showOverheatBarByDefault = BUILDER.define("showOverheatBarByDefault", true)

        coplanarFaceSeparation = BUILDER.comment(
            "Depth gap in model pixels lifting overlapping flush faces of gun, attachment and other TacZ models,",
            "so textures such as camouflage do not flicker where parts share a surface. 0 keeps models unchanged.",
            "Takes effect on the next resource reload."
        ).defineInRange("coplanarFaceSeparation", 0.002, 0.0, 0.1)

        coplanarFaceSeparationNamespaces = BUILDER.comment(
            "Model resource namespaces eligible for coplanar face separation (exact matches, no wildcards).",
            "Only official TacZ models are enabled by default. An empty list disables separation for all models.",
            "Uses the model ID namespace, not the gun ID or pack folder. Takes effect on the next resource reload."
        ).defineListAllowEmpty("coplanarFaceSeparationNamespaces", listOf("tacz")) {
            it is String && it.matches(Regex("[a-z0-9_.-]+"))
        }

        BUILDER.pop()


        BUILDER.push("Other Settings")
        BUILDER.comment("Show compatibility message on join")
        enableModsCompatibilityCheck = BUILDER.define("enableModsCompatibilityCheck", true)
        BUILDER.pop()




        ScopeShaderConfig.register(BUILDER)
        PaintUiConfig.register(BUILDER)
        CLIENT_CONFIG = BUILDER.build()
    }

}
