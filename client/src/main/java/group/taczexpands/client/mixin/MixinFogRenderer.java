package group.taczexpands.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import group.taczexpands.client.TACZExpandsClient;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(FogRenderer.class)
public class MixinFogRenderer {
}
