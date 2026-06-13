package group.taczexpands.server.event

import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import net.minecraftforge.eventbus.api.Event

class BulletExplosionKillEvent(val event: EntityHurtByGunEvent.Pre): Event() {
}