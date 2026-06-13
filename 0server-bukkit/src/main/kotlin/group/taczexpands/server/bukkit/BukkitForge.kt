package group.taczexpands.server.bukkit

import java.lang.reflect.Method

object BukkitForge {
    var getHandleMethod: Method? = null
    var getBukkitEntityMethod: Method? = null

    fun bukkitToForge(entity: org.bukkit.entity.Entity): net.minecraft.world.entity.Entity {
        if (getHandleMethod == null) {
            getHandleMethod = entity.javaClass.getDeclaredMethod("getHandle")
        }
        return getHandleMethod!!.invoke(entity) as net.minecraft.world.entity.Entity
    }

    fun forgeToBukkit(entity: net.minecraft.world.entity.Entity): org.bukkit.entity.Entity {
        if (getBukkitEntityMethod == null) {
            getBukkitEntityMethod = entity.javaClass.getDeclaredMethod("getBukkitEntity")
        }
        return getBukkitEntityMethod!!.invoke(entity) as org.bukkit.entity.Entity
    }

    fun hasPermission(player: net.minecraft.world.entity.player.Player, permission: String): Boolean {
        return (player.toBukkit() as org.bukkit.entity.Player).hasPermission(permission)
    }
}

fun org.bukkit.entity.Entity.toForge(): net.minecraft.world.entity.Entity {
    return BukkitForge.bukkitToForge(this)
}

fun net.minecraft.world.entity.Entity.toBukkit(): org.bukkit.entity.Entity {
    return BukkitForge.forgeToBukkit(this)
}