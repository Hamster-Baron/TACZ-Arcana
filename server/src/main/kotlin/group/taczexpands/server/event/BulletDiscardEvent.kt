package group.taczexpands.server.event

import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.server.entity.BulletExtraData
import net.minecraftforge.eventbus.api.Event

class BulletDiscardEvent(val bullet: EntityKineticBullet, val extraData: BulletExtraData) : Event() {

}