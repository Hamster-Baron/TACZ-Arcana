package group.taczexpands.client.compat.iris

import group.taczexpands.client.accessor.IAccessorIrisApiV0Impl
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.apiimpl.IrisApiV0Impl
import net.irisshaders.iris.pathways.HandRenderer
import net.irisshaders.iris.vertices.ImmediateState

object IrisCompat {
    fun hasShaderPackInUse(): Boolean {
        return IrisApi.getInstance().isShaderPackInUse
    }

    fun isHandRendererActive(): Boolean {
        return HandRenderer.INSTANCE.isActive
    }

    fun setHookUsingShaderPack(): Boolean {
        return (IrisApiV0Impl.INSTANCE as IAccessorIrisApiV0Impl).`taczexpands$setHookUsingShaderPack`(true)
    }

    fun restoreHookUsingShaderPack(previous: Boolean) {
        (IrisApiV0Impl.INSTANCE as IAccessorIrisApiV0Impl).`taczexpands$setHookUsingShaderPack`(previous)
    }

    /** The caller owns this saved value, including when thermal rendering nests inside its pass. */
    fun beginLightweightLayout(): Boolean {
        val previous = ImmediateState.renderWithExtendedVertexFormat
        ImmediateState.renderWithExtendedVertexFormat = false
        return previous
    }

    fun endLightweightLayout(previous: Boolean) {
        ImmediateState.renderWithExtendedVertexFormat = previous
    }

}
