package group.taczexpands.client.config

object RuntimeConfig {
    var defaultBlockAimWhileReloading: Boolean = false

    fun reset() {
        defaultBlockAimWhileReloading = false
    }
}