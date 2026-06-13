package group.taczexpands.client.config

import com.tacz.guns.config.PreLoadModConfig
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.config.ConfigTracker
import net.minecraftforge.fml.config.IConfigEvent
import net.minecraftforge.fml.config.ModConfig
import java.nio.file.Path

object PreLoadConfig {

    private val spec: ForgeConfigSpec
    var enableDefaultStateMachineReplacement: ForgeConfigSpec.BooleanValue

    init {
        val builder = ForgeConfigSpec.Builder()
        builder.push("Other Settings")

        builder.comment("Replace the tacz default pack's default state machine lua script")
        enableDefaultStateMachineReplacement = builder.define("enableDefaultStateMachineReplacement", false)

        builder.pop()
        spec = builder.build()
    }

    val modConfig: PreLoadModConfig
        get() {
            val ctx = ModLoadingContext.get()
            val c = PreLoadModConfig(ModConfig.Type.CLIENT, spec, ctx.getActiveContainer(), "taczexpands-pre.toml")
            ConfigTracker.INSTANCE.configSets().get(ModConfig.Type.CLIENT)!!.remove(c)
            ConfigTracker.INSTANCE.fileMap().remove(c.getFileName(), c)
            return c
        }

    fun load(configBasePath: Path) {
        if (spec.isLoaded()) return
        val config = modConfig
        val configData = config.getHandler().reader(configBasePath).apply(config)
        config.setConfigData(configData)
        config.fireEvent(IConfigEvent.loading(config))
        config.save()
    }

}