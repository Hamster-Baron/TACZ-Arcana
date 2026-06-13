package group.taczexpands.client.mixin;

import group.taczexpands.client.accessor.IAccessorTextureStateShard;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Optional;

@Mixin(RenderStateShard.TextureStateShard.class)
public class MixinTextureStateShard implements IAccessorTextureStateShard {
    @Shadow @Final private Optional<ResourceLocation> texture;

    @Override
    public ResourceLocation taczexpands$getTexture() {
        return texture.orElse(null);
    }
}
