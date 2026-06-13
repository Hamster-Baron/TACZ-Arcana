package group.taczexpands.client.mixin;

import group.taczexpands.client.accessor.IAccessorCompositeState;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(RenderType.CompositeState.class)
public class MixinCompositeState implements IAccessorCompositeState {

    @Shadow @Final private RenderStateShard.EmptyTextureStateShard textureState;

    @Override
    public RenderStateShard.EmptyTextureStateShard taczexpands$getTexture() {
        return this.textureState;
    }
}
