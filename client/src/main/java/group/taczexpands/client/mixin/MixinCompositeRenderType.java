package group.taczexpands.client.mixin;

import group.taczexpands.client.accessor.IAccessorCompositeState;
import group.taczexpands.client.accessor.IAccessorRenderType;
import group.taczexpands.client.accessor.IAccessorTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(targets = "net.minecraft.client.renderer.RenderType$CompositeRenderType")
public class MixinCompositeRenderType implements IAccessorRenderType {

    @Shadow @Final private RenderType.CompositeState state;

    @Override
    public ResourceLocation taczexpands$getTexture() {
        var texture = ((IAccessorCompositeState)(Object)this.state).taczexpands$getTexture();
        if(texture instanceof RenderStateShard.TextureStateShard stateShard) {
            return ((IAccessorTextureStateShard)(Object)stateShard).taczexpands$getTexture();
        }
        return null;
    }
}
