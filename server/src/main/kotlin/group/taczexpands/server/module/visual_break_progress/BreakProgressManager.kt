package group.taczexpands.server.module.visual_break_progress

import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap
import net.minecraft.core.BlockPos
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket
import net.minecraft.resources.ResourceKey
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraftforge.server.ServerLifecycleHooks


object BreakProgressManager {
    val progressMap = mutableMapOf<ResourceKey<Level>, Long2ObjectOpenHashMap<BlockHolder>>()

    fun getOrCreateHolder(level: ServerLevel, blockPos: BlockPos): BlockHolder {
        val map = progressMap.computeIfAbsent(level.dimension()) { Long2ObjectOpenHashMap() }
        return map.computeIfAbsent(blockPos.asLong()) { BlockHolder() }
    }

    fun getHolder(level: ServerLevel, blockPos: BlockPos): BlockHolder? {
        val map = progressMap[level.dimension()] ?: return null
        return map[blockPos.asLong()] ?: return null
    }

    fun putHolder(level: ServerLevel, blockPos: BlockPos, holder: BlockHolder) {
        val map = progressMap.computeIfAbsent(level.dimension()) { Long2ObjectOpenHashMap() }
        map[blockPos.asLong()] = holder
    }

    fun removeHolder(level: ServerLevel, blockPos: BlockPos) {
        val map = progressMap[level.dimension()] ?: return
        map.remove(blockPos.asLong())
    }

    fun updateDamage(level: ServerLevel, blockPos: BlockPos, damage: Float): Pair<Float, Boolean> {
        val holder = getOrCreateHolder(level, blockPos)
        val needProgress = 1.0f - holder.progress
        val remainingDamage =
            if (needProgress <= damage) {
                holder.progress = 1.0f
                damage - needProgress
            } else {
                holder.progress += damage
                0.0f
            }

        if (holder.progress >= 1.0f) {
            removeHolder(level, blockPos)
        }

        notify(level, blockPos, holder.getStage())
        return remainingDamage to (holder.progress >= 1.0f)
    }

    fun notify(level: ServerLevel, blockPos: BlockPos, stage: Int) {
        level.chunkSource.chunkMap.getPlayers(ChunkPos(blockPos), false).forEach {
            it.connection.send(ClientboundBlockDestructionPacket(blockPos.hashCode(), blockPos, stage))
        }
    }

    fun onServerTick() {
        val server = ServerLifecycleHooks.getCurrentServer()
        val currentTicks = server.tickCount
        if (currentTicks % 50 != 0) return
        progressMap.forEach {
            val level = server.getLevel(it.key) ?: return@forEach
            val iterator = it.value.long2ObjectEntrySet().fastIterator()
            while (iterator.hasNext()) {
                val entry = iterator.next()
                val holder = entry.value
                if (currentTicks - holder.lastUpdated >= 100) {
                    notify(level, BlockPos.of(entry.longKey), -1)
                    iterator.remove()
                }
            }
        }
    }
}
