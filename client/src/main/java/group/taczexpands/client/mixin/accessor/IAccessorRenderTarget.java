package group.taczexpands.client.mixin.accessor;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderTarget.class)
public interface IAccessorRenderTarget {
    @Accessor("stencilEnabled")
    void setStencilEnabled(boolean enabled);
}
