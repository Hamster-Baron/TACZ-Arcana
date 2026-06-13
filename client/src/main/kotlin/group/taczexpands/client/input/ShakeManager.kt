package group.taczexpands.client.input

import com.tacz.guns.api.entity.IGunOperator
import group.taczexpands.client.util.Shake
import group.taczexpands.common.accessor.IAccessorGunData
import net.minecraft.client.Minecraft

object ShakeManager {
    val shakeList = mutableListOf<Shake>()

    var aimShake: Shake? = null

    fun onRenderTick() {
        val now = System.currentTimeMillis()
        shakeList.removeIf { !it.update(now) }
        aimShake?.let { if (!it.update(now)) aimShake = null }
    }

    fun onClientTick() {
        val mc = Minecraft.getInstance()
        val player = mc.player ?: let {
            aimShake = null
            return
        }

        val gunExtra = IAccessorGunData.getExtraHolder(player.mainHandItem) ?: let {
            aimShake = null
            return
        }
        val data = gunExtra.aimShake ?: let {
            aimShake = null
            return
        }

        if (!IGunOperator.fromLivingEntity(player).synIsAiming) {
            aimShake = null
            return
        }

        if (aimShake == null) {
            aimShake = Shake(
                data.amplitudeYRot,
                data.amplitudeXRot,
                24 * 60 * 60 * 1000L,
                data.frequencyHz,
                data.randomness,
                data.dynamicPhaseOffset,
                data.phaseOffsetYRot,
                data.phaseOffsetXRot,
                data.delay
            )
        }
    }

    fun add(shake: Shake) {
        shakeList.add(shake)
    }

    fun reset() {
        shakeList.clear()
        aimShake = null
    }
}