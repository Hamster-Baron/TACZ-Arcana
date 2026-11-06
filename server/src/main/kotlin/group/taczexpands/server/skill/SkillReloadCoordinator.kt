package group.taczexpands.server.skill

import group.taczexpands.server.TACZExpandsServer
import group.taczexpands.server.util.schedule

object SkillReloadCoordinator {
    private var serverStarted = false
    private var resourceGeneration = 0L
    private var loadedResourceGeneration = -1L
    private var reloadScheduled = false

    @Synchronized
    fun requestResourceReload() {
        resourceGeneration++
        if (!serverStarted || reloadScheduled) return
        reloadScheduled = true
        schedule(0) { runScheduledReload() }
    }

    fun onServerStarted(): SkillReloadResult {
        val generation = synchronized(this) {
            serverStarted = true
            resourceGeneration
        }
        val result = TACZExpandsServer.INSTANCE.reload()
        synchronized(this) {
            loadedResourceGeneration = generation
        }
        return result
    }

    fun reloadNow(): SkillReloadResult {
        val generation = synchronized(this) { resourceGeneration }
        val result = TACZExpandsServer.INSTANCE.reload()
        synchronized(this) {
            loadedResourceGeneration = generation
        }
        return result
    }

    private fun runScheduledReload() {
        val generation = synchronized(this) {
            reloadScheduled = false
            if (!serverStarted || resourceGeneration == loadedResourceGeneration) null else resourceGeneration
        } ?: return
        TACZExpandsServer.INSTANCE.reload()
        synchronized(this) {
            loadedResourceGeneration = generation
            if (serverStarted && resourceGeneration != loadedResourceGeneration && !reloadScheduled) {
                reloadScheduled = true
                schedule(0) { runScheduledReload() }
            }
        }
    }
}
