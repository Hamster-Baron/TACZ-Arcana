package group.taczexpands.server.util

import group.taczexpands.common.TACZExpandsCommon.Companion.LOGGER

inline fun handleEvent(eventName: String, action: () -> Unit) {
    try {
        action()
    } catch (e: Exception) {
        LOGGER.error("Failed to handle event '{}'", eventName, e)
    }
}
