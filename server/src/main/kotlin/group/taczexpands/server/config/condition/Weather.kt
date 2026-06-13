package group.taczexpands.server.config.condition

import group.taczexpands.server.config.condition.base.Condition
import group.taczexpands.server.context.Context
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class WeatherType {

    @SerialName("none")
    NONE,

    @SerialName("rain")
    RAIN,

    @SerialName("thunder")
    THUNDER,
}


@Serializable
@SerialName("Weather")
data class Weather(val weathers: List<WeatherType>) : Condition {

    companion object {
        val EXAMPLE = Weather(listOf(WeatherType.THUNDER, WeatherType.RAIN, WeatherType.NONE))
    }

    override fun check(context: Context): Boolean {
        val level = context.self.level()
        if (level.isRaining && !level.isThundering && weathers.contains(WeatherType.RAIN)) return true
        if (level.isThundering && weathers.contains(WeatherType.THUNDER)) return true
        if (weathers.contains(WeatherType.NONE)) return true
        return false
    }
}