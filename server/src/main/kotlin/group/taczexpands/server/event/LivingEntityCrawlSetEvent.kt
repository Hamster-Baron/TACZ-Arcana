package group.taczexpands.server.event

import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.eventbus.api.Event

class LivingEntityCrawlSetEvent(val entity: LivingEntity, val isCrawling: Boolean) : Event() {
}