package group.taczexpands.server.event

import net.minecraft.server.level.ServerPlayer
import net.minecraftforge.eventbus.api.Event

class PlayerMouseScrollEvent(val player: ServerPlayer, val delta: Int): Event() {
}