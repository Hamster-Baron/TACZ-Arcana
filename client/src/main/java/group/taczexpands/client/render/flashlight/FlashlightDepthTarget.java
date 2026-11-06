package group.taczexpands.client.render.flashlight;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;
import org.lwjgl.opengl.GL30;
import java.nio.IntBuffer;

/** A framebuffer whose depth is sampled directly by the flashlight composition. */
public class FlashlightDepthTarget extends TextureTarget {
    public static final int SHADOW_SIZE = 1024;

    public FlashlightDepthTarget(int width, int height) {
        super(width, height, true, Minecraft.ON_OSX);
    }

    public static TextureTarget backup(int width, int height) {
        return new Backup(width, height);
    }

    protected int depthFormat() {
        return GL30.GL_DEPTH_COMPONENT32F;
    }

    private static final class Backup extends FlashlightDepthTarget {
        private Backup(int width, int height) {
            super(width, height);
        }

        @Override
        protected int depthFormat() {
            return GL11.GL_DEPTH_COMPONENT;
        }
    }

    @Override
    public void createBuffers(int width, int height, boolean clearError) {
        RenderSystem.assertOnRenderThreadOrInit();
        this.width = this.viewWidth = width;
        this.height = this.viewHeight = height;
        frameBufferId = GlStateManager.glGenFramebuffers();
        depthBufferId = TextureUtil.generateTextureId();
        RenderSystem.bindTexture(depthBufferId);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_NEAREST);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, GL12.GL_CLAMP_TO_EDGE);
        GL11.glTexParameteri(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, GL12.GL_CLAMP_TO_EDGE);
        if (isStencilEnabled()) {
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, GL30.GL_DEPTH32F_STENCIL8, width, height, 0,
                    GL30.GL_DEPTH_STENCIL, GL30.GL_FLOAT_32_UNSIGNED_INT_24_8_REV, (IntBuffer) null);
        } else {
            GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, depthFormat(), width, height, 0,
                    GL11.GL_DEPTH_COMPONENT, GL11.GL_FLOAT, (IntBuffer) null);
        }
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, frameBufferId);
        GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, isStencilEnabled()
                ? GL30.GL_DEPTH_STENCIL_ATTACHMENT : GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, depthBufferId, 0);
        GL11.glDrawBuffer(GL11.GL_NONE);
        GL11.glReadBuffer(GL11.GL_NONE);
        checkStatus();
        clear(clearError);
        unbindRead();
    }

    @Override
    public void bindRead() {
        RenderSystem.bindTexture(depthBufferId);
    }

    @Override
    public void clear(boolean clearError) {
        bindWrite(false);
        RenderSystem.clearDepth(1.0);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, clearError);
        unbindWrite();
    }
}
