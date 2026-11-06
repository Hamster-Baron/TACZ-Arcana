package group.taczexpands.server.context

import net.minecraft.resources.ResourceLocation
import net.minecraft.server.level.ServerPlayer

/** The immutable ID carried by the request packet, independent of later held-gun changes. */
class AmmoChangeRequestContext(
    self: ServerPlayer,
    val requestedAmmoId: ResourceLocation
) : Context(self)
