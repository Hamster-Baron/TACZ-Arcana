package group.taczexpands.server.skill

import group.taczexpands.common.network.v2.s2c.S2CActionLockState
import group.taczexpands.server.module.rotation.RotationManager
import group.taczexpands.server.TACZExpandsServer
import group.taczexpands.server.listener.PlayerListener
import group.taczexpands.server.network.NetworkManager
import net.minecraft.server.level.ServerPlayer
import java.util.UUID

private enum class ActionLockSourceType { TIMED, STATUS, INTERNAL }

private data class ActionLockKey(
    val action: S2CActionLockState.Action,
    val sourceType: ActionLockSourceType,
    val status: TriggerType?,
    val identifier: String
)

private data class ActionLockEntry(val key: ActionLockKey, var expiresAtServerTick: Long?)

object ActionManager {
    val STATUS_MAP = mutableMapOf<ServerPlayer, MutableSet<TriggerType>>()
    private val actionLockEntries = mutableMapOf<ServerPlayer, MutableMap<ActionLockKey, ActionLockEntry>>()
    private val lastSynchronizedActions = mutableMapOf<ServerPlayer, Set<S2CActionLockState.Action>>()
    private val synchronizationRevisions = mutableMapOf<UUID, Long>()

    fun initialize(player: ServerPlayer) {
        clearPlayerState(player)
        synchronizationRevisions.remove(player.uuid)
        synchronize(player, true)
    }

    fun reinitialize(player: ServerPlayer) {
        clearPlayerState(player)
        synchronize(player, true)
    }

    fun discardPlayerInstance(player: ServerPlayer) {
        clearPlayerState(player)
    }

    fun resetAll() {
        RotationManager.clearAll()
        val players = linkedSetOf<ServerPlayer>()
        players.addAll(STATUS_MAP.keys)
        players.addAll(actionLockEntries.keys)
        players.addAll(lastSynchronizedActions.keys)
        players.forEach { player ->
            try {
                reinitialize(player)
            } catch (e: Exception) {
                TACZExpandsServer.LOGGER.error("Failed to synchronize action reset for ${player.uuid}", e)
            }
        }
    }

    fun cancelAction(player: ServerPlayer, action: S2CActionLockState.Action, entryTrigger: TriggerType, identifier: String) {
        val key = ActionLockKey(action, ActionLockSourceType.STATUS, entryTrigger, identifier)
        actionLockEntries.getOrPut(player) { mutableMapOf() }[key] = ActionLockEntry(key, null)
        applyImmediateCancellation(player, action)
        synchronize(player)
    }

    fun cancelAction(player: ServerPlayer, action: S2CActionLockState.Action, duration: Int, identifier: String) {
        val key = ActionLockKey(action, ActionLockSourceType.TIMED, null, identifier)
        if (duration > 0) {
            val expiresAtServerTick = player.serverLevel().gameTime + duration.toLong()
            actionLockEntries.getOrPut(player) { mutableMapOf() }[key] = ActionLockEntry(key, expiresAtServerTick)
            applyImmediateCancellation(player, action)
        } else {
            actionLockEntries[player]?.remove(key)
        }
        synchronize(player)
    }

    fun cancelInternalAction(player: ServerPlayer, action: S2CActionLockState.Action, identifier: String) {
        val key = ActionLockKey(action, ActionLockSourceType.INTERNAL, null, identifier)
        actionLockEntries.getOrPut(player) { mutableMapOf() }[key] = ActionLockEntry(key, null)
        applyImmediateCancellation(player, action)
        synchronize(player)
    }

    fun removeCancel(player: ServerPlayer, action: S2CActionLockState.Action, identifier: String) {
        val key = ActionLockKey(action, ActionLockSourceType.TIMED, null, identifier)
        actionLockEntries[player]?.remove(key)
        synchronize(player)
    }

    fun removeCancel(player: ServerPlayer, action: S2CActionLockState.Action, entryTrigger: TriggerType, identifier: String) {
        val key = ActionLockKey(action, ActionLockSourceType.STATUS, entryTrigger, identifier)
        actionLockEntries[player]?.remove(key)
        synchronize(player)
    }

    fun removeInternalAction(player: ServerPlayer, action: S2CActionLockState.Action, identifier: String) {
        val key = ActionLockKey(action, ActionLockSourceType.INTERNAL, null, identifier)
        actionLockEntries[player]?.remove(key)
        synchronize(player)
    }

    fun shouldCancel(player: ServerPlayer, action: S2CActionLockState.Action): Boolean {
        return actionLockEntries[player]?.values?.any { it.key.action == action && isActive(player, it) } == true
    }

    fun reset(player: ServerPlayer) {
        clearPlayerState(player)
        synchronizationRevisions.remove(player.uuid)
    }

    private fun clearPlayerState(player: ServerPlayer) {
        RotationManager.clear(player)
        STATUS_MAP.remove(player)
        actionLockEntries.remove(player)
        lastSynchronizedActions.remove(player)
    }

    fun hasStatus(player: ServerPlayer, triggerType: TriggerType): Boolean {
        return STATUS_MAP[player]?.contains(triggerType) ?: false
    }

    fun notifyStatusEntry(player: ServerPlayer, entryTrigger: TriggerType) {
        if (STATUS_MAP.getOrPut(player) { mutableSetOf() }.add(entryTrigger)) synchronize(player)
    }

    fun notifyStatusLeave(player: ServerPlayer, entryTrigger: TriggerType) {
        RotationManager.leave(player, entryTrigger)
        if (STATUS_MAP.getOrPut(player) { mutableSetOf() }.remove(entryTrigger)) synchronize(player)
    }

    fun onServerTick() {
        actionLockEntries.keys.toList().forEach { player ->
            val currentServerTick = player.serverLevel().gameTime
            val removed = actionLockEntries[player]?.entries?.removeIf {
                it.value.key.sourceType == ActionLockSourceType.TIMED &&
                    (it.value.expiresAtServerTick ?: Long.MAX_VALUE) <= currentServerTick
            } == true
            if (removed) synchronize(player)
        }
    }

    private fun synchronize(player: ServerPlayer, force: Boolean = false) {
        val actions = S2CActionLockState.Action.entries.filterTo(linkedSetOf()) { action -> shouldCancel(player, action) }
        if (!force && lastSynchronizedActions[player] == actions) return
        val revision = (synchronizationRevisions[player.uuid] ?: 0L) + 1L
        NetworkManager.sendToPlayer(S2CActionLockState(actions, revision).create(), player)
        synchronizationRevisions[player.uuid] = revision
        lastSynchronizedActions[player] = actions
    }

    private fun isActive(player: ServerPlayer, entry: ActionLockEntry): Boolean {
        return when (entry.key.sourceType) {
            ActionLockSourceType.TIMED -> (entry.expiresAtServerTick ?: Long.MIN_VALUE) > player.serverLevel().gameTime
            ActionLockSourceType.STATUS -> entry.key.status?.let { hasStatus(player, it) } == true
            ActionLockSourceType.INTERNAL -> true
        }
    }

    private fun applyImmediateCancellation(player: ServerPlayer, action: S2CActionLockState.Action) {
        when (action) {
            S2CActionLockState.Action.Sprint -> player.isSprinting = false
            S2CActionLockState.Action.ShootKey -> PlayerListener.getPlayerStates(player).shootKeyDown = false
            else -> {}
        }
    }
}
