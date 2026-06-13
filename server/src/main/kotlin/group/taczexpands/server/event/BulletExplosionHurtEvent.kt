package group.taczexpands.server.event

import com.tacz.guns.api.event.common.EntityHurtByGunEvent
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event

@Cancelable
class BulletExplosionHurtEvent(val event: EntityHurtByGunEvent.Pre): Event() {
}