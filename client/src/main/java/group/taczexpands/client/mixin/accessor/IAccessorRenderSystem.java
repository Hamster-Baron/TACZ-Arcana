package group.taczexpands.client.mixin.accessor;

import com.mojang.blaze3d.systems.RenderSystem;
import org.joml.Vector3f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RenderSystem.class)
public interface IAccessorRenderSystem {
    @Accessor("shaderLightDirections")
    static Vector3f[] getShaderLightDirections() {
        return null;
    }
}
