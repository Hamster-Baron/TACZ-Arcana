package group.taczexpands.server.event

import net.minecraft.world.entity.LivingEntity
import net.minecraftforge.eventbus.api.Event

class MeleeKillEvent(val user: LivingEntity, val target: LivingEntity, val damage: Float) : Event() {

}