package group.taczexpands.client.compat.iris

import group.taczexpands.client.accessor.IAccessorIrisApiV0Impl
import net.irisshaders.iris.api.v0.IrisApi
import net.irisshaders.iris.apiimpl.IrisApiV0Impl
import net.irisshaders.iris.pathways.HandRenderer
import net.irisshaders.iris.uniforms.CapturedRenderingState
import net.irisshaders.iris.vertices.ImmediateState

object IrisCompat {
    fun hasShaderPackInUse(): Boolean {
        return IrisApi.getInstance().isShaderPackInUse
    }

    fun isHandRendererActive(): Boolean {
        return HandRenderer.INSTANCE.isActive
    }

    fun setHookUsingShaderPack() {
        (IrisApiV0Impl.INSTANCE as IAccessorIrisApiV0Impl).`taczexpands$setHookUsingShaderPack`()
    }

    fun unSetHookUsingShaderPack() {
        (IrisApiV0Impl.INSTANCE as IAccessorIrisApiV0Impl).`taczexpands$unSetHookUsingShaderPack`()
    }

    var prevState: Boolean? = null

    fun saveAndDisableExtendedVertexFormat() {
        prevState = ImmediateState.renderWithExtendedVertexFormat
        ImmediateState.renderWithExtendedVertexFormat = false
    }

    fun restoreExtendedVertexFormat() {
        if (prevState != null) {
            ImmediateState.renderWithExtendedVertexFormat = prevState!!
        }
        prevState = null
    }

    var prevEntity: Int? = null

    fun saveAndSetCurrentEntity(value: Int) {
        prevEntity = CapturedRenderingState.INSTANCE.currentRenderedEntity
        CapturedRenderingState.INSTANCE.setCurrentEntity(value)
    }

    fun restoreCurrentEntity() {
        if (prevEntity != null) {
            CapturedRenderingState.INSTANCE.setCurrentEntity(prevEntity!!)
        }
        prevEntity = null
    }


}