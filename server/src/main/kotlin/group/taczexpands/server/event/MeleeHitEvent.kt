package group.taczexpands.server.event

import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.eventbus.api.Event

class MeleeHitEvent(val user: LivingEntity, val target: LivingEntity, var damage: Float) : Event() {

}