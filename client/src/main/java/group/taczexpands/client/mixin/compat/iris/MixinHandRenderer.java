package group.taczexpands.client.mixin.compat.iris;

import com.mojang.blaze3d.vertex.PoseStack;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.override.BeamRendererOverride;
import net.irisshaders.iris.pathways.HandRenderer;
import net.irisshaders.iris.pipeline.WorldRenderingPipeline;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HandRenderer.class, remap = false)
public class MixinHandRenderer {
}
