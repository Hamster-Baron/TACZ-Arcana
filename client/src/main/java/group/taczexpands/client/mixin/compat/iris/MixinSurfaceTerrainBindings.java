package group.taczexpands.client.mixin.compat.iris;

import com.llamalad7.mixinextras.sugar.Local;
import group.taczexpands.client.compat.iris.IrisTerrainBindings;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Named binding at the builder method boundary, independent of createShader's invocation layout. */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gl.shader.GlProgram$Builder", remap = false)
public abstract class MixinSurfaceTerrainBindings {
    @Shadow @Final private ResourceLocation name;

    @ModifyVariable(method = "bindAttribute(Ljava/lang/String;I)Lme/jellysquid/mods/sodium/client/gl/shader/GlProgram$Builder;",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int taczexpands$surfaceAttribute(int location, @Local(argsOnly = true) String attribute) {
        return IrisTerrainBindings.isBasicPass()
                ? IrisTerrainBindings.location(name, attribute, location) : location;
    }
}
