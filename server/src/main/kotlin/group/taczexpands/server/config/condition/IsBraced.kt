package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import group.taczexpands.server.util.hasSolidGroundInFront
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
@SerialName("IsBraced")
data class IsBraced(val `is`: Boolean) : Condition {
    companion object {
        val EXAMPLE = IsBraced(true)
    }

    override fun check(context: Context): Boolean {
        val isBraced = context.self.hasSolidGroundInFront()
        return isBraced == `is`
    }

}