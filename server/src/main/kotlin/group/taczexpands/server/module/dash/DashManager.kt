package group.taczexpands.server.module.dash

import group.taczexpands.server.config.action.Dash
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.entity.player.Player
import net.minecraft.world.phys.Vec3
import net.minecraftforge.event.TickEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import java.util.WeakHashMap
import kotlin.math.cos
import kotlin.math.sin

object DashManager {
    val activeDashes = WeakHashMap<ServerPlayer, DashInstance>()

    @SubscribeEvent
    fun onPlayerTick(event: TickEvent.PlayerTickEvent) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) return
        val player = event.player as? ServerPlayer ?: return

        val instance = activeDashes[player] ?: return

        if (!instance.tick()) {
            activeDashes.remove(player)
        }
    }

    fun getDirectionVec(yaw: Float, pitch: Float): Vec3 {
        val f = yaw * (Math.PI.toFloat() / 180f)
        val f1 = -pitch * (Math.PI.toFloat() / 180f)
        val f2 = cos(f1.toDouble()).toFloat()
        return Vec3(
            (-sin(f.toDouble()) * f2),
            sin(f1.toDouble()),
            (cos(f.toDouble()) * f2)
        ).normalize()
    }
}