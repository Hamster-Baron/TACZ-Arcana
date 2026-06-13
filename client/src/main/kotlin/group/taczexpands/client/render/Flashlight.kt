package group.taczexpands.client.render

import com.mojang.blaze3d.pipeline.TextureTarget
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.tacz.guns.client.model.bedrock.BedrockPart
import com.tacz.guns.init.ModItems
import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.mixin.accessor.IAccessorPostChain
import group.taczexpands.client.util.RenderHelper
import group.taczexpands.common.TACZExpandsCommon
import group.taczexpands.common.accessor.IAccessorAttachmentData
import group.taczexpands.common.nbt.GunExtras
import group.taczexpands.common.util.perspectiveDiv
import net.minecraft.client.Camera
import net.minecraft.client.Minecraft
import net.minecraft.client.Screenshot
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
import kotlin.math.abs

class FlashlightData(val isFirstPerson: Boolean, val entity: LivingEntity, val depthRenderTarget: TextureCache, val range: Float, val angle: Float, val luminance: Float, val worldToFlashlightCamera: Matrix4f, val flashlightCameraToClip: Matrix4f, val camera: Camera = Camera()) {
    val firstPersonFlashlightToMainCamera: Matrix4f = Matrix4f()
    val firstPersonMainCameraToClip: Matrix4f = Matrix4f()
    var firstPersonCameraWorldPosition: Vector3f = Vector3f()
    var firstPersonCameraWorldRotation = Vector2f()

    fun prepareRenderTarget() {
        val window = Minecraft.getInstance().window
        if (depthRenderTarget.textureTarget.width != window.width || depthRenderTarget.textureTarget.height != window.height) {
            depthRenderTarget.textureTarget.resize(window.width, window.height, Minecraft.ON_OSX)
        }
        depthRenderTarget.textureTarget.clear(Minecraft.ON_OSX)
    }

    fun updateFirstPerson(worldToMainCamera: Matrix4f, mainCameraToClip: Matrix4f) {

        val camera = Minecraft.getInstance().gameRenderer.mainCamera
        val invFirstPersonModelViewMat = Matrix4f(firstPersonFlashlightToMainCamera).invert()
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
}

class TextureCache() {
    private var _textureTarget: TextureTarget? = null
    val textureTarget: TextureTarget
        get() {
            if (_textureTarget == null)
                _textureTarget = TextureTarget(Minecraft.getInstance().window.width, Minecraft.getInstance().window.height, true, Minecraft.ON_OSX)
            return _textureTarget!!
        }

    var occupied = false
}

object Flashlight {
    var postChain: PostChain? = null
    var width: Int = 0
    var height: Int = 0

    var firstPersonFlashlightData: FlashlightData? = null
    val flashlights = mutableListOf<FlashlightData>()
    val firstPersonDepthCache by lazy { TextureCache().also { it.occupied = true } }
    val depthCache by lazy { List<TextureCache>(8) { TextureCache() } }
    val mainDepthBackup by lazy {
        TextureTarget(Minecraft.getInstance().window.width, Minecraft.getInstance().window.height, true, Minecraft.ON_OSX)
    }

    fun init() {
        val mc = Minecraft.getInstance()
        postChain = PostChain(mc.textureManager, mc.resourceManager, mc.mainRenderTarget,
            ResourceLocation(TACZExpandsCommon.MODID, "shaders/post/flashlight.json"))
        postChain!!.resize(mc.window.width, mc.window.height)
        width = mc.window.width
        height = mc.window.height
    }

    fun reset() {
        postChain = null
        firstPersonFlashlightData = null
        flashlights.clear()
    }

    fun getRenderTarget(): TextureCache? {
        val result = depthCache.firstOrNull { !it.occupied } ?: return null
        result.occupied = true
        return result
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
            if (data != null && data.enable && GunExtras.getFlashlight(mainHand)) {
                val buffer = getRenderTarget() ?: return
                flashlights.add(FlashlightData(false, entity, buffer, data.range, data.angle, data.luminance, Matrix4f(), Matrix4f()))
            }
        }
    }


