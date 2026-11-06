package group.taczexpands.client

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.DefaultVertexFormat
import com.mojang.blaze3d.vertex.VertexFormat
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.client.particle.AmmoParticleSpawner
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.init.ModItems
import group.taczexpands.client.api.GunScriptAPIClient
import group.taczexpands.client.compat.CompatHelper
import group.taczexpands.client.compat.RendererCapabilities
import group.taczexpands.client.compat.RendererPresence
import group.taczexpands.client.compat.iris.IrisPaintPbr
import group.taczexpands.client.compat.iris.IrisSurfacePbr
import group.taczexpands.client.compat.iris.IrisSurfacePipeline
import group.taczexpands.client.config.ClientConfig
import group.taczexpands.client.config.RuntimeConfig
import group.taczexpands.client.entity.CustomDisplayEntityRenderer
import group.taczexpands.client.entity.CustomDisplayManager
import group.taczexpands.client.gui.*
import group.taczexpands.client.input.CameraManager
import group.taczexpands.client.input.RemoteCameraSession
import group.taczexpands.client.input.ViewStateManager
import group.taczexpands.client.input.InputManager
import group.taczexpands.client.input.KeyInputs
import group.taczexpands.client.input.ShakeManager
import group.taczexpands.client.input.rotation.RotationController
import group.taczexpands.client.network.s2c.S2CRotationStateHandler
import group.taczexpands.common.network.RotationNetwork
import group.taczexpands.client.mixin.accessor.IAccessorCamera
import group.taczexpands.client.nbt.PlayerExtrasClient
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.client.override.BeamRendererOverride
import group.taczexpands.client.render.Flashlight
import group.taczexpands.client.render.surface.SurfaceRenderer
import group.taczexpands.client.render.surface.SurfaceResources
import group.taczexpands.client.render.surface.SurfaceBackend
import group.taczexpands.client.render.surface.SurfaceEngine
import group.taczexpands.client.render.paint.PaintResources
import group.taczexpands.client.render.particle.SnowstormParticles
import group.taczexpands.client.render.HookManager
import group.taczexpands.client.sound.DynamicSoundManager
import group.taczexpands.client.util.Rotation
import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.common.accessor.IAccessorAttachmentData
import group.taczexpands.common.accessor.IAccessorBulletData
import group.taczexpands.common.data.GuidanceType
import group.taczexpands.common.entity.EntityKineticBulletShared
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.network.c2s.C2SActionKey
import group.taczexpands.common.util.sendModMessage
import net.minecraft.Util
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.InventoryScreen
import net.minecraft.client.renderer.RenderStateShard
import net.minecraft.client.renderer.RenderStateShard.TextureStateShard
import net.minecraft.client.renderer.RenderStateShard.TransparencyStateShard
import net.minecraft.client.renderer.RenderType
import net.minecraft.client.renderer.RenderType.CompositeState
import net.minecraft.client.renderer.ShaderInstance
import net.minecraft.client.renderer.entity.EntityRenderers
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.packs.resources.PreparableReloadListener
import net.minecraft.server.packs.resources.ResourceManager
import net.minecraft.util.Mth
import net.minecraft.util.profiling.ProfilerFiller
import net.minecraft.world.phys.Vec3
import net.minecraftforge.api.distmarker.Dist
import net.minecraftforge.client.event.*
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.TickEvent.RenderTickEvent
import net.minecraftforge.event.entity.EntityTravelToDimensionEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.eventbus.api.EventPriority
import net.minecraftforge.fml.DistExecutor
import net.minecraftforge.fml.ModList
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent
import org.slf4j.Logger
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import java.io.File
import java.io.IOException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executor
import java.util.function.Function
import java.util.function.Supplier
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.sqrt


