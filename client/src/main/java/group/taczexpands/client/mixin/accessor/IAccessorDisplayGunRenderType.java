package group.taczexpands.client.mixin.accessor;

import com.tacz.guns.client.renderer.block.DisplayGunRenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.RenderType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import java.util.Map;

@Mixin(value = DisplayGunRenderType.class, remap = false)
public interface IAccessorDisplayGunRenderType {
    @Accessor("CACHE") static Map<ResourceLocation, RenderType> taczexpands$getCache() { throw new AssertionError(); }
}
