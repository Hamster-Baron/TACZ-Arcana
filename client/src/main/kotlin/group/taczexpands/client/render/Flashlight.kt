package group.taczexpands.client.render

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.math.Axis
import com.tacz.guns.client.model.bedrock.BedrockPart
import com.tacz.guns.init.ModItems
import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.accessor.IAccessorGameRenderer
import group.taczexpands.client.compat.RendererCapabilities
import group.taczexpands.client.compat.iris.IrisTerrainBindings
import group.taczexpands.client.mixin.accessor.IAccessorCamera
import group.taczexpands.client.mixin.accessor.IAccessorPostChain
import group.taczexpands.client.render.flashlight.FlashlightDepthState
import group.taczexpands.client.render.flashlight.FlashlightDepthTarget
import group.taczexpands.client.render.flashlight.FlashlightViews
import group.taczexpands.client.render.surface.SurfaceEngine
import group.taczexpands.client.render.surface.SurfaceFeed
import group.taczexpands.client.util.RenderHelper
import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.common.accessor.IAccessorAttachmentData
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.util.perspectiveDiv
import kotlin.math.abs
import net.minecraft.client.Camera
import net.minecraft.client.CameraType
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.PostChain
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemDisplayContext
import net.minecraft.world.item.ItemStack
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector2f
import org.joml.Vector3f
import org.joml.Vector4f

class FlashlightData(val isFirstPerson: Boolean, val entity: LivingEntity, val depthRenderTarget: TextureCache, val range: Float, val angle: Float, val luminance: Float, val worldToFlashlightCamera: Matrix4f, val flashlightCameraToClip: Matrix4f, val camera: Camera = Camera()) {
    val firstPersonFlashlightToMainCamera: Matrix4f = Matrix4f()
    val firstPersonMainCameraToClip: Matrix4f = Matrix4f()
    var firstPersonCameraWorldPosition: Vector3f = Vector3f()
    var firstPersonCameraWorldRotation = Vector2f()

    fun prepareRenderTarget() {
        depthRenderTarget.textureTarget.clear(Minecraft.ON_OSX)
    }

    fun updateFirstPerson(worldToMainCamera: Matrix4f, mainCameraToClip: Matrix4f) {

        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val mainCameraToWorld = Matrix4f(worldToMainCamera).invert()
        val clipToMainCamera = Matrix4f(mainCameraToClip).invert()
        val posCenterModel = Vector4f(0.0f, 0.0f, 0.0f, 1.0f)
        val posCenterModelInWorldMainCameraOffset = posCenterModel.mul(firstPersonFlashlightToMainCamera).mul(firstPersonMainCameraToClip)
        val from = posCenterModelInWorldMainCameraOffset
        from.mul(clipToMainCamera)
        from.perspectiveDiv()
        from.mul(mainCameraToWorld)
        firstPersonCameraWorldPosition.set(camera.position.x + from.x, camera.position.y + from.y, camera.position.z + from.z)

        val to = Vector4f(0.0f, 0.0f, -1.0f, 1.0f).mul(firstPersonFlashlightToMainCamera).mul(firstPersonMainCameraToClip)
        to.mul(clipToMainCamera)
        to.perspectiveDiv()
        to.mul(mainCameraToWorld)
        val dir = Vector3f(to.x - from.x, to.y - from.y, to.z - from.z)
        dir.normalize()
        val (yaw, pitch) = TACZExpandsClient.INSTANCE.vecToYawPitch(Vec3(dir.x.toDouble(), dir.y.toDouble(), dir.z.toDouble()))
        firstPersonCameraWorldRotation.set(yaw, pitch)

    }