    fun onRenderLevel(partialTicks: Float, modelViewStack: PoseStack) {


        val player = Minecraft.getInstance().player ?: return

        val cameraEntity = Minecraft.getInstance().cameraEntity
        val valid = if (cameraEntity != null && cameraEntity is LivingEntity) {
            val data = IAccessorAttachmentData.getFlashlight(cameraEntity.mainHandItem)
            if (data != null && data.enable && GunExtras.getFlashlight(cameraEntity.mainHandItem)) {
                if (Minecraft.getInstance().options.cameraType.isFirstPerson) {
                    true
                } else false
            } else false
        } else false

        if (!valid) {
            firstPersonFlashlightData = null
        }

        val firstPerson = firstPersonFlashlightData

        if (firstPerson == null) {
            add(player)
        }
        val nearby = player.level().getEntities(player, player.boundingBox.inflate(128.0)) { it is LivingEntity }.sortedBy { player.distanceToSqr(it) }
        nearby.forEach { if (it is LivingEntity) add(it) }

        if (postChain == null) init()
        val postChain = postChain ?: return
        val window = Minecraft.getInstance().window
        if (window.width != width || window.height != height) {
            postChain.resize(window.width, window.height)
            width = window.width
            height = window.height
        }

        val mainCamera = Minecraft.getInstance().gameRenderer.mainCamera
        val mainCameraToClip = RenderSystem.getProjectionMatrix()
        val worldToMainCamera = Matrix4f(modelViewStack.last().pose()).mul(RenderSystem.getModelViewMatrix())


        val clipToMainCamera = Matrix4f(mainCameraToClip).invert()
        val mainCameraToWorld = Matrix4f(worldToMainCamera).invert()


        val data = if (firstPerson != null) {
            firstPerson.updateFirstPerson(worldToMainCamera, mainCameraToClip)
            flashlights + firstPerson
        } else flashlights

        for (flashlight in data) {
            Depth.call(flashlight)
        }
        for (index in data.indices) {
            val flashlight = data[index]
            val hasNext = index + 1 < flashlights.size
            if (Depth.outputScreenshot) {
                Screenshot.grab(Minecraft.getInstance().gameDirectory, flashlight.depthRenderTarget.textureTarget) {}
            }

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


            val target = postChain.getTempTarget("flashlight")
            RenderHelper.fitStencil(flashlight.depthRenderTarget.textureTarget, target)
            target.copyDepthFrom(flashlight.depthRenderTarget.textureTarget)
            for (pass in (postChain as IAccessorPostChain).passes) {
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
                pass.getEffect().safeGetUniform("ShadowPixelStep").set(1.0f / target.width.toFloat(), 1.0f / target.height.toFloat())
                pass.getEffect().safeGetUniform("From").set(Vector3f(from.x, from.y, from.z))
                pass.getEffect().safeGetUniform("To").set(Vector3f(to.x, to.y, to.z))
                pass.getEffect().safeGetUniform("Angle").set(Math.toRadians(flashlight.angle.toDouble()).toFloat())
                pass.getEffect().safeGetUniform("Range").set(flashlight.range + abs(nearZ))
                pass.getEffect().safeGetUniform("Luminance").set(flashlight.luminance)
                pass.getEffect().safeGetUniform("MinZ").set(0.0f)
            }


            val window = Minecraft.getInstance().window
            if (mainDepthBackup.width != window.width || mainDepthBackup.height != window.height) {
                mainDepthBackup.resize(window.width, window.height, Minecraft.ON_OSX)
            }

            mainDepthBackup.clear(Minecraft.ON_OSX)
            RenderHelper.fitStencil(mainDepthBackup, Minecraft.getInstance().mainRenderTarget)
            mainDepthBackup.copyDepthFrom(Minecraft.getInstance().mainRenderTarget)
            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            Minecraft.getInstance().mainRenderTarget.bindRead()

            postChain.process(partialTicks)

            Minecraft.getInstance().mainRenderTarget.copyDepthFrom(mainDepthBackup)

            Minecraft.getInstance().mainRenderTarget.bindWrite(false)
            Minecraft.getInstance().mainRenderTarget.bindRead()
        }
        Depth.outputScreenshot = false
        afterRender()

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
                    it.firstPersonMainCameraToClip.set(RenderSystem.getProjectionMatrix())
                }
            }
        }

        poseStack.popPose()
    }
}