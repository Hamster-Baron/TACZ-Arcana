package group.taczexpands.server.util

import net.minecraft.core.BlockPos
import net.minecraft.world.entity.Entity
import net.minecraft.world.phys.HitResult
import net.minecraft.world.phys.Vec3

data class CameraTraceResult(
    val type: HitResult.Type,
    val location: Vec3?,
    val blockPos: BlockPos?,
    val entity: Entity?,
    val distance: Double?
)