    fun updateCamera(partialTick: Float) {
        val access = camera as IAccessorCamera
        val eyeHeight = if (isFirstPerson) 0.0f else entity.eyeHeight
        access.setEyeHeight(eyeHeight)
        access.setEyeHeightOld(eyeHeight)
        camera.setup(entity.level(), entity, false, false, partialTick)
        if (isFirstPerson) {
            access.setPosition(Vec3(firstPersonCameraWorldPosition.x.toDouble(),
                firstPersonCameraWorldPosition.y.toDouble(), firstPersonCameraWorldPosition.z.toDouble()))
            access.setRotation(firstPersonCameraWorldRotation.x, firstPersonCameraWorldRotation.y)
        } else {
            access.move(0.5, 0.0, 0.0)
        }
    }
}

class TextureCache() {
    private var _textureTarget: TextureTarget? = null
    val textureTarget: TextureTarget
        get() {
            if (_textureTarget == null)
                _textureTarget = FlashlightDepthTarget(FlashlightDepthTarget.SHADOW_SIZE, FlashlightDepthTarget.SHADOW_SIZE)
            return _textureTarget!!
        }

    var occupied = false

    fun release() {
        _textureTarget?.let {
            ShaderTextureReferences.clear(it.depthTextureId)
            it.destroyBuffers()
        }
        _textureTarget = null
        occupied = false
    }
}

private fun releaseCompositionChain(chain: PostChain) {
    val buffer = chain.getTempTarget("swap")
    ShaderTextureReferences.clear(buffer.colorTextureId)
    ShaderTextureReferences.clear(buffer.depthTextureId)
    chain.close()
}

/** One output-sized composition chain; depth preparation remains owned by the main view. */
class FlashlightComposition(val target: RenderTarget) : AutoCloseable {
    val chain: PostChain
    val depthBackup: TextureTarget = FlashlightDepthTarget.backup(target.width, target.height)
    init {
        val mc = Minecraft.getInstance()
        chain = PostChain(mc.textureManager, mc.resourceManager, target,
            ResourceLocation(TACZExpandsCommon.MODID, "shaders/post/flashlight.json"))
        chain.resize(target.width, target.height)
    }
    override fun close() {
        releaseCompositionChain(chain)
        ShaderTextureReferences.clear(depthBackup.colorTextureId)
        ShaderTextureReferences.clear(depthBackup.depthTextureId)
        depthBackup.destroyBuffers()
    }
}

object Flashlight {
    var postChain: PostChain? = null
    var width: Int = 0
    var height: Int = 0

    var firstPersonFlashlightData: FlashlightData? = null
    val flashlights = mutableListOf<FlashlightData>()
    private var prepared: List<FlashlightData> = emptyList()
    private var preparedLevel: ClientLevel? = null
    private var preparedFrame = -1L
    private var frameViews: FlashlightViews? = null
    private val frameProjection = Matrix4f()
    private val previousLens = Matrix4f()
    private val previousView = Matrix4f()
    private var previousViewReady = false
    private val mainProjection = Matrix4f()
    private val mainView = Matrix4f()
    private var mainViewReady = false
    private var candidateLevel: ClientLevel? = null
    private var candidateTick = -1L
    private var candidates: List<LivingEntity> = emptyList()
    val firstPersonDepthCache by lazy { TextureCache().also { it.occupied = true } }
    val depthCache by lazy { List<TextureCache>(8) { TextureCache() } }
    private var backup: TextureTarget? = null
    val mainDepthBackup: TextureTarget
        get() = backup ?: FlashlightDepthTarget.backup(Minecraft.getInstance().window.width,
            Minecraft.getInstance().window.height).also { backup = it }

    fun init() {
        val mc = Minecraft.getInstance()
        postChain = PostChain(mc.textureManager, mc.resourceManager, mc.mainRenderTarget,
            ResourceLocation(TACZExpandsCommon.MODID, "shaders/post/flashlight.json"))
        postChain!!.resize(mc.window.width, mc.window.height)
        width = mc.window.width
        height = mc.window.height
    }

