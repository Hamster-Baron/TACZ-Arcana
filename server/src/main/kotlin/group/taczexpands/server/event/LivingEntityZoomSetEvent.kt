package group.taczexpands.server.event

import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.eventbus.api.Cancelable
import net.minecraftforge.eventbus.api.Event

@Cancelable
class LivingEntityZoomSetEvent(val entity: LivingEntity, val zoomNumber: Int): Event() {

}
