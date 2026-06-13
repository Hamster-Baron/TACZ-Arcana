package group.taczexpands.client.mixin;

import com.tacz.guns.entity.EntityKineticBullet;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ItemInHandRenderer.class)
public class MixinItemInHandRenderer {
    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderHandsWithItems", at = @At("HEAD"), cancellable = true)
    public void onPreRenderHandsWithItems(CallbackInfo ci) {
        if(minecraft.cameraEntity != null && minecraft.cameraEntity instanceof EntityKineticBullet) {
            ci.cancel();
        }
    }
}
