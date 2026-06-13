package group.taczexpands.client.mixin;

import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.functional.ShellRender;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ShellRender.class, remap = false)
public class MixinShellRender {
    @Shadow
    @Final
    private BedrockGunModel bedrockGunModel;

    @Redirect(method = "renderShell", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoId()Lnet/minecraft/resources/ResourceLocation;"))
    public ResourceLocation hookGetAmmoId(GunData instance) {
        return IAccessorGunData.getCurrentAmmoId(instance, bedrockGunModel.getCurrentGunItem());
    }
}
