package group.taczexpands.server.skill

object DefinitionScope {
    var current: String? = null

    fun <T> with(scope: String, block: () -> T): T {
        val previous = current
        current = scope
        return try {
            block()
        } finally {
            current = previous
        }
    }
}
