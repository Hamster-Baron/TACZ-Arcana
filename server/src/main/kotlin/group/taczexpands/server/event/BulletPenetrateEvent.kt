package group.taczexpands.server.event

import com.tacz.guns.entity.EntityKineticBullet
import net.minecraft.world.phys.BlockHitResult
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event

@Cancelable
class BulletPenetrateEvent(val bullet: EntityKineticBullet, val hitResult: BlockHitResult): Event() {
}