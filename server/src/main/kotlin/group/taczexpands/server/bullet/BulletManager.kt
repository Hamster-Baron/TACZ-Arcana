package group.taczexpands.server.bullet

import com.tacz.guns.entity.EntityKineticBullet
import group.taczexpands.common.network.v2.s2c.S2CSyncBullet
import group.taczexpands.common.util.BlockResistanceData
import group.taczexpands.server.config.ServerConfig
import group.taczexpands.server.network.NetworkManager
import group.taczexpands.server.util.TypeHolder
import net.minecraft.core.registries.Registries
import net.minecraft.world.level.block.Block

object BulletManager {
    val blockResistanceTable = mutableMapOf<TypeHolder<Block>, BlockResistanceData>()

    fun reloadBlockResistanceTable() {
        blockResistanceTable.clear()
        val list = ServerConfig.blockResistanceTable.get()
        for (entry in list) {
            val line = entry.replace(" ", "").split(",")
            if (line.size != 6) continue

            val key = line[0]
            val resistance = line[1].toIntOrNull() ?: continue
            val shouldDestroyBlock = line[2].toBooleanStrictOrNull() ?: continue
            val particleOnPenetrate = line[3].toBooleanStrictOrNull() ?: continue
            val accumulateBlockDamage = line[4].toBooleanStrictOrNull() ?: continue
            val deflectable = line[5].toBooleanStrictOrNull() ?: continue

            val holder = TypeHolder(Registries.BLOCK, key)

            blockResistanceTable[holder] = BlockResistanceData(resistance, shouldDestroyBlock, particleOnPenetrate, accumulateBlockDamage, deflectable, false)
        }
    }

    fun notify(bullet: EntityKineticBullet) {
        val velocity = bullet.deltaMovement
        NetworkManager.broadcast(S2CSyncBullet(bullet.id, bullet.x, bullet.y, bullet.z, bullet.yRot, bullet.xRot, velocity.x, velocity.y, velocity.z).create(), bullet)
    }
}