package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.UnshadedVertexConsumer;
import com.tacz.guns.compat.ar.ARCompat;

import group.taczexpands.client.accessor.IAccessorSurfacePart;
import group.taczexpands.client.render.surface.SurfaceVertexConsumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Render live faces through part traversal so their changing textures bypass AR's immutable mesh cache. */
@Mixin(value = BedrockPart.class, remap = false, priority = 900)
public class MixinSurfacePart implements IAccessorSurfacePart {
    @Unique private boolean taczexpands$surfaceDynamic;

    @Override
    public void taczexpands$setSurfaceDynamic(boolean dynamic) {
        taczexpands$surfaceDynamic = dynamic;
    }

    @Override
    public boolean taczexpands$isSurfaceDynamic() {
        return taczexpands$surfaceDynamic;
    }

    // Apply after TacZ's priority-1000 mixin so this HEAD rewrite precedes its injected callback.
    @ModifyVariable(method = "compile", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private VertexConsumer taczexpands$liveConsumer(VertexConsumer consumer) {
        if (!taczexpands$surfaceDynamic) return consumer;
        var delegate = consumer instanceof UnshadedVertexConsumer unshaded
                ? unshaded.getDelegate() : consumer;
        return ARCompat.isAccelerated(delegate) ? new SurfaceVertexConsumer(consumer) : consumer;
    }
}
