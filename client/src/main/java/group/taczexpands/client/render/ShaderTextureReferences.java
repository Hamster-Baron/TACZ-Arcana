package group.taczexpands.client.render;

import com.mojang.blaze3d.systems.RenderSystem;

/** Clears Minecraft's retained shader bindings before their GL texture is deleted. */
public final class ShaderTextureReferences {
    private ShaderTextureReferences() {}

    /** Called on the render thread before releasing a texture owner. */
    public static void clear(int textureId) {
        RenderSystem.assertOnRenderThread();
        if (textureId <= 0) return;
        // Minecraft 1.20.1 retains twelve shader IDs independently of GL bindings.
        for (int slot = 0; slot < 12; slot++) {
            if (RenderSystem.getShaderTexture(slot) == textureId) RenderSystem.setShaderTexture(slot, 0);
        }
    }
}