class TACZExpandsClient {
    companion object {
        lateinit var INSTANCE: TACZExpandsClient
        lateinit var LOGGER: Logger

        var nextSwitchDraw: Boolean = false


        val rotations = mutableListOf<Rotation>()

        // True only while rendering the independent scope world view.
        var patchMainTargetDraw = false
        var mainTarget: RenderTarget? = null


        var _configPath: File? = null
        val configPath: File
            get() = _configPath!!

        var _blitScreenScopeShader: ShaderInstance? = null
        var _thermalImagingShaderArmor: ShaderInstance? = null
        var _thermalImagingShaderEntity: ShaderInstance? = null
        var _thermalImagingShaderEntityTranslucent: ShaderInstance? = null
        var _CursorShader: ShaderInstance? = null

        fun getBlitScreenScopeShader(): ShaderInstance {
            if (_blitScreenScopeShader == null) {
                _blitScreenScopeShader = ShaderInstance(
                    Minecraft.getInstance().resourceManager,
                    ResourceLocation(TACZExpandsCommon.MODID, "blit_screen_scope"),
                    DefaultVertexFormat.BLIT_SCREEN
                )
            }
            return _blitScreenScopeShader!!
        }

        fun getThermalImagingShaderArmor(): ShaderInstance {
            if (_thermalImagingShaderArmor == null) {
                _thermalImagingShaderArmor = ShaderInstance(
                    Minecraft.getInstance().resourceManager,
                    ResourceLocation(TACZExpandsCommon.MODID, "rendertype_armor_cutout_no_cull"),
                    DefaultVertexFormat.NEW_ENTITY
                )
            }
            return _thermalImagingShaderArmor!!
        }

        fun getThermalImagingShaderEntity(): ShaderInstance {
            if (_thermalImagingShaderEntity == null) {
                _thermalImagingShaderEntity = ShaderInstance(
                    Minecraft.getInstance().resourceManager,
                    ResourceLocation(TACZExpandsCommon.MODID, "rendertype_entity_cutout_no_cull"),
                    DefaultVertexFormat.NEW_ENTITY
                )
            }
            return _thermalImagingShaderEntity!!
        }

        fun getThermalImagingShaderEntityTranslucent(): ShaderInstance {
            if (_thermalImagingShaderEntityTranslucent == null) {
                _thermalImagingShaderEntityTranslucent = ShaderInstance(
                    Minecraft.getInstance().resourceManager,
                    ResourceLocation(TACZExpandsCommon.MODID, "rendertype_entity_translucent"),
                    DefaultVertexFormat.NEW_ENTITY
                )
            }
            return _thermalImagingShaderEntityTranslucent!!
        }

        fun getCursorShader(): ShaderInstance {
            if (_CursorShader == null) {
                _CursorShader = ShaderInstance(
                    Minecraft.getInstance().resourceManager,
                    ResourceLocation(TACZExpandsCommon.MODID, "rendertype_cursor"),
                    DefaultVertexFormat.NEW_ENTITY
                )
            }
            return _CursorShader!!
        }

        fun isAdvancedRendering(): Boolean {
            if (!ClientConfig.enableRenderFunc.get()) return false

            val player = Minecraft.getInstance().player ?: return false
            val gunItem = ModItems.MODERN_KINETIC_GUN.get()
            val mainHand = player.mainHandItem
            if (mainHand.item !== gunItem) return false
            val scope = IAccessorAttachmentData.getExtraHolder(
                mainHand,
                AttachmentType.SCOPE
            )
            if (scope == null) return false
            return scope.advancedRendering ?: ClientConfig.enableAdvancedRenderingByDefault.get()
        }

        @JvmOverloads
        fun isMonochrome(bypassPhase: Boolean = false): Boolean {
            if (!ClientConfig.enableRenderFunc.get()) return false
            if (patchMainTargetDraw || bypassPhase) {
                val player = Minecraft.getInstance().player ?: return false
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                val mainHand = player.mainHandItem
                if (mainHand.item !== gunItem) return false
                return GunExtras.getOverrideMonochrome(mainHand) ?: return IAccessorAttachmentData.getExtraHolder(
                    mainHand,
                    AttachmentType.SCOPE
                )?.monochrome ?: return false
            }
            return false
        }


        @JvmOverloads
        fun shouldUseThermalImaging(bypassPhase: Boolean = false): Boolean {
            if (SurfaceRenderer.cameraPass()) return SurfaceRenderer.vision().equals("thermal")
            if (!ClientConfig.enableRenderFunc.get()) return false
            if (patchMainTargetDraw || bypassPhase) {
                val player = Minecraft.getInstance().player ?: return false
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                val mainHand = player.mainHandItem
                if (mainHand.item !== gunItem) return false
                return GunExtras.getOverrideThermalImaging(mainHand) ?: return IAccessorAttachmentData.getExtraHolder(
                    mainHand,
                    AttachmentType.SCOPE
                )?.thermalImaging ?: return false
            }
            return false
        }

        @JvmOverloads
        fun shouldUseNightVision(bypassPhase: Boolean = false): Boolean {
            if (SurfaceRenderer.cameraPass()) return SurfaceRenderer.vision().equals("night")
            if (!ClientConfig.enableRenderFunc.get()) return false
            if (patchMainTargetDraw || bypassPhase) {
                val player = Minecraft.getInstance().player ?: return false
                val gunItem = ModItems.MODERN_KINETIC_GUN.get()
                val mainHand = player.mainHandItem
                if (mainHand.item !== gunItem) return false
                return GunExtras.getOverrideNightVision(mainHand) ?: return IAccessorAttachmentData.getExtraHolder(
                    mainHand,
                    AttachmentType.SCOPE
                )?.nightVision ?: return false
            }
            return false
        }

        val CURSOR: Function<ResourceLocation, RenderType> = Util.memoize { p_286173_: ResourceLocation? ->
            val `rendertype$compositestate` = CompositeState.builder()
                .setShaderState(RenderStateShard.ShaderStateShard(::getCursorShader))
                .setTextureState(TextureStateShard(p_286173_, false, false))
                .setTransparencyState(TransparencyStateShard("no_transparency", {
                    RenderSystem.disableBlend()
                }, {})).setLightmapState(RenderStateShard.LightmapStateShard(true))
                .setOverlayState(RenderStateShard.OverlayStateShard(true)).createCompositeState(true)
            RenderType.create(
                "cursor",
                DefaultVertexFormat.NEW_ENTITY,
                VertexFormat.Mode.QUADS,
                256,
                true,
                false,
                `rendertype$compositestate`
            )
        }
    }