    fun reset() {
        postChain?.let(::releaseCompositionChain)
        postChain = null
        firstPersonFlashlightData = null
        flashlights.clear()
        prepared = emptyList()
        preparedLevel = null
        preparedFrame = -1L
        frameViews = null
        previousViewReady = false
        mainViewReady = false
        candidateLevel = null
        candidates = emptyList()
        firstPersonDepthCache.release()
        depthCache.forEach { it.release() }
        backup?.let {
            ShaderTextureReferences.clear(it.depthTextureId)
            it.destroyBuffers()
        }
        backup = null
    }

    fun afterRender() {
        flashlights.removeIf {
            it.depthRenderTarget.occupied = false
            return@removeIf true
        }
    }

    fun add(entity: LivingEntity) {
        if (entity == Minecraft.getInstance().player && Minecraft.getInstance().options.cameraType.isFirstPerson) {
            return
        }

        val mainHand = entity.mainHandItem
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (mainHand.item != gunItem) return
        if (GunExtras.getFlashlight(mainHand)) {
            val data = IAccessorAttachmentData.getFlashlight(mainHand)
            if (data != null && data.enable) {
                val buffer = depthCache.firstOrNull { !it.occupied } ?: return
                val light = FlashlightData(false, entity, buffer, data.range, data.angle, data.luminance, Matrix4f(), Matrix4f())
                light.updateCamera(Minecraft.getInstance().partialTick)
                if (frameViews?.affects(light) != true) return
                buffer.occupied = true
                flashlights.add(light)
            }
        }
    }

    fun onRenderLevel(partialTicks: Float, modelViewStack: PoseStack) {
        if (!TACZExpandsClient.patchMainTargetDraw) {
            mainProjection.set(RenderSystem.getProjectionMatrix())
            mainView.set(modelViewStack.last().pose())
            mainViewReady = true
        }
        if (!hasPreparedLights()) return
        val mc = Minecraft.getInstance()
        if (postChain == null) init()
        val chain = postChain ?: return
        if (width != mc.window.width || height != mc.window.height) {
            chain.resize(mc.window.width, mc.window.height)
            width = mc.window.width
            height = mc.window.height
        }
        composite(partialTicks, modelViewStack, mc.mainRenderTarget, chain, mainDepthBackup)
    }

    fun beginFrame(partialTick: Float, feeds: List<SurfaceFeed>) {
        val mc = Minecraft.getInstance()
        val level = mc.level ?: return
        val entity = mc.cameraEntity ?: return
        if (preparedLevel !== level) {
            previousViewReady = false
            mainViewReady = false
        }
        // Pair the preceding main view's effects with its recorded hand locator.
        val lensEffects = if (previousViewReady && mainViewReady)
            Matrix4f(previousLens).invert().mul(mainProjection) else Matrix4f()
        val viewEffects = if (previousViewReady && mainViewReady)
            Matrix4f(mainView).mul(Matrix4f(previousView).invert()) else Matrix4f()
        val camera = mc.gameRenderer.mainCamera
        camera.setup(level, entity, !mc.options.cameraType.isFirstPerson,
            mc.options.cameraType == CameraType.THIRD_PERSON_FRONT, partialTick)
        val pose = PoseStack()
        pose.mulPose(Axis.XP.rotationDegrees(camera.xRot))
        pose.mulPose(Axis.YP.rotationDegrees(camera.yRot + 180))
        val fov = (mc.gameRenderer as IAccessorGameRenderer).`taczexpands$getFOV`(camera, partialTick, true)
        previousLens.set(mc.gameRenderer.getProjectionMatrix(fov))
        previousView.set(pose.last().pose())
        previousViewReady = true
        frameProjection.set(previousLens).mul(lensEffects)
        pose.last().pose().set(viewEffects.mul(previousView))
        if (RendererCapabilities.current().terrainBindings() && IrisTerrainBindings.cameraUnavailableReason() != null) {
            preparedLevel = level
            prepared = emptyList()
            return
        }
        frameViews = FlashlightViews(camera, frameProjection, pose.last().pose(), feeds)
        prepare(pose)
    }

