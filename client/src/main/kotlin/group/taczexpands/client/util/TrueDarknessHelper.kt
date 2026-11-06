package group.taczexpands.client.util

import group.taczexpands.common.TACZExpandsCommon.Companion.LOGGER
import java.lang.reflect.Field
import java.lang.reflect.InaccessibleObjectException
import java.lang.reflect.Modifier
import java.util.concurrent.ConcurrentHashMap

object TrueDarknessHelper {
    private var trueDarknessChloride: Boolean? = null
    private var trueDarknessPlusPlus: Boolean? = null
    private var trueDarknessExtraPlus: Boolean? = null

    private sealed class FieldResult {
        data class Success(val field: Field) : FieldResult()
        object NotFound : FieldResult()
    }

    private val fieldCache = ConcurrentHashMap<String, FieldResult>()

    fun saveStateAndTurnOff() {
        trueDarknessChloride = getBooleanField(
            "me.srrapero720.chloride.features.TrueDarknessFeature",
            "enabled"
        )
        setBooleanField(
            "me.srrapero720.chloride.features.TrueDarknessFeature",
            "enabled",
            false
        )

        trueDarknessPlusPlus = getBooleanField(
            "me.srrapero720.embeddiumplus.foundation.darkness.DarknessPlus",
            "enabled"
        )
        setBooleanField(
            "me.srrapero720.embeddiumplus.foundation.darkness.DarknessPlus",
            "enabled",
            false
        )

        trueDarknessExtraPlus = getBooleanField(
            "toni.embeddiumextras.foundation.darkness.DarknessPlus",
            "enabled"
        )
        setBooleanField(
            "toni.embeddiumextras.foundation.darkness.DarknessPlus",
            "enabled",
            false
        )
    }

    fun restoreState() {
        trueDarknessChloride?.let {
            setBooleanField(
                "me.srrapero720.chloride.features.TrueDarknessFeature",
                "enabled",
                it
            )
        }
        trueDarknessChloride = null

        trueDarknessPlusPlus?.let {
            setBooleanField(
                "me.srrapero720.embeddiumplus.foundation.darkness.DarknessPlus",
                "enabled",
                it
            )
        }
        trueDarknessPlusPlus = null

        trueDarknessExtraPlus?.let {
            setBooleanField(
                "toni.embeddiumextras.foundation.darkness.DarknessPlus",
                "enabled",
                it
            )
        }
        trueDarknessExtraPlus = null
    }

    private fun getBooleanField(className: String, fieldName: String): Boolean? {
        return when (val result = getCachedField(className, fieldName)) {
            is FieldResult.Success -> result.field.getBoolean(null)
            FieldResult.NotFound -> null
        }
    }

    private fun setBooleanField(className: String, fieldName: String, value: Boolean) {
        when (val result = getCachedField(className, fieldName)) {
            is FieldResult.Success -> result.field.setBoolean(null, value)
            FieldResult.NotFound -> {}
        }
    }

    private fun getCachedField(className: String, fieldName: String): FieldResult {
        val key = "$className#$fieldName"
        return fieldCache.getOrPut(key) {
            try {
                val clazz = Class.forName(className)
                val field = clazz.getDeclaredField(fieldName)
                if (field.type != Boolean::class.javaPrimitiveType || Modifier.isFinal(field.modifiers) || !Modifier.isStatic(field.modifiers)) {
                    LOGGER.warn("True Darkness field {}#{} must be a mutable static boolean", className, fieldName)
                    FieldResult.NotFound
                } else {
                    field.isAccessible = true
                    FieldResult.Success(field)
                }
            } catch (e: ClassNotFoundException) {
                LOGGER.debug("True Darkness class '{}' is unavailable", className, e)
                FieldResult.NotFound
            } catch (e: ReflectiveOperationException) {
                LOGGER.warn("Failed to resolve True Darkness field {}#{}", className, fieldName, e)
                FieldResult.NotFound
            } catch (e: InaccessibleObjectException) {
                LOGGER.warn("True Darkness field {}#{} is inaccessible", className, fieldName, e)
                FieldResult.NotFound
            }
        }
    }
}
