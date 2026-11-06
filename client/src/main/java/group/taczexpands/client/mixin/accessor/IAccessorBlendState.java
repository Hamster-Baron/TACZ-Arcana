package group.taczexpands.client.mixin.accessor;

import com.mojang.blaze3d.platform.GlStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(GlStateManager.BlendState.class)
public interface IAccessorBlendState {
    @Accessor("mode") GlStateManager.BooleanState taczexpands$getMode();
    @Accessor("srcRgb") int taczexpands$getSrcRgb();
    @Accessor("dstRgb") int taczexpands$getDstRgb();
    @Accessor("srcAlpha") int taczexpands$getSrcAlpha();
    @Accessor("dstAlpha") int taczexpands$getDstAlpha();
}