    fun prepare(modelViewStack: PoseStack) {
        val mc = Minecraft.getInstance()
        if (preparedFrame == SurfaceEngine.frame() && preparedLevel === mc.level) return
        preparedFrame = SurfaceEngine.frame()
        preparedLevel = mc.level
        afterRender()
        prepared = emptyList()

        val player = Minecraft.getInstance().player ?: return

        val cameraEntity = Minecraft.getInstance().cameraEntity
        val valid = if (cameraEntity is LivingEntity) {
            val data = if (GunExtras.getFlashlight(cameraEntity.mainHandItem))
                IAccessorAttachmentData.getFlashlight(cameraEntity.mainHandItem) else null
            data != null && data.enable && mc.options.cameraType.isFirstPerson
        } else false

        if (!valid) {
            firstPersonFlashlightData = null
        }

        val firstPerson = firstPersonFlashlightData

        val level = mc.level ?: return
        if (candidateLevel !== level || candidateTick != level.gameTime) {
            candidateLevel = level
            candidateTick = level.gameTime
            candidates = level.getEntitiesOfClass(LivingEntity::class.java, player.boundingBox.inflate(128.0)) {
                it.mainHandItem.item == ModItems.MODERN_KINETIC_GUN.get() && GunExtras.getFlashlight(it.mainHandItem)
            }
        }
        if (candidates.isNotEmpty()) {
            candidates.sortedBy { mc.gameRenderer.mainCamera.position.distanceToSqr(it.position()) }.forEach(::add)
        }
        val data = if (firstPerson != null) {
            firstPerson.updateFirstPerson(modelViewStack.last().pose(), frameProjection)
            firstPerson.updateCamera(mc.partialTick)
            if (frameViews?.affects(firstPerson) == true) flashlights + firstPerson else flashlights
        } else flashlights

        for (flashlight in data) {
            Depth.call(flashlight)
        }
        prepared = data.toList()
    }

    fun hasPreparedLights(): Boolean = prepared.isNotEmpty() && preparedLevel === Minecraft.getInstance().level

