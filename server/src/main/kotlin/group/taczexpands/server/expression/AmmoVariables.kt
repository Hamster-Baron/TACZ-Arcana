package group.taczexpands.server.expression

import group.taczexpands.server.context.AmmoChangeRequestContext
import group.taczexpands.server.context.Context
import net.minecraft.world.entity.Entity

/** Registers ammo variables alongside built-ins and evaluates them on demand. */
internal class RequestedAmmoIdVariable : Variables.Variable<String>("RequestedAmmoId") {
    override fun get(context: Context, target: Entity?): String =
        (context as? AmmoChangeRequestContext)?.requestedAmmoId?.toString() ?: ""
}

/** Reads the actual projectile. It neither follows the held gun nor tracks entities. */
internal class BulletAmmoIdVariable : Variables.Variable<String>("BulletAmmoId") {
    override fun get(context: Context, target: Entity?): String {
        val bullet = context.bullet ?: return ""
        val ammoId = bullet.ammoId ?: return ""
        return ammoId.toString()
    }
}
