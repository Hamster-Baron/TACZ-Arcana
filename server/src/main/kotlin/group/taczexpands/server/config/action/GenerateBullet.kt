package group.taczexpands.server.config.action

import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.entity.EntityKineticBullet
import com.tacz.guns.sound.SoundManager
import group.taczexpands.server.accessor.IAccessorBullet
import group.taczexpands.server.bullet.CameraData
import group.taczexpands.server.bullet.MissileManager
import group.taczexpands.server.config.SelectorData
import group.taczexpands.server.config.action.base.Action
import group.taczexpands.server.config.action.base.ListPrepareData
import group.taczexpands.server.config.action.base.PrepareData
import group.taczexpands.server.config.action.base.SelectorPrepareData
import group.taczexpands.server.config.action.base.toData
import group.taczexpands.server.config.create
import group.taczexpands.server.context.Context
import group.taczexpands.server.event.BulletSpawnEvent
import group.taczexpands.server.expression.ExpressionHelper
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.skill.Skill
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerLevel
import net.minecraft.server.level.ServerPlayer
import net.minecraft.util.Mth
import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.common.MinecraftForge
import kotlin.jvm.optionals.getOrNull
import kotlin.random.Random


@Serializable
@SerialName("GenerateBullet")
data class GenerateBullet(
    val gunId: String,
    val shooter: SelectorData? = null,
    val x: String,
    val y: String,
    val z: String,
    val yaw: String,
    val pitch: String,
    val inaccuracy: String,
    val isTracer: Boolean,
    val bypassSpawnTrigger: Boolean,
    val soundDistance: Int,
    val silenceSound: Boolean,
    val setLockingTarget: Boolean = false,
    val missileTarget: SelectorData? = null,
    val isTV: Boolean = false,
    val args: List<String>? = null,
    override val delay: Int? = null
) : Action {

    companion object {
        val EXAMPLE = GenerateBullet(
            "tacz:ak47",
            x = "",
            y = "",
            z = "",
            yaw = "",
            pitch = "",
            inaccuracy = "",
            isTracer = false,
            bypassSpawnTrigger = false,
            soundDistance = 32,
            silenceSound = false
        )
    }

    override fun prepare(skill: Skill, context: Context): PrepareData {
        return ListPrepareData(shooter.create(context).toData(), missileTarget.create(context).toData())
    }

    override fun execute(skill: Skill, context: Context, data: PrepareData) {
        val targets = data.dataList[0].selector.getTargets(context)
        targets.forEach { target ->
            if (target !is LivingEntity) return@forEach
            val level = target.level() as? ServerLevel ?: return@forEach
            val gunId = ResourceLocation(gunId)
            val gunIndex = TimelessAPI.getCommonGunIndex(gunId).getOrNull() ?: return@forEach

            if (setLockingTarget) {
                if (target !is ServerPlayer) return@forEach

                PlayerListener.getPlayerStates(target).lockingTargetBuffer =
                    data.dataList[1].selector.getTarget(context)
            }

            if (isTV) {
                if (target !is ServerPlayer) return@forEach

                val cameraData = MissileManager.tvGuidancePlayers[target]
                if (cameraData != null) {
                    if (cameraData.bullet == null || !cameraData.bullet!!.isRemoved) {
                        return
                    } else {
                        MissileManager.tvGuidancePlayers.remove(target)
                    }
                }

                MissileManager.tvGuidancePlayers[target] = CameraData()
            }

            val entity = EntityKineticBullet(
                target.level(),
                target,
                target.mainHandItem,
                gunIndex.gunData.ammoId,
                gunId,
                gunIndex.pojo.display,
                isTracer,
                gunIndex.gunData,
                gunIndex.gunData.bulletData
            )
            val x = ExpressionHelper.parse(x, args, context, target)?.toDoubleOrNull() ?: 0.0
            val y = ExpressionHelper.parse(y, args, context, target)?.toDoubleOrNull() ?: 0.0
            val z = ExpressionHelper.parse(z, args, context, target)?.toDoubleOrNull() ?: 0.0
            val yaw = ExpressionHelper.parse(yaw, args, context, target)?.toFloatOrNull() ?: 0.0f
            val pitch = ExpressionHelper.parse(pitch, args, context, target)?.toFloatOrNull() ?: 0.0f
            val inaccuracy = ExpressionHelper.parse(inaccuracy, args, context, target)?.toFloatOrNull() ?: 0.0f

            val speed = Mth.clamp(gunIndex.gunData.bulletData.speed / 20.0f, 0f, Float.MAX_VALUE)

            (entity as IAccessorBullet).`taczexpands$initCustomData`(x, y, z, speed)
            entity.shootFromRotation(target, pitch, yaw, 0.0f, speed, inaccuracy)
            level.addFreshEntity(entity)
            if (soundDistance > 0) {
                val soundId = if (silenceSound) SoundManager.SILENCE_3P_SOUND else SoundManager.SHOOT_3P_SOUND
                SoundManager.sendSoundToNearby(target, soundDistance, gunId, gunIndex.pojo.display, soundId, 0.8f, 0.9f + Random.nextFloat() * 0.125f)

            }
            if (!bypassSpawnTrigger) {
                MinecraftForge.EVENT_BUS.post(BulletSpawnEvent(entity))
            }
        }
    }
}