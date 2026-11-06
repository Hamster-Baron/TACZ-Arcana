package group.taczexpands.server.config.action.rotation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Skill configuration. Resolved, world-relative limits belong to common.rotation. */
@Serializable
@SerialName("RotationRange")
data class RotationSpec(
    val yawMin: Double = -180.0,
    val yawMax: Double = 180.0,
    val pitchMin: Double = -90.0,
    val pitchMax: Double = 90.0,
    val yawReference: String = "CURRENT",
    val pitchReference: String = "HORIZONTAL",
    val yawCenter: Double = 0.0,
    val pitchCenter: Double = 0.0,
    val releaseOnMove: Boolean = false,
    val releaseOnPoseChange: Boolean = false,
    val releaseOnItemChange: Boolean = false
) {
    init {
        require(listOf(yawMin, yawMax, pitchMin, pitchMax, yawCenter, pitchCenter).all { it.isFinite() }) {
            "rotation angles must be finite"
        }
        require(yawMin >= -180 && yawMax <= 180 && yawMin <= yawMax &&
            pitchMin >= -180 && pitchMax <= 180 && pitchMin <= pitchMax) {
            "rotation bounds invalid"
        }
        require(yawReference == "CURRENT" || yawReference == "FIXED") {
            "yawReference must be CURRENT or FIXED"
        }
        require(pitchReference == "CURRENT" || pitchReference == "HORIZONTAL" || pitchReference == "FIXED") {
            "pitchReference must be CURRENT, HORIZONTAL or FIXED"
        }
        require(kotlin.math.abs(pitchCenter) <= 90) { "pitchCenter must be within -90..90" }
    }
}