    init {
        INSTANCE = this
        LOGGER = TACZExpandsCommon.LOGGER
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, ClientConfig.CLIENT_CONFIG)
        Bus.FORGE.bus().get().register(this)
        MOD_BUS.addListener(::onSetup)
        MOD_BUS.addListener(::onRegisterKey)
        MOD_BUS.addListener(::onRegisterResourceReload)
        MOD_BUS.addListener(::onClientSetup)
        MOD_BUS.addListener(::onRegisterGuiOverlaysEvent)

        Bus.FORGE.bus().get().register(KeyInputs)
        Bus.FORGE.bus().get().register(Flashlight)
        Bus.FORGE.bus().get().register(ItemPreviewManager)
        Bus.FORGE.bus().get().register(CameraManager)
        Bus.FORGE.bus().get().register(ViewStateManager)

        CustomDisplayManager.init()


        EntityKineticBulletShared.onMissileAmmoParticleSpawnDelegate = {
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, Supplier { Runnable { AmmoParticleSpawner.addParticle(it) } })
        }

        GunScriptAPIClient.init()
        PlayerExtrasClient.init()
        RotationNetwork.registerClientHandler(S2CRotationStateHandler::handle)
        RotationController.initialize()
        if (RendererPresence.OCULUS_PRESENT) {
            val capabilities = RendererCapabilities.current()
            SurfaceEngine.installBackend(if (capabilities.irisViews()) IrisSurfacePipeline.INSTANCE
                else SurfaceBackend.unavailable(capabilities.cameraFailure()))
            if (capabilities.paintPbr()) IrisPaintPbr.register()
            if (capabilities.surfacePbr()) IrisSurfacePbr.register()
        }
        SurfaceClient.initialize()
        PaintClient.initialize()

    }

    fun onRegisterGuiOverlaysEvent(event: RegisterGuiOverlaysEvent) {
        event.registerAbove(VanillaGuiOverlay.HELMET.id(), "camera", TVOverlay)
        event.registerAbove(VanillaGuiOverlay.HELMET.id(), "custom_hud", CustomHUDOverlay)
        event.registerAbove(VanillaGuiOverlay.FROSTBITE.id(), "frostbite", FrostbiteOverlay)
        event.registerAbove(VanillaGuiOverlay.PORTAL.id(), "flash", FlashOverlay)
    }

    fun vecToYawPitch(direction: Vec3): Pair<Float, Float> {
        val x = direction.x
        val y = direction.y
        val z = direction.z

        val horizontalLength = sqrt(x * x + z * z)

        if (horizontalLength < 0.001) {
            val pitchDeg = if (y > 0) -90.0f else 90.0f
            return Pair(0.0f, pitchDeg)
        }

        val yawRad = atan2(-x, z)

        val pitchRad = asin(y / sqrt(x * x + y * y + z * z))

        val pitchDeg = Math.toDegrees(pitchRad.toDouble()).toFloat()
        var yawDeg = Math.toDegrees(yawRad.toDouble()).toFloat()

        if (yawDeg > 180.0f) {
            yawDeg -= 360.0f
        } else if (yawDeg < -180.0f) {
            yawDeg += 360.0f
        }

        return Pair(yawDeg, -pitchDeg)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onCameraSetup(event: ViewportEvent.ComputeCameraAngles) {
        val entity = event.camera.entity
        if (entity is EntityKineticBullet) {
            InputManager.updateClamp()
            if (!entity.entityData.get(EntityKineticBulletShared.TV_ROTATION_LOCK_DATA_ACCESSOR)) {
                event.yaw = InputManager.yaw
                event.pitch = InputManager.pitch
            } else {
                val partialTicks = Minecraft.getInstance().partialTick
                val missileYaw = -(Mth.lerp(partialTicks, entity.yRotO, entity.yRot))
                val missilePitch = -(Mth.lerp(partialTicks, entity.xRotO, entity.xRot))

                event.yaw = missileYaw
                event.pitch = missilePitch
            }
            (event.camera as IAccessorCamera).setRotation(event.yaw, event.pitch)
        }
    }

    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.END) return
        DynamicSoundManager.onClientTick()
        SnowstormParticles.tick()

        if (Minecraft.getInstance().player == null) return

        BeamRendererOverride.reset()

        InputManager.onClientTick()
        ShakeManager.onClientTick()

        FrostbiteOverlay.onClientTick()

        ScopeManager.onClientTick()
        RemoteCameraSession.tick()
    }

    @SubscribeEvent
    fun onChangeDimension(event: EntityTravelToDimensionEvent) {
        HookManager.reset()
        CameraManager.reset()
        if (event.entity == Minecraft.getInstance().player) {
            RemoteCameraSession.close(false)
            InputManager.clear()
            ViewStateManager.invalidateSample()
        }
    }

    @SubscribeEvent
    fun onRenderTick(event: RenderTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        val now = System.currentTimeMillis()
        ShakeManager.onRenderTick()
        rotations.removeIf { !it.update(now) }
    }

    @SubscribeEvent
    fun onRenderEntity(event: RenderLivingEvent.Post<*, *>) {
        HookManager.render(event.entity, event.partialTick, event.poseStack, event.multiBufferSource)
    }

    @SubscribeEvent
    fun onRender(event: RenderLevelStageEvent) {
        if (event.stage != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return
        val mc = Minecraft.getInstance()
        if (mc.options.cameraType.isFirstPerson) {
            val player = mc.player ?: return
            if (player == mc.cameraEntity) {
                val pPartialTick = event.partialTick
                val pEntity = player
                val pCamPos = event.camera.position
                val d0 = Mth.lerp(pPartialTick.toDouble(), pEntity.xOld, pEntity.getX())
                val d1 = Mth.lerp(pPartialTick.toDouble(), pEntity.yOld, pEntity.getY())
                val d2 = Mth.lerp(pPartialTick.toDouble(), pEntity.zOld, pEntity.getZ())

                val renderer = mc.entityRenderDispatcher.getRenderer(pEntity)
                val renderOffset = renderer.getRenderOffset(pEntity, pPartialTick)
                val pX = d0 - pCamPos.x
                val pY = d1 - pCamPos.y
                val pZ = d2 - pCamPos.z
                val pPoseStack = event.poseStack
                val buffer = mc.renderBuffers().bufferSource()

                val x: Double = pX + renderOffset.x()
                val y: Double = pY + renderOffset.y()
                val z: Double = pZ + renderOffset.z()
                pPoseStack.pushPose()
                pPoseStack.translate(x, y, z)
                HookManager.render(pEntity, pPartialTick, pPoseStack, buffer)
                buffer.endLastBatch()
                buffer.endBatch(RenderType.leash())
                pPoseStack.popPose()
            }
        }
    }


    @SubscribeEvent
    fun onLeaveServer(event: ClientPlayerNetworkEvent.LoggingOut) {
        VariableManager.reset()
        FlashOverlay.reset()
        Flashlight.reset()
        GunContextManager.reset()
        ScopeManager.reset()
        ShakeManager.reset()
        rotations.clear()
        HookManager.reset()
        CameraManager.reset(false)
        RemoteCameraSession.close(false)
        InputManager.clear()
        ViewStateManager.reset()
        PlayerExtrasClient.reset()
        DynamicSoundManager.stopAll()
        CustomHUDOverlay.reset()
        RuntimeConfig.reset()
        SnowstormParticles.clear()
    }


    @SubscribeEvent
    fun onJoinServer(event: ClientPlayerNetworkEvent.LoggingIn) {
        if (!ClientConfig.enableRenderFunc.get())
            event.player.sendModMessage(Component.translatable("message.taczexpands.advanced_rendering_notify"))

        if (ClientConfig.enableModsCompatibilityCheck.get()) {
            if (CompatHelper.hasTACZTweaksV3()) {
                event.player.sendModMessage(Component.translatable("message.taczexpands.incompatible.downgrade", "tacztweaks", "< 3.0"))
            }
        }
    }

    @SubscribeEvent
    fun onScreen(event: ScreenEvent.Opening) {
        if (KeyInputs.inventoryCancelTicks > 0) {
            if (event.screen is InventoryScreen) {
                event.isCanceled = true
            }
        }
    }



    fun onSetup(event: FMLCommonSetupEvent) {
        NetworkManager.init()
    }

    fun onClientSetup(event: FMLClientSetupEvent) {
        event.enqueueWork {
            EntityRenderers.register(TACZExpandsCommon.ENTITYTYPE_CUSTOM_DISPLAY.get(), ::CustomDisplayEntityRenderer)
        }
    }

    fun onRegisterKey(event: RegisterKeyMappingsEvent) {
        KeyInputs.init(event)
    }

    fun onRegisterResourceReload(event: RegisterClientReloadListenersEvent) {
        SurfaceResources.registerReloadListeners(event)
        PaintResources.registerReloadListeners(event)
        event.registerReloadListener(object : PreparableReloadListener {
            override fun reload(
                stage: PreparableReloadListener.PreparationBarrier,
                p1: ResourceManager,
                p2: ProfilerFiller,
                p3: ProfilerFiller,
                p4: Executor,
                gameExecutor: Executor
            ): CompletableFuture<Void> {
                return CompletableFuture.runAsync({
                    RenderSystem.recordRenderCall {
                        Flashlight.reset()
                        DynamicSoundManager.clearSoundResourceCache()
                        if (_blitScreenScopeShader != null) {
                            _blitScreenScopeShader!!.close()
                            _blitScreenScopeShader = null
                        }
                        if (_thermalImagingShaderArmor != null) {
                            _thermalImagingShaderArmor!!.close()
                            _thermalImagingShaderArmor = null
                        }
                        if (_thermalImagingShaderEntity != null) {
                            _thermalImagingShaderEntity!!.close()
                            _thermalImagingShaderEntity = null
                        }
                        if (_thermalImagingShaderEntityTranslucent != null) {
                            _thermalImagingShaderEntityTranslucent!!.close()
                            _thermalImagingShaderEntityTranslucent = null
                        }

                        if (_CursorShader != null) {
                            _CursorShader!!.close()
                            _CursorShader = null
                        }
                        try {
                            _blitScreenScopeShader = ShaderInstance(
                                Minecraft.getInstance().resourceManager,
                                ResourceLocation(TACZExpandsCommon.MODID, "blit_screen_scope"),
                                DefaultVertexFormat.BLIT_SCREEN
                            )
                            _thermalImagingShaderArmor = ShaderInstance(
                                Minecraft.getInstance().resourceManager,
                                ResourceLocation(TACZExpandsCommon.MODID, "rendertype_armor_cutout_no_cull"),
                                DefaultVertexFormat.NEW_ENTITY
                            )
                            _thermalImagingShaderEntity = ShaderInstance(
                                Minecraft.getInstance().resourceManager,
                                ResourceLocation(TACZExpandsCommon.MODID, "rendertype_entity_cutout_no_cull"),
                                DefaultVertexFormat.NEW_ENTITY
                            )

                            _thermalImagingShaderEntityTranslucent = ShaderInstance(
                                Minecraft.getInstance().resourceManager,
                                ResourceLocation(TACZExpandsCommon.MODID, "rendertype_entity_translucent"),
                                DefaultVertexFormat.NEW_ENTITY
                            )
                            _CursorShader = ShaderInstance(
                                Minecraft.getInstance().resourceManager,
                                ResourceLocation(TACZExpandsCommon.MODID, "rendertype_cursor"),
                                DefaultVertexFormat.NEW_ENTITY
                            )

                        } catch (e: IOException) {
                            LOGGER.error("Failed to load Arcana shader resources", e)
                        }
                    }
                }, gameExecutor).thenCompose(stage::wait)
            }

        })
    }



}
