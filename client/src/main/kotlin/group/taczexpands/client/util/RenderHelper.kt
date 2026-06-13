package group.taczexpands.client.util

import com.mojang.blaze3d.pipeline.RenderTarget
import com.mojang.blaze3d.platform.GlStateManager
import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.*
import com.tacz.guns.client.model.FunctionalBedrockPart
import com.tacz.guns.client.model.bedrock.BedrockModel
import com.tacz.guns.client.model.bedrock.BedrockPart
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO
import com.tacz.guns.client.resource.pojo.model.BonesItem
import com.tacz.guns.util.RenderHelper
import group.taczexpands.client.TACZExpandsClient
import group.taczexpands.client.compat.CompatHelper.hasIris
import group.taczexpands.client.compat.iris.IrisCompat.hasShaderPackInUse
import group.taczexpands.client.mixin.accessor.IAccessorBedrockPart
import group.taczexpands.client.mixin.accessor.IAccessorBonesItem
import group.taczexpands.client.mixin.accessor.IAccessorRenderTarget
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.LevelRenderer
import org.joml.Matrix4f
import org.lwjgl.opengl.GL11
import org.lwjgl.opengl.GL30
import java.nio.ByteBuffer


object RenderHelper {
    fun drawFullscreenQuad(texture: Int, scale: Float) {
        GL30.glBindTexture(GL30.GL_TEXTURE_2D, texture)

        GL30.glBegin(GL30.GL_QUADS)
        GL30.glTexCoord2f(0.0f, 0.0f)
        GL30.glVertex2f(-1.0f * scale, -1.0f * scale)
        GL30.glTexCoord2f(1.0f, 0.0f)
        GL30.glVertex2f(1.0f * scale, -1.0f * scale)
        GL30.glTexCoord2f(1.0f, 1.0f)
        GL30.glVertex2f(1.0f * scale, 1.0f * scale)
        GL30.glTexCoord2f(0.0f, 1.0f)
        GL30.glVertex2f(-1.0f * scale, 1.0f * scale)
        GL30.glEnd()
    }

