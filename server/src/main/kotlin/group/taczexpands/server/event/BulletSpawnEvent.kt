package group.taczexpands.server.event

import com.tacz.guns.entity.EntityKineticBullet
import net.minecraftforge.eventbus.api.Event

class BulletSpawnEvent(val bullet: EntityKineticBullet) : Event() {

}