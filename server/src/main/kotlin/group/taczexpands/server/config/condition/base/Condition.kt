package group.taczexpands.server.config.condition.base

import group.taczexpands.server.context.Context

interface Condition {
    fun check(context: Context): Boolean
}