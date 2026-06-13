package group.taczexpands.server

import com.mojang.brigadier.Command
import com.mojang.brigadier.arguments.*
import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.common.entity.CustomDisplayEntity
import group.taczexpands.common.entity.EntityKineticBulletShared
import group.taczexpands.common.network.s2c.S2CAction
import group.taczexpands.common.network.s2c.S2CShake
import group.taczexpands.common.util.sendModMessage
import group.taczexpands.server.api.GunScriptAPIServer
import group.taczexpands.server.bullet.BulletManager
import group.taczexpands.server.bullet.MissileManager
import group.taczexpands.server.config.*
import group.taczexpands.server.nbt.DataStorage
import group.taczexpands.server.nbt.DataType
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.module.dash.DashManager
import group.taczexpands.server.module.visual_break_progress.BreakProgressManager
import group.taczexpands.server.nbt.PlayerExtrasServer
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.skill.*
import group.taczexpands.server.util.Debug
import group.taczexpands.server.util.ScheduledTask
import group.taczexpands.server.util.YAML
import kotlinx.serialization.encodeToString
import net.minecraft.commands.CommandSourceStack
import net.minecraft.commands.Commands
import net.minecraft.commands.arguments.EntityArgument
import net.minecraft.network.chat.Component
import net.minecraftforge.event.AddReloadListenerEvent
import net.minecraftforge.event.RegisterCommandsEvent
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.TickEvent.ServerTickEvent
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import net.minecraftforge.fml.loading.FMLPaths
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.io.File
import kotlin.jvm.optionals.getOrNull


class TACZExpandsServer {
    companion object {
        lateinit var INSTANCE: TACZExpandsServer
        lateinit var LOGGER: Logger

        var _configPath: File? = null
        val configPath: File
            get() = _configPath!!

        val SCHEDULED_TASKS = mutableListOf<ScheduledTask>()
    }

    var reloadMessageListener: CommandSourceStack? = null
    var reloadErrorCount: Int = 0

    init {
        INSTANCE = this
        LOGGER = TACZExpandsCommon.LOGGER

        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ServerConfig.Server_CONFIG)
        Bus.FORGE.bus().get().register(PlayerListener)
        Bus.FORGE.bus().get().register(this)
        Bus.FORGE.bus().get().register(DashManager)
        MOD_BUS.addListener(::onSetup)

        CustomDisplayManager.init()

        EntityKineticBulletShared.onMissileBulletTickDelegate = MissileManager::onMissileBulletTick
        EntityKineticBulletShared.isMissileDroneDelegate = MissileManager::isMissileDrone

