package group.taczexpands.client.render

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import com.mojang.blaze3d.vertex.VertexConsumer
import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.client.gui.ScopeManager
import group.taczexpands.client.mixin.accessor.IAccessorEntityRenderer
import group.taczexpands.client.network.NetworkManager
import group.taczexpands.common.accessor.IAccessorBullet
import group.taczexpands.common.data.HookData
import group.taczexpands.common.data.HookInstance
import group.taczexpands.common.network.c2s.C2SActionKey
import net.minecraft.client.Minecraft
import net.minecraft.client.multiplayer.ClientLevel
import net.minecraft.client.renderer.LightTexture
import net.minecraft.client.renderer.MultiBufferSource
import net.minecraft.client.renderer.RenderType
import net.minecraft.core.BlockPos
import net.minecraft.util.Mth
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.level.LightLayer
import net.minecraft.world.phys.Vec3
import org.joml.Matrix4f
import org.joml.Vector3f
import org.joml.Vector4f
import java.util.concurrent.CopyOnWriteArrayList

class HookEntity(val from: LivingEntity, val to: LivingEntity, val data: HookData) {}
class HookBlock(val from: LivingEntity, val to: Vec3, val data: HookData) {}
object HookManager {
    val listHookEntity = CopyOnWriteArrayList<HookEntity>()
    val listHookBlock = CopyOnWriteArrayList<HookBlock>()

    fun render(entity: LivingEntity, partialTick: Float, poseStack: PoseStack, multiBufferSource: MultiBufferSource) {
        listHookEntity.forEach {
            if (it.from != entity) return@forEach
            renderLeashHolderBase(it.to, partialTick, poseStack, multiBufferSource, it.from)

        }
        listHookBlock.forEach {
            if (it.from != entity) return@forEach
            renderLeashHolderBase(it.to, partialTick, poseStack, multiBufferSource, it.from)
        }
    }

    fun processHook(hook: HookInstance) {
        val level = Minecraft.getInstance().level ?: return
        when (hook.type) {
            HookInstance.Type.ATTACH_ENTITY -> {
                val from = level.getEntity(hook.fromEntityId) as? LivingEntity ?: return
                val to = level.getEntity(hook.toEntityId) as? LivingEntity ?: return
                listHookEntity.add(HookEntity(from, to, hook.data))
            }

            HookInstance.Type.DETACH_ENTITY -> {
                listHookEntity.removeIf { it.from.id == hook.fromEntityId && it.to.id == hook.toEntityId }
            }

            HookInstance.Type.ATTACH_BLOCK -> {
                val from = level.getEntity(hook.fromEntityId) as? LivingEntity ?: return
                listHookBlock.add(HookBlock(from, hook.toPos, hook.data))
            }

            HookInstance.Type.DETACH_BLOCK -> {
                listHookBlock.removeIf { it.from.id == hook.fromEntityId && it.to == hook.toPos }
            }
        }
    }

    fun tryRelease() {
        NetworkManager.sendToServer(C2SActionKey(C2SActionKey.Action.RELEASE_HOOK))
    }

    fun reset() {
        listHookEntity.clear()
        listHookBlock.clear()
    }

    enum class Source {
        FROM,
        TO
    }

    fun predicateHookData(block: (List<Pair<Source, HookData>>) -> Unit) {
        val player = Minecraft.getInstance().player
        if (player == null) {
            block(listOf())
            return
        }
        val level = player.level() as? ClientLevel
        if (level == null) {
            block(listOf())
            return
        }

        val finalList = level.entitiesForRendering().mapNotNull {
            if (it !is EntityKineticBullet) return@mapNotNull null
            if (it.owner != player) return@mapNotNull null
            it as IAccessorBullet
            if (!it.`taczexpands$isHook`()) return@mapNotNull null
            Source.FROM to it.`taczexpands$getHookData`()
        } + listHookEntity.mapNotNull {
            if (it.from == player) Source.FROM to it.data
            if (it.to == player) Source.TO to it.data
            else null
        } + listHookBlock.mapNotNull {
            if (it.from != player) return@mapNotNull null
            Source.FROM to it.data
        }

        block(finalList)
    }