    fun copyAndScaleFramebuffer(targetFramebuffer: Int, scaleFactor: Int) {
        val mc = Minecraft.getInstance()
        val worldFramebuffer = mc.mainRenderTarget.frameBufferId

        val width = mc.window.width
        val height = mc.window.height

        val scaledWidth = width * scaleFactor
        val scaledHeight = height * scaleFactor

        val prevFramebufferId = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)

        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, targetFramebuffer)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, worldFramebuffer)

        GL30.glBlitFramebuffer(
            0, 0, width, height,
            0, 0, scaledWidth, scaledHeight,
            GL30.GL_COLOR_BUFFER_BIT, GL30.GL_LINEAR
        )

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebufferId)
    }

    fun createFramebuffer(width: Int, height: Int): Pair<Int, Int> {
        val fbo = GL30.glGenFramebuffers()
        val prevFramebufferId = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)
        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo)

        val texture = GL11.glGenTextures()
        GL11.glBindTexture(GL11.GL_TEXTURE_2D, texture)
        GL11.glTexImage2D(
            GL11.GL_TEXTURE_2D,
            0,
            GL11.GL_RGB,
            width,
            height,
            0,
            GL11.GL_RGB,
            GL11.GL_UNSIGNED_BYTE,
            null as ByteBuffer?
        )
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR)
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR)

        GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, texture, 0)


        val rbo = GL30.glGenRenderbuffers()
        GL30.glBindRenderbuffer(GL30.GL_RENDERBUFFER, rbo)
        GL30.glRenderbufferStorage(GL30.GL_RENDERBUFFER, GL30.GL_DEPTH24_STENCIL8, width, height)
        GL30.glFramebufferRenderbuffer(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_STENCIL_ATTACHMENT, GL30.GL_RENDERBUFFER, rbo)


        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebufferId)

        return fbo to texture
    }

    fun copyWorldFramebuffer(targetFramebuffer: Int) {
        val mc = Minecraft.getInstance()
        val worldFramebuffer = mc.mainRenderTarget.frameBufferId

        val prevFramebufferId = GL30.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING)
        GL30.glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, targetFramebuffer)
        GL30.glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, worldFramebuffer)

        val width = mc.window.width
        val height = mc.window.height

        GL30.glBlitFramebuffer(
            0, 0, width, height,
            0, 0, width, height,
            GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST
        )

        GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFramebufferId)
    }

    fun copy(from: RenderTarget, to: RenderTarget) {
        RenderSystem.assertOnRenderThreadOrInit()
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, from.frameBufferId)
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to.frameBufferId)
        GlStateManager._glBlitFrameBuffer(0, 0, from.width, from.height, 0, 0, to.width, to.height, GL30.GL_COLOR_BUFFER_BIT, GL30.GL_NEAREST)
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0)
    }

    fun copyStencil(from: RenderTarget, to: RenderTarget) {
        val currentStencilMask = GL30.glGetInteger(GL30.GL_STENCIL_WRITEMASK)
        val currentReadFbo = GL30.glGetInteger(GL30.GL_READ_FRAMEBUFFER_BINDING)
        val currentDrawFbo = GL30.glGetInteger(GL30.GL_DRAW_FRAMEBUFFER_BINDING)

        GlStateManager._stencilMask(0xff)
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, from.frameBufferId)
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, to.frameBufferId)

        GlStateManager._glBlitFrameBuffer(0, 0, from.width, from.height, 0, 0, to.width, to.height, GL30.GL_STENCIL_BUFFER_BIT, GL30.GL_NEAREST)
        GlStateManager._stencilMask(currentStencilMask)
        GlStateManager._glBindFramebuffer(GL30.GL_READ_FRAMEBUFFER, currentReadFbo)
        GlStateManager._glBindFramebuffer(GL30.GL_DRAW_FRAMEBUFFER, currentDrawFbo)
    }

    fun blitScope(p_83972_: Int, p_83973_: Int, p_83974_: Boolean, target: RenderTarget) {
        RenderSystem.assertOnRenderThread()
        val originalDepthFunc = GL11.glGetInteger(GL11.GL_DEPTH_FUNC)

        GlStateManager._colorMask(true, true, true, true)
        GlStateManager._enableDepthTest()
        RenderSystem.depthFunc(GL11.GL_ALWAYS)
        GlStateManager._depthMask(true)
        GlStateManager._viewport(0, 0, p_83972_, p_83973_)
        if (p_83974_) {
            GlStateManager._disableBlend()
        }

        val shaderinstance = TACZExpandsClient.getBlitScreenScopeShader()
        shaderinstance.setSampler("DiffuseSampler", target.colorTextureId)
        val matrix4f = (Matrix4f()).setOrtho(0.0f, p_83972_.toFloat(), p_83973_.toFloat(), 0.0f, 1000.0f, 3000.0f)
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z)
        if (shaderinstance.MODEL_VIEW_MATRIX != null) {
            shaderinstance.MODEL_VIEW_MATRIX!!.set((Matrix4f()).translation(0.0f, 0.0f, -2000.0f))
        }

        if (shaderinstance.PROJECTION_MATRIX != null) {
            shaderinstance.PROJECTION_MATRIX!!.set(matrix4f)
        }

        val (r, g, b) = if (TACZExpandsClient.shouldUseThermalImaging(true)) {
            shaderinstance.safeGetUniform("ThermalImaging").set(1)
            if (hasIris() && hasShaderPackInUse()) {
                Triple(255, 65, 130)
            } else {
                Triple(65, 65, 130)
            }

        } else if (TACZExpandsClient.shouldUseNightVision(true)) {
            shaderinstance.safeGetUniform("ThermalImaging").set(0)
            Triple(179, 255, 179)
        } else {
            shaderinstance.safeGetUniform("ThermalImaging").set(0)
            Triple(255, 255, 255)
        }

        val isMonochrome = TACZExpandsClient.isMonochrome(true)
        shaderinstance.safeGetUniform("Monochrome").set(if (isMonochrome) 1 else 0)

        shaderinstance.apply()
        val f: Float = p_83972_.toFloat()
        val f1: Float = p_83973_.toFloat()
        val f2: Float = target.viewWidth.toFloat() / target.width.toFloat()
        val f3: Float = target.viewHeight.toFloat() / target.height.toFloat()
        val tesselator = Tesselator.getInstance()
        val bufferbuilder = tesselator.builder
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)

        bufferbuilder.vertex(0.0, f1.toDouble(), 0.0).uv(0.0f, 0.0f).color(r, g, b, 255).endVertex()
        bufferbuilder.vertex(f.toDouble(), f1.toDouble(), 0.0).uv(f2, 0.0f).color(r, g, b, 255).endVertex()
        bufferbuilder.vertex(f.toDouble(), 0.0, 0.0).uv(f2, f3).color(r, g, b, 255).endVertex()
        bufferbuilder.vertex(0.0, 0.0, 0.0).uv(0.0f, f3).color(r, g, b, 255).endVertex()
        BufferUploader.draw(bufferbuilder.end())
        shaderinstance.clear()
        RenderSystem.depthFunc(originalDepthFunc)
        GlStateManager._depthMask(true)
        GlStateManager._colorMask(true, true, true, true)
    }

    private fun _blitToScreen(p_83972_: Int, p_83973_: Int, p_83974_: Boolean, target: RenderTarget) {
        RenderSystem.assertOnRenderThread()
        GlStateManager._colorMask(true, true, true, false)
        GlStateManager._enableDepthTest()
        GlStateManager._depthMask(false)
        GlStateManager._viewport(0, 0, p_83972_, p_83973_)
        if (p_83974_) {
            GlStateManager._disableBlend()
        }

        val minecraft = Minecraft.getInstance()
        val shaderinstance = minecraft.gameRenderer.blitShader
        shaderinstance.setSampler("DiffuseSampler", target.colorTextureId)
        val matrix4f = (Matrix4f()).setOrtho(0.0f, p_83972_.toFloat(), p_83973_.toFloat(), 0.0f, 1000.0f, 3000.0f)
        RenderSystem.setProjectionMatrix(matrix4f, VertexSorting.ORTHOGRAPHIC_Z)
        if (shaderinstance.MODEL_VIEW_MATRIX != null) {
            shaderinstance.MODEL_VIEW_MATRIX!!.set((Matrix4f()).translation(0.0f, 0.0f, -2000.0f))
        }

        if (shaderinstance.PROJECTION_MATRIX != null) {
            shaderinstance.PROJECTION_MATRIX!!.set(matrix4f)
        }

        shaderinstance.apply()
        val f = p_83972_.toFloat()
        val f1 = p_83973_.toFloat()
        val f2 = target.viewWidth.toFloat() / target.width.toFloat()
        val f3 = target.viewHeight.toFloat() / target.height.toFloat()
        val tesselator = RenderSystem.renderThreadTesselator()
        val bufferbuilder = tesselator.getBuilder()
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR)
        bufferbuilder.vertex(0.0, f1.toDouble(), 0.0).uv(0.0f, 0.0f).color(255, 255, 255, 255).endVertex()
        bufferbuilder.vertex(f.toDouble(), f1.toDouble(), 0.0).uv(f2, 0.0f).color(255, 255, 255, 255).endVertex()
        bufferbuilder.vertex(f.toDouble(), 0.0, 0.0).uv(f2, f3).color(255, 255, 255, 255).endVertex()
        bufferbuilder.vertex(0.0, 0.0, 0.0).uv(0.0f, f3).color(255, 255, 255, 255).endVertex()
        BufferUploader.draw(bufferbuilder.end())
        shaderinstance.clear()
        GlStateManager._depthMask(true)
        GlStateManager._colorMask(true, true, true, true)
    }

    fun doEntityOutline(levelRenderer: LevelRenderer) {
        if (levelRenderer.shouldShowEntityOutlines()) {
            val window = Minecraft.getInstance().window
            RenderSystem.enableBlend()
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ZERO,
                GlStateManager.DestFactor.ONE
            )
            val target = levelRenderer.entityTarget()
            val shouldEnableStencil = TACZExpandsClient.isAdvancedRendering() && !TACZExpandsClient.patchMainTargetDraw
            if (target != null) {
                if (shouldEnableStencil) {
                    RenderHelper.enableItemEntityStencilTest()
                    RenderSystem.stencilFunc(GL11.GL_EQUAL, 0, 0xff)
                    RenderSystem.stencilOp(GL11.GL_KEEP, GL11.GL_KEEP, GL11.GL_KEEP)
                }
                _blitToScreen(window.width, window.height, false, target)
                if (shouldEnableStencil) {
                    RenderSystem.stencilFunc(GL11.GL_ALWAYS, 0, 0xff)
                    RenderHelper.disableItemEntityStencilTest()
                }
            }
            RenderSystem.disableBlend()
            RenderSystem.defaultBlendFunc()
        }
    }

    fun fitStencil(source: RenderTarget, sample: RenderTarget) {
        if (source.isStencilEnabled != sample.isStencilEnabled) {
            if (sample.isStencilEnabled) {
                source.enableStencil()
            } else {
                (source as IAccessorRenderTarget).setStencilEnabled(false)
                source.resize(source.viewWidth, source.viewHeight, Minecraft.ON_OSX)
            }
        }
    }

    fun patchBedrockModel(pojo: BedrockModelPOJO) {

        pojo.geometryModelNew?.bones?.let {
            patchBones(it)
        }

        pojo.geometryModelLegacy?.bones?.let {
            patchBones(it)
        }
    }

    fun patchBones(list: MutableList<BonesItem>) {
        mirror(list, "laser_pos", "module_pos")
        mirror(list, "laser_default", "module_default")
        copy(list, "refit_view", "refit_module_view")
    }

    fun copy(list: MutableList<BonesItem>, old: String, new: String) {
        list.firstOrNull { it.name == old }?.let { oldBones ->
            if (list.firstOrNull { it.name == new } != null) return@let

            val newBones = BonesItem()

            newBones as IAccessorBonesItem

            newBones.cubes = oldBones.cubes
            newBones.name = new
            newBones.pivot = cloneList(oldBones.pivot)
            newBones.rotation = cloneList(oldBones.rotation)
            newBones.parent = oldBones.parent
            newBones.isMirror = oldBones.isMirror

            list.add(newBones)
        }
    }

    fun mirror(list: MutableList<BonesItem>, old: String, new: String) {
        list.firstOrNull { it.name == old }?.let { oldBones ->
            if (list.firstOrNull { it.name == new } != null) return@let

            val newBones = BonesItem()

            newBones as IAccessorBonesItem

            newBones.cubes = oldBones.cubes
            newBones.name = new
            newBones.pivot = cloneList(oldBones.pivot)
            newBones.rotation = cloneList(oldBones.rotation)
            newBones.parent = oldBones.parent
            newBones.isMirror = oldBones.isMirror

            if (newBones.pivot != null && newBones.pivot.size > 0) {
                newBones.pivot[0] = -newBones.pivot[0]
            }

            if (newBones.rotation != null && newBones.rotation.size >= 3) {
                newBones.rotation[1] = -newBones.rotation[1]
                newBones.rotation[2] = -newBones.rotation[2]
            }

            list.add(newBones)
        }
    }

    fun <T> cloneList(old: MutableList<T>?): MutableList<T>? {
        if (old == null) return null
        return old.toMutableList()
    }


}