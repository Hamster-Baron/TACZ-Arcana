package group.taczexpands.server.skill

import group.taczexpands.common.network.s2c.S2CCancelAction
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.network.NetworkManager
import net.minecraft.server.level.ServerPlayer

data class ActionCancelStatus(val action: S2CCancelAction.Action, val identifier: String?, var duration: Int?, var entryTrigger: TriggerType?)


object ActionManager {
    val STATUS_MAP = mutableMapOf<ServerPlayer, MutableSet<TriggerType>>()
    val CANCEL_ACTION_MAP = mutableMapOf<ServerPlayer, MutableList<ActionCancelStatus>>()

    fun cancelAction(player: ServerPlayer, action: S2CCancelAction.Action, entryTrigger: TriggerType, identifier: String?) {
        val playerCancelList = CANCEL_ACTION_MAP.getOrPut(player) { mutableListOf() }
        val existsStatus = playerCancelList.firstOrNull { it.action == action && it.entryTrigger == entryTrigger }
        if (existsStatus == null) {
            val newCancelStatus = ActionCancelStatus(action, identifier, null, entryTrigger)
            playerCancelList.add(newCancelStatus)
            if (hasStatus(player, entryTrigger)) {
                NetworkManager.sendToPlayer(S2CCancelAction(action, Int.MAX_VALUE - 1), player)
            }
        }
    }

    fun cancelAction(player: ServerPlayer, action: S2CCancelAction.Action, duration: Int, identifier: String?) {
        if (duration > 0) {
            val playerCancelList = CANCEL_ACTION_MAP.getOrPut(player) { mutableListOf() }
            val existsStatus = playerCancelList.firstOrNull { it.action == action }
            if (existsStatus == null) {
                val newCancelStatus = ActionCancelStatus(action, identifier, duration, null)
                playerCancelList.add(newCancelStatus)
            } else {
                existsStatus.duration = duration
            }
        }

        when (action) {
            S2CCancelAction.Action.Sprint -> player.isSprinting = false

            S2CCancelAction.Action.ShootKey -> {
                PlayerListener.getPlayerStates(player).shootKeyDown = false
            }

            else -> {}
        }

        val triggerEntry = CANCEL_ACTION_MAP[player]?.firstOrNull {
            it.action == action && it.entryTrigger != null && hasStatus(player, it.entryTrigger!!)
        }

        if (triggerEntry == null) {
            NetworkManager.sendToPlayer(S2CCancelAction(action, duration), player)
        }
    }

    fun removeCancel(player: ServerPlayer, action: S2CCancelAction.Action, identifier: String?) {
        if (CANCEL_ACTION_MAP[player]?.removeIf { it.action == action && it.duration != null && (identifier == null || identifier == it.identifier) } ?: false) {
            val triggerEntry = CANCEL_ACTION_MAP[player]?.firstOrNull {
                it.action == action && it.entryTrigger != null && hasStatus(player, it.entryTrigger!!)
            }
            if (triggerEntry == null) {
                NetworkManager.sendToPlayer(S2CCancelAction(action, 0, true), player)
            }
        }
    }

    fun removeCancel(player: ServerPlayer, action: S2CCancelAction.Action, entryTrigger: TriggerType, identifier: String?) {
        if (CANCEL_ACTION_MAP[player]?.removeIf { it.action == action && it.entryTrigger == entryTrigger && (identifier == null || identifier == it.identifier) } ?: false) {
            val durationEntry = CANCEL_ACTION_MAP[player]?.firstOrNull { it.action == action && it.duration != null }
            if (durationEntry == null) {
                NetworkManager.sendToPlayer(S2CCancelAction(action, 0, true), player)
            } else {
                NetworkManager.sendToPlayer(S2CCancelAction(action, durationEntry.duration!!), player)
            }
        }
    }

    fun shouldCancel(player: ServerPlayer, action: S2CCancelAction.Action): Boolean {
        return CANCEL_ACTION_MAP[player]?.firstOrNull {
            it.action == action && ((it.duration != null) || (it.entryTrigger != null && hasStatus(player, it.entryTrigger!!)))
        } != null

    }

    fun reset(player: ServerPlayer) {
        CANCEL_ACTION_MAP.remove(player)
    }

    fun hasStatus(player: ServerPlayer, triggerType: TriggerType): Boolean {
        return STATUS_MAP[player]?.contains(triggerType) ?: false
    }

    fun notifyStatusEntry(player: ServerPlayer, entryTrigger: TriggerType) {
        STATUS_MAP.getOrPut(player) { mutableSetOf() }.add(entryTrigger)
        val playerCancelList = CANCEL_ACTION_MAP.getOrPut(player) { mutableListOf() }
        playerCancelList.filter { it.entryTrigger == entryTrigger }.forEach { status ->
            NetworkManager.sendToPlayer(S2CCancelAction(status.action, Int.MAX_VALUE - 1), player)
        }
    }

    fun notifyStatusLeave(player: ServerPlayer, entryTrigger: TriggerType) {
        STATUS_MAP.getOrPut(player) { mutableSetOf() }.remove(entryTrigger)
        val playerCancelList = CANCEL_ACTION_MAP.getOrPut(player) { mutableListOf() }
        playerCancelList.filter { it.entryTrigger == entryTrigger }.forEach { status ->
            val durationEntry = playerCancelList.firstOrNull { it.action == status.action && it.duration != null }
            if (durationEntry == null) {
                NetworkManager.sendToPlayer(S2CCancelAction(status.action, 0, true), player)
            } else {
                NetworkManager.sendToPlayer(S2CCancelAction(status.action, durationEntry.duration!!), player)
            }
        }
    }


    fun onServerTick() {
        CANCEL_ACTION_MAP.forEach { (player, list) ->
            list.indices.reversed().forEach {
                val entry = list[it]
                if (entry.duration == null) return@forEach
                entry.duration = entry.duration!! - 1
                if (entry.duration!! <= 0)
                    list.removeAt(it)
            }
        }
    }
}