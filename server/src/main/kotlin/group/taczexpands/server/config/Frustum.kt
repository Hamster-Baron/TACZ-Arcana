package group.taczexpands.server.config

import group.taczexpands.server.util.getCenterPosition
import net.minecraft.world.phys.Vec3
import javax.swing.text.html.parser.Entity
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.tan

class Frustum(val pos: Vec3, val yaw: Float, val pitch: Float, val fov: Double, val aspect: Float, val range: Double) {
    private val fovRad = Math.toRadians(fov)
    private val halfHeight = tan(fovRad / 2.0)
    private val halfWidth = halfHeight * aspect

    private val look = Vec3.directionFromRotation(pitch, yaw).normalize()

    private val right = if (abs(pitch) > 89.9f) {
        val yawRad = Math.toRadians(yaw.toDouble())
        Vec3(-cos(yawRad), 0.0, -sin(yawRad)).normalize()
    } else {
        look.cross(Vec3(0.0, 1.0, 0.0)).normalize()
    }

    private val up = right.cross(look).normalize()

    fun isInFrustum(center: Vec3): Boolean {
        val v = center.subtract(pos)

        val z = v.dot(look)
        if (z < 0 || z > range) return false

        val hBound = z * halfHeight
        val wBound = z * halfWidth

        val y = v.dot(up)
        val x = v.dot(right)

        return abs(y) <= hBound && abs(x) <= wBound
    }
}