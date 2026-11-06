package group.taczexpands.client.mixin.accessor;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.class)
public interface IAccessorGlStateManager {
    @Accessor("BLEND")
    static GlStateManager.BlendState taczexpands$getBlend() {
        throw new AssertionError();
    }
}