        GunScriptAPIServer.init()
        PlayerExtrasServer.init()

    }

    fun onSetup(event: FMLCommonSetupEvent) {
        NetworkManager.init()
    }

    @SubscribeEvent
    fun onAddReloadListener(event: AddReloadListenerEvent) {
        event.addListener(PackSkillsManager)
        event.addListener(PackValuesManager)
    }

    @SubscribeEvent
    fun onRegisterCommands(event: RegisterCommandsEvent) {
        event.dispatcher.register(Commands.literal("taczexpands")
            .requires { it.hasPermission(2) }
            .then(Commands.literal("dump")
                .executes {
                    Debug.dump()
                    return@executes Command.SINGLE_SUCCESS
                })


            .then(Commands.literal("version")
                .executes {
                    val version = ModList.get().getModContainerById(TACZExpandsCommon.MODID).getOrNull()?.modInfo?.version
                    it.source.sendModMessage(Component.translatable("message.taczexpands.version", if (version == null) "Unknown" else "v$version"))
                    return@executes Command.SINGLE_SUCCESS
                })

            .then(Commands.literal("signal")
                .then(Commands.argument("name", StringArgumentType.string())
                    .then(Commands.argument("duration", IntegerArgumentType.integer())
                        .executes {
                            val name = StringArgumentType.getString(it, "name")
                            val duration = IntegerArgumentType.getInteger(it, "duration")
                            SignalManager.dispatchSignal(name, duration)
                            it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                            return@executes Command.SINGLE_SUCCESS
                        })))

            .then(Commands.literal("score")
                .then(Commands.literal("string")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes {
                                val name = StringArgumentType.getString(it, "name")
                                val storage = DataStorage.get()
                                if (storage.addScore(DataType.STRING, name) == null) {
                                    it.source.sendModMessage(Component.translatable("message.taczexpands.score.existed", name))
                                    return@executes 0
                                }
                                it.source.sendModMessage(Component.translatable("message.taczexpands.score.created", name))
                                return@executes Command.SINGLE_SUCCESS
                            }
                        )
                    )

                    .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes {
                                val name = StringArgumentType.getString(it, "name")
                                val storage = DataStorage.get()
                                if (!storage.removeScore(DataType.STRING, name)) {
                                    it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                    return@executes 0
                                }
                                it.source.sendModMessage(Component.translatable("message.taczexpands.score.removed"))
                                return@executes Command.SINGLE_SUCCESS
                            }
                        )
                    )

                    .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", StringArgumentType.string())
                                    .executes {
                                        val name = StringArgumentType.getString(it, "name")
                                        val player = EntityArgument.getPlayer(it, "player")
                                        val value = StringArgumentType.getString(it, "value")

                                        val storage = DataStorage.get()
                                        val score = storage.getScore(DataType.STRING, name) ?: run {
                                            it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                            return@executes 0
                                        }

                                        score.setValue(player, value)

                                        it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                                        return@executes Command.SINGLE_SUCCESS
                                    }
                                )
                            )
                        )
                    )

                    .then(Commands.literal("get")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes {
                                    val name = StringArgumentType.getString(it, "name")
                                    val player = EntityArgument.getPlayer(it, "player")

                                    val storage = DataStorage.get()
                                    val score = storage.getScore(DataType.STRING, name) ?: run {
                                        it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                        return@executes 0
                                    }

                                    it.source.sendModMessage(Component.translatable("message.taczexpands.score.value",
                                        name,
                                        player.name,
                                        score.getValue(player)))
                                    return@executes Command.SINGLE_SUCCESS
                                }
                            )
                        )
                    )

                    .then(Commands.literal("unset")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes {
                                    val name = StringArgumentType.getString(it, "name")
                                    val player = EntityArgument.getPlayer(it, "player")

                                    val storage = DataStorage.get()
                                    val score = storage.getScore(DataType.STRING, name) ?: run {
                                        it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                        return@executes 0
                                    }

                                    score.removeValue(player)

                                    it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                                    return@executes Command.SINGLE_SUCCESS
                                }
                            )
                        )
                    )
                )

                .then(Commands.literal("float")
                    .then(Commands.literal("create")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes {
                                val name = StringArgumentType.getString(it, "name")
                                val storage = DataStorage.get()
                                if (storage.addScore(DataType.FLOAT, name) == null) {
                                    it.source.sendModMessage(Component.translatable("message.taczexpands.score.existed", name))
                                    return@executes 0
                                }
                                it.source.sendModMessage(Component.translatable("message.taczexpands.score.created", name))
                                return@executes Command.SINGLE_SUCCESS
                            }
                        )
                    )

                    .then(Commands.literal("remove")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .executes {
                                val name = StringArgumentType.getString(it, "name")
                                val storage = DataStorage.get()
                                if (!storage.removeScore(DataType.FLOAT, name)) {
                                    it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                    return@executes 0
                                }
                                it.source.sendModMessage(Component.translatable("message.taczexpands.score.removed"))
                                return@executes Command.SINGLE_SUCCESS
                            }
                        )
                    )

                    .then(Commands.literal("set")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("player", EntityArgument.player())
                                .then(Commands.argument("value", FloatArgumentType.floatArg())
                                    .executes {
                                        val name = StringArgumentType.getString(it, "name")
                                        val player = EntityArgument.getPlayer(it, "player")
                                        val value = FloatArgumentType.getFloat(it, "value")

                                        val storage = DataStorage.get()
                                        val score = storage.getScore(DataType.FLOAT, name) ?: run {
                                            it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                            return@executes 0
                                        }

                                        score.setValue(player, value)

                                        it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                                        return@executes Command.SINGLE_SUCCESS
                                    }
                                )
                            )
                        )
                    )

                    .then(Commands.literal("get")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes {
                                    val name = StringArgumentType.getString(it, "name")
                                    val player = EntityArgument.getPlayer(it, "player")

                                    val storage = DataStorage.get()
                                    val score = storage.getScore(DataType.FLOAT, name) ?: run {
                                        it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                        return@executes 0
                                    }

                                    it.source.sendModMessage(Component.translatable("message.taczexpands.score.value",
                                        name,
                                        player.name,
                                        score.getValue(player).toString()))

                                    return@executes Command.SINGLE_SUCCESS
                                }
                            )
                        )
                    )

                    .then(Commands.literal("unset")
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes {
                                    val name = StringArgumentType.getString(it, "name")
                                    val player = EntityArgument.getPlayer(it, "player")

                                    val storage = DataStorage.get()
                                    val score = storage.getScore(DataType.FLOAT, name) ?: run {
                                        it.source.sendModMessage(Component.translatable("message.taczexpands.score.not_existed", name))
                                        return@executes 0
                                    }

                                    score.removeValue(player)

                                    it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                                    return@executes Command.SINGLE_SUCCESS
                                }
                            )
                        )
                    )
                )
            )

            .then(Commands.literal("reload")
                .executes {
                    reloadMessageListener = it.source
                    reloadErrorCount = 0

                    val loadedSkills = reload()

                    if (loadedSkills >= 0) {
                        it.source.sendModMessage(Component.translatable("message.taczexpands.reloaded_server", loadedSkills.toString()))
                    } else {
                        it.source.sendModMessage(Component.translatable("message.taczexpands.reload_server_unsupported"))
                    }

                    reloadMessageListener = null
                    if (reloadErrorCount > 0) {
                        it.source.sendModMessage(Component.translatable("message.taczexpands.reload_error_count", reloadErrorCount.toString()))
                    }

                    reloadErrorCount = 0

                    return@executes Command.SINGLE_SUCCESS
                })

            .then(Commands.literal("shake")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amplitudeYRot", FloatArgumentType.floatArg())
                        .then(Commands.argument("amplitudeXRot", FloatArgumentType.floatArg())
                            .then(Commands.argument("durationMillis", LongArgumentType.longArg())
                                .then(Commands.argument("frequencyHz", FloatArgumentType.floatArg())
                                    .then(Commands.argument("randomness", FloatArgumentType.floatArg())
                                        .then(Commands.argument("dynamicPhaseOffset", BoolArgumentType.bool())
                                            .executes {
                                                val player = EntityArgument.getPlayer(it, "player")
                                                val amplitudeYRot = FloatArgumentType.getFloat(it, "amplitudeYRot")
                                                val amplitudeXRot = FloatArgumentType.getFloat(it, "amplitudeXRot")
                                                val durationMillis = LongArgumentType.getLong(it, "durationMillis")
                                                val frequencyHz = FloatArgumentType.getFloat(it, "frequencyHz")
                                                val randomness = FloatArgumentType.getFloat(it, "randomness")
                                                val dynamicPhaseOffset = BoolArgumentType.getBool(it, "dynamicPhaseOffset")

                                                if (player == null) {
                                                    it.source.sendModMessage(Component.translatable("message.taczexpands.player_not_found"))
                                                    return@executes 0
                                                }

                                                NetworkManager.sendToPlayer(S2CShake(
                                                    amplitudeYRot,
                                                    amplitudeXRot,
                                                    durationMillis,
                                                    frequencyHz,
                                                    randomness,
                                                    dynamicPhaseOffset,
                                                    null,
                                                    null
                                                ), player)

                                                it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                                                return@executes Command.SINGLE_SUCCESS
                                            }))))))))

            .then(Commands.literal("shake")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.argument("amplitudeYRot", FloatArgumentType.floatArg())
                        .then(Commands.argument("amplitudeXRot", FloatArgumentType.floatArg())
                            .then(Commands.argument("durationMillis", LongArgumentType.longArg())
                                .then(Commands.argument("frequencyHz", FloatArgumentType.floatArg())
                                    .then(Commands.argument("randomness", FloatArgumentType.floatArg())
                                        .then(Commands.argument("dynamicPhaseOffset", BoolArgumentType.bool())
                                            .then(Commands.argument("phaseOffsetYRot", FloatArgumentType.floatArg())
                                                .then(Commands.argument("phaseOffsetXRot", FloatArgumentType.floatArg())
                                                    .executes {
                                                        val player = EntityArgument.getPlayer(it, "player")
                                                        val amplitudeYRot = FloatArgumentType.getFloat(it, "amplitudeYRot")
                                                        val amplitudeXRot = FloatArgumentType.getFloat(it, "amplitudeXRot")
                                                        val durationMillis = LongArgumentType.getLong(it, "durationMillis")
                                                        val frequencyHz = FloatArgumentType.getFloat(it, "frequencyHz")
                                                        val randomness = FloatArgumentType.getFloat(it, "randomness")
                                                        val dynamicPhaseOffset = BoolArgumentType.getBool(it, "dynamicPhaseOffset")
                                                        val phaseOffsetYRot = FloatArgumentType.getFloat(it, "phaseOffsetYRot")
                                                        val phaseOffsetXRot = FloatArgumentType.getFloat(it, "phaseOffsetXRot")

                                                        if (player == null) {
                                                            it.source.sendModMessage(Component.translatable("message.taczexpands.player_not_found"))
                                                            return@executes 0
                                                        }

                                                        NetworkManager.sendToPlayer(S2CShake(
                                                            amplitudeYRot,
                                                            amplitudeXRot,
                                                            durationMillis,
                                                            frequencyHz,
                                                            randomness,
                                                            dynamicPhaseOffset,
                                                            phaseOffsetYRot,
                                                            phaseOffsetXRot
                                                        ), player)

                                                        it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                                                        return@executes Command.SINGLE_SUCCESS
                                                    }))))))))))

            .then(Commands.literal("shake")
                .then(Commands.argument("player", EntityArgument.player())
                    .then(Commands.literal("clear")
                        .executes {
                            val player = EntityArgument.getPlayer(it, "player")
                            if (player == null) {
                                it.source.sendModMessage(Component.translatable("message.taczexpands.player_not_found"))
                                return@executes 0
                            }

                            NetworkManager.sendToPlayer(S2CAction(S2CAction.Action.StopShake), player)
                            it.source.sendModMessage(Component.translatable("message.taczexpands.succeeded"))
                            return@executes Command.SINGLE_SUCCESS
                        })))
        )
    }

    fun reload(): Int {
        try {
            VolatileVariablesConfig.clear()
            SkillManager.clear()
            MissileManager.reloadJammingEntities()
            BulletManager.reloadBlockResistanceTable()

            val loadedSkills = loadPlainSkillData()
            LOGGER.info("Reloaded. (Skills: $loadedSkills)")

            return loadedSkills
        } catch (e: Exception) {
            e.printStackTrace()
            return -1
        }
    }

    private fun loadPlainSkillData(): Int {
        val before = SkillManager.LOADED_SKILLS.values.sumOf { it.size }

        loadPlainDirectory("conditions", 1)
        loadPlainDirectory("actions", 2)
        loadPlainDirectory("skills", 0)

        return SkillManager.LOADED_SKILLS.values.sumOf { it.size } - before
    }

    private fun loadPlainDirectory(name: String, type: Int) {
        val directory = File(configPath, name)
        if (!directory.isDirectory) return

        directory.walkTopDown()
            .filter { it.isFile }
            .filter { it.extension.equals("yml", true) || it.extension.equals("yaml", true) }
            .filterNot { it.name.endsWith(".disabled", true) }
            .sortedBy { it.absolutePath }
            .forEach {
                SkillManager.appendData(type, it.readBytes())
            }
    }

    @SubscribeEvent
    fun setup(event: ServerStartedEvent) {
        val baseConfigDir = FMLPaths.CONFIGDIR.get()
        val configPath = File(baseConfigDir.toFile(), TACZExpandsCommon.MODID)
        if (!configPath.exists()) {
            configPath.mkdirs()

            val volatileVariablesFile = File(configPath, "volatile_variables.yml")
            val defaultVolatileVariables = VolatileVariablesConfig()
            val yamlStr = YAML.encodeToString(defaultVolatileVariables)
            volatileVariablesFile.writeText(yamlStr, Charsets.UTF_8)
        }
        val skillsPath = File(configPath, "skills")
        if (!skillsPath.exists()) {
            skillsPath.mkdirs()

            val exampleFile = File(skillsPath, "example.yaml.disabled")
            val exampleConfig = SkillConfig.EXAMPLE
            val yamlStr = YAML.encodeToString(exampleConfig)
            exampleFile.writeText(yamlStr, Charsets.UTF_8)
        }

        val conditionsPath = File(configPath, "conditions")
        if (!conditionsPath.exists()) {
            conditionsPath.mkdirs()

            val exampleFile = File(conditionsPath, "example.yaml.disabled")
            val exampleConfig = ConditionConfig("test_condition", listOf())
            val yamlStr = YAML.encodeToString(exampleConfig)
            exampleFile.writeText(yamlStr, Charsets.UTF_8)
        }

        val actionsPath = File(configPath, "actions")
        if (!actionsPath.exists()) {
            actionsPath.mkdirs()

            val exampleFile = File(actionsPath, "example.yaml.disabled")
            val exampleConfig = ActionConfig("test_action", listOf())
            val yamlStr = YAML.encodeToString(exampleConfig)
            exampleFile.writeText(yamlStr, Charsets.UTF_8)
        }
        _configPath = configPath
        reload()
    }

    @SubscribeEvent
    fun onPostServerTick(event: ServerTickEvent) {
        try {
            if (event.phase != TickEvent.Phase.END) return
            SCHEDULED_TASKS.indices.reversed().forEach {
                val task = SCHEDULED_TASKS[it]
                if (try {
                        task.tick()
                    } catch (e: Exception) {
                        e.printStackTrace()
                        true
                    }) {
                    SCHEDULED_TASKS.removeAt(it)
                }

            }

            SkillManager.onServerTick()
            ActionManager.onServerTick()
            SignalManager.onServerTick()
            ParticleEmitterManager.onServerTick()
            BreakProgressManager.onServerTick()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SubscribeEvent
    fun onPreServerTick(event: ServerTickEvent) {
        try {
            if (event.phase != TickEvent.Phase.START) return
            HookManager.onServerTick()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
