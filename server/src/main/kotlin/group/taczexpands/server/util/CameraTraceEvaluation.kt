package group.taczexpands.server.util

import group.taczexpands.server.context.Context
import net.minecraft.world.entity.Entity
import java.util.IdentityHashMap

class CameraTraceEvaluationScope {
    private val results = IdentityHashMap<Entity, CameraTraceResult>()

    fun trace(viewer: Entity): CameraTraceResult = results.getOrPut(viewer) { CameraRaycastService.trace(viewer) }

    fun invalidate() {
        results.clear()
    }
}

object CameraTraceEvaluation {
    fun trace(context: Context, target: Entity?): CameraTraceResult {
        val viewer = target ?: context.self
        return context.cameraTraceEvaluationScope?.trace(viewer) ?: CameraRaycastService.trace(viewer)
    }

    fun <T> withFreshScope(context: Context, block: () -> T): T {
        val previous = context.cameraTraceEvaluationScope
        val scope = CameraTraceEvaluationScope()
        context.cameraTraceEvaluationScope = scope
        return try {
            block()
        } finally {
            scope.invalidate()
            context.cameraTraceEvaluationScope = previous
            previous?.invalidate()
        }
    }

    fun <T> withScope(context: Context, block: () -> T): T =
        if (context.cameraTraceEvaluationScope != null) block() else withFreshScope(context, block)

    fun <T> withScope(context: Context, scope: CameraTraceEvaluationScope, block: () -> T): T {
        val previous = context.cameraTraceEvaluationScope
        context.cameraTraceEvaluationScope = scope
        return try {
            block()
        } finally {
            context.cameraTraceEvaluationScope = previous
        }
    }
}