    fun renderLeashHolderBase(vec: Vec3, partialTicks: Float, poseStack: PoseStack, pBuffer: MultiBufferSource, pLeashHolder: LivingEntity) {
        poseStack.pushPose()
        val holdPos = if (pLeashHolder == Minecraft.getInstance().player && Minecraft.getInstance().options.cameraType.isFirstPerson) {
            val invProj = Matrix4f(RenderSystem.getProjectionMatrix()).invert()
            val invModelView = Matrix4f(RenderSystem.getInverseViewRotationMatrix())
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            val pos = Vector4f(ScopeManager.lastHookPosNDC).mulProject(invProj)
                .mul(invModelView)
                .add(camera.position.x.toFloat(), camera.position.y.toFloat(), camera.position.z.toFloat(), 0.0f)
            Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        } else {
            pLeashHolder.getRopeHoldPosition(partialTicks)
        }

        val holderPos = pLeashHolder.getPosition(partialTicks)
        val holdOffset = Vec3(holdPos.x, holdPos.y, holdPos.z).subtract(holderPos)
        poseStack.translate(holdOffset.x, holdOffset.y, holdOffset.z)

        val entityPos = vec

        val f = (entityPos.x - holderPos.x - holdOffset.x).toFloat()
        val f1 = (entityPos.y - holderPos.y - holdOffset.y).toFloat()
        val f2 = (entityPos.z - holderPos.z - holdOffset.z).toFloat()
        val f3 = 0.025f
        val vertexconsumer = pBuffer.getBuffer(RenderType.leash())
        val matrix4f = poseStack.last().pose()
        val f4 = Mth.invSqrt(f * f + f2 * f2) * f3 / 2.0f
        val f5 = f2 * f4
        val f6 = f * f4
        val blockpos = BlockPos.containing(entityPos)
        val blockpos1 = BlockPos.containing(pLeashHolder.getEyePosition(partialTicks))
        val j: Int = pLeashHolder.level().getBrightness(LightLayer.BLOCK, blockpos)
        val i: Int = (Minecraft.getInstance().entityRenderDispatcher.getRenderer(pLeashHolder) as IAccessorEntityRenderer).getBlockLightLevel(pLeashHolder,
            blockpos1)
        val l: Int = pLeashHolder.level().getBrightness(LightLayer.SKY, blockpos)
        val k: Int = pLeashHolder.level().getBrightness(LightLayer.SKY, blockpos1)

        for (i1 in 0..24) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025f, 0.025f, f5, f6, i1, false)
        }

        for (j1 in 24 downTo 0) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025f, 0.0f, f5, f6, j1, true)
        }

        poseStack.popPose()
    }

    fun renderLeashHolderBase(entity: Entity, partialTicks: Float, poseStack: PoseStack, pBuffer: MultiBufferSource, pLeashHolder: LivingEntity) {
        poseStack.pushPose()
        val holdPos = if (pLeashHolder == Minecraft.getInstance().player && Minecraft.getInstance().options.cameraType.isFirstPerson) {
            val invProj = Matrix4f(RenderSystem.getProjectionMatrix()).invert()
            val invModelView = Matrix4f(RenderSystem.getInverseViewRotationMatrix())
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            val pos = Vector4f(ScopeManager.lastHookPosNDC).mulProject(invProj)
                .mul(invModelView)
                .add(camera.position.x.toFloat(), camera.position.y.toFloat(), camera.position.z.toFloat(), 0.0f)
            Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        } else {
            pLeashHolder.getRopeHoldPosition(partialTicks)
        }

        val holderPos = pLeashHolder.getPosition(partialTicks)
        val holdOffset = Vec3(holdPos.x, holdPos.y, holdPos.z).subtract(holderPos)
        poseStack.translate(holdOffset.x, holdOffset.y, holdOffset.z)

        val entityPos = entity.getEyePosition(partialTicks)

        val f = (entityPos.x - holderPos.x - holdOffset.x).toFloat()
        val f1 = (entityPos.y - holderPos.y - holdOffset.y).toFloat()
        val f2 = (entityPos.z - holderPos.z - holdOffset.z).toFloat()
        val f3 = 0.025f
        val vertexconsumer = pBuffer.getBuffer(RenderType.leash())
        val matrix4f = poseStack.last().pose()
        val f4 = Mth.invSqrt(f * f + f2 * f2) * f3 / 2.0f
        val f5 = f2 * f4
        val f6 = f * f4
        val blockpos = BlockPos.containing(entity.getEyePosition(partialTicks))
        val blockpos1 = BlockPos.containing(pLeashHolder.getEyePosition(partialTicks))
        val j: Int = (Minecraft.getInstance().entityRenderDispatcher.getRenderer(entity) as IAccessorEntityRenderer).getBlockLightLevel(entity, blockpos)
        val i: Int = (Minecraft.getInstance().entityRenderDispatcher.getRenderer(pLeashHolder) as IAccessorEntityRenderer).getBlockLightLevel(pLeashHolder,
            blockpos1)
        val l: Int = pLeashHolder.level().getBrightness(LightLayer.SKY, blockpos)
        val k: Int = pLeashHolder.level().getBrightness(LightLayer.SKY, blockpos1)

        for (i1 in 0..24) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025f, 0.025f, f5, f6, i1, false)
        }

        for (j1 in 24 downTo 0) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025f, 0.0f, f5, f6, j1, true)
        }

        poseStack.popPose()
    }

    fun renderLeash(entity: Entity, offset: Vector3f, partialTicks: Float, poseStack: PoseStack, pBuffer: MultiBufferSource, pLeashHolder: LivingEntity) {
        poseStack.pushPose()
        val holdPos = if (pLeashHolder == Minecraft.getInstance().player && Minecraft.getInstance().options.cameraType.isFirstPerson) {
            val invProj = Matrix4f(RenderSystem.getProjectionMatrix()).invert()
            val invModelView = Matrix4f(RenderSystem.getInverseViewRotationMatrix())
            val camera = Minecraft.getInstance().gameRenderer.mainCamera
            val pos = Vector4f(ScopeManager.lastHookPosNDC).mulProject(invProj)
                .mul(invModelView)
                .add(camera.position.x.toFloat(), camera.position.y.toFloat(), camera.position.z.toFloat(), 0.0f)
            Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())
        } else {
            pLeashHolder.getRopeHoldPosition(partialTicks)
        }

        poseStack.translate(offset.x, offset.y, offset.z)

        val entityPos = entity.getPosition(partialTicks)

        val f = (holdPos.x - offset.x - entityPos.x).toFloat()
        val f1 = (holdPos.y - offset.y - entityPos.y).toFloat()
        val f2 = (holdPos.z - offset.z - entityPos.z).toFloat()
        val f3 = 0.025f
        val vertexconsumer = pBuffer.getBuffer(RenderType.leash())
        val matrix4f = poseStack.last().pose()
        val f4 = Mth.invSqrt(f * f + f2 * f2) * f3 / 2.0f
        val f5 = f2 * f4
        val f6 = f * f4
        val blockpos = BlockPos.containing(entity.getEyePosition(partialTicks))
        val blockpos1 = BlockPos.containing(pLeashHolder.getEyePosition(partialTicks))
        val i: Int = this.getBlockLightLevel(entity, blockpos)
        val j: Int = (Minecraft.getInstance().entityRenderDispatcher.getRenderer(pLeashHolder) as IAccessorEntityRenderer).getBlockLightLevel(pLeashHolder,
            blockpos1)
        val k: Int = entity.level().getBrightness(LightLayer.SKY, blockpos)
        val l: Int = entity.level().getBrightness(LightLayer.SKY, blockpos1)

        for (i1 in 0..24) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025f, 0.025f, f5, f6, i1, false)
        }

        for (j1 in 24 downTo 0) {
            addVertexPair(vertexconsumer, matrix4f, f, f1, f2, i, j, k, l, 0.025f, 0.0f, f5, f6, j1, true)
        }

        poseStack.popPose()
    }

    fun getBlockLightLevel(pEntity: Entity, pPos: BlockPos): Int {
        return if (pEntity.isOnFire()) 15 else pEntity.level().getBrightness(LightLayer.BLOCK, pPos)
    }

    private fun addVertexPair(pConsumer: VertexConsumer, pMatrix: Matrix4f, p_174310_: Float, p_174311_: Float, p_174312_: Float, pEntityBlockLightLevel: Int, pLeashHolderBlockLightLevel: Int, pEntitySkyLightLevel: Int, pLeashHolderSkyLightLevel: Int, p_174317_: Float, p_174318_: Float, p_174319_: Float, p_174320_: Float, pIndex: Int, p_174322_: Boolean) {
        val f = pIndex.toFloat() / 24.0f
        val i = Mth.lerp(f, pEntityBlockLightLevel.toFloat(), pLeashHolderBlockLightLevel.toFloat()).toInt()
        val j = Mth.lerp(f, pEntitySkyLightLevel.toFloat(), pLeashHolderSkyLightLevel.toFloat()).toInt()
        val k = LightTexture.pack(i, j)
        val f1 = if (pIndex % 2 == (if (p_174322_) 1 else 0)) 0.7f else 1.0f
        val f2 = 0.5f * f1
        val f3 = 0.4f * f1
        val f4 = 0.3f * f1
        val f5 = p_174310_ * f
        val f6 = p_174311_ * f
        val f7 = p_174312_ * f
        pConsumer.vertex(pMatrix, f5 - p_174319_, f6 + p_174318_, f7 + p_174320_).color(f2, f3, f4, 1.0f).uv2(k).endVertex()
        pConsumer.vertex(pMatrix, f5 + p_174319_, f6 + p_174317_ - p_174318_, f7 - p_174320_).color(f2, f3, f4, 1.0f).uv2(k).endVertex()
    }
}