    fun composite(partialTicks: Float, modelViewStack: PoseStack, output: RenderTarget,
                  postChain: PostChain, depthBackup: TextureTarget) {
        if (!hasPreparedLights()) return
        val mainCamera = Minecraft.getInstance().gameRenderer.mainCamera
        val worldToMainCamera = Matrix4f(modelViewStack.last().pose()).mul(RenderSystem.getModelViewMatrix())
        val clipToMainCamera = Matrix4f(RenderSystem.getProjectionMatrix()).invert()
        val mainCameraToWorld = Matrix4f(worldToMainCamera).invert()
        val data = prepared
        for (index in data.indices) {
            val flashlight = data[index]

            val worldToFlashlightCamera = flashlight.worldToFlashlightCamera
            val flashlightCameraToClip = flashlight.flashlightCameraToClip

            val flashlightCameraToWorld = Matrix4f(worldToFlashlightCamera).invert()
            val clipToFlashlightCamera = Matrix4f(flashlightCameraToClip).invert()

            val level = Minecraft.getInstance().level ?: return

            val nearZ = 0.0f
            val from = Vector4f(0.0f, 0.0f, nearZ, 1.0f).mul(flashlightCameraToWorld)
            from.add(flashlight.camera.position.x.toFloat(), flashlight.camera.position.y.toFloat(), flashlight.camera.position.z.toFloat(), 0.0f)
            from.sub(mainCamera.position.x.toFloat(), mainCamera.position.y.toFloat(), mainCamera.position.z.toFloat(), 0.0f)
            from.mul(worldToMainCamera)

            val to = Vector4f(0.0f, 0.0f, nearZ - flashlight.range, 1.0f).mul(flashlightCameraToWorld)
            to.add(flashlight.camera.position.x.toFloat(), flashlight.camera.position.y.toFloat(), flashlight.camera.position.z.toFloat(), 0.0f)
            to.sub(mainCamera.position.x.toFloat(), mainCamera.position.y.toFloat(), mainCamera.position.z.toFloat(), 0.0f)
            to.mul(worldToMainCamera)

            val depthSource = flashlight.depthRenderTarget.textureTarget
            for (pass in (postChain as IAccessorPostChain).passes) {
                pass.effect.setSampler("FlashlightDepthSampler") { depthSource.depthTextureId }
                pass.getEffect().safeGetUniform("InvProjMat").set(clipToMainCamera)
                pass.getEffect().safeGetUniform("InvModelViewMat").set(Matrix4f(RenderSystem.getModelViewMatrix()).invert())
                pass.getEffect().safeGetUniform("MainCameraToWorldMat").set(mainCameraToWorld)
                pass.getEffect().safeGetUniform("FlashlightProjMat").set(flashlight.flashlightCameraToClip)
                pass.getEffect().safeGetUniform("FlashlightModelViewMat").set(flashlight.worldToFlashlightCamera)
                pass.getEffect()
                    .safeGetUniform("WorldOffset")
                    .set(Vector3f(mainCamera.position.x.toFloat() - flashlight.camera.position.x.toFloat(),
                        mainCamera.position.y.toFloat() - flashlight.camera.position.y.toFloat(),
                        mainCamera.position.z.toFloat() - flashlight.camera.position.z.toFloat()))
                pass.getEffect().safeGetUniform("ShadowPixelStep").set(
                    1.0f / depthSource.width, 1.0f / depthSource.height)
                pass.getEffect().safeGetUniform("From").set(Vector3f(from.x, from.y, from.z))
                pass.getEffect().safeGetUniform("To").set(Vector3f(to.x, to.y, to.z))
                pass.getEffect().safeGetUniform("Angle").set(Math.toRadians(flashlight.angle.toDouble()).toFloat())
                pass.getEffect().safeGetUniform("Range").set(flashlight.range + abs(nearZ))
                pass.getEffect().safeGetUniform("Luminance").set(flashlight.luminance)
                pass.getEffect().safeGetUniform("MinZ").set(0.0f)
            }

            if (depthBackup.width != output.width || depthBackup.height != output.height) {
                depthBackup.resize(output.width, output.height, Minecraft.ON_OSX)
            }
            depthBackup.clear(Minecraft.ON_OSX)
            RenderHelper.fitStencil(depthBackup, output)
            depthBackup.copyDepthFrom(output)
            output.bindWrite(true)
            output.bindRead()
            postChain.process(partialTicks)
            output.copyDepthFrom(depthBackup)
            output.bindWrite(true)
            output.bindRead()
        }

    }

    fun renderHand(gunItem: ItemStack?, renderItem: ItemStack?, poseStack: PoseStack, transformType: ItemDisplayContext, path: List<BedrockPart>) {
        gunItem ?: return
        renderItem ?: return
        if (!transformType.firstPerson()) return

        poseStack.pushPose()
        for (bedrockPart in path) {
            bedrockPart.translateAndRotateAndScale(poseStack)
        }

        val pose = poseStack.last().pose()
        val cameraEntity = Minecraft.getInstance().cameraEntity
        if (cameraEntity != null && cameraEntity is LivingEntity) {
            val data = IAccessorAttachmentData.getFlashlight(cameraEntity.mainHandItem)
            if (data != null && data.enable && GunExtras.getFlashlight(cameraEntity.mainHandItem)) {
                firstPersonFlashlightData = FlashlightData(true,
                    cameraEntity,
                    firstPersonDepthCache,
                    data.range,
                    data.angle,
                    data.luminance,
                    Matrix4f(),
                    Matrix4f()).also {
                    it.firstPersonFlashlightToMainCamera.set(pose).also { it.mul(RenderSystem.getModelViewMatrix()) }
                    it.firstPersonMainCameraToClip.set(FlashlightDepthState.handProjection())
                }
            }
        }

        poseStack.popPose()
    }
}
