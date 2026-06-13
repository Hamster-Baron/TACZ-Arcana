package group.taczexpands.client.mixin;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import group.taczexpands.client.accessor.IAccessorAmmoParticle;
import group.taczexpands.client.accessor.IAccessorBedrockModelPOJO;
import group.taczexpands.client.accessor.IAccessorGunDisplay;
import group.taczexpands.client.util.RenderHelper;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunDisplayInstance.class, remap = false)
public class MixinGunDisplayInstance implements IAccessorGunDisplay {
    @Shadow
    private @Nullable AmmoParticle particle;
    @Unique
    private boolean taczexpands$hideMuzzleFlash = false;

    @Unique
    private boolean taczexpands$hideShell = false;

    @Override
    public boolean taczexpands$getHideMuzzleFlash() {
        return taczexpands$hideMuzzleFlash;
    }

    @Override
    public boolean taczexpands$getHideShell() {
        return taczexpands$hideShell;
    }

    @Inject(method = "<init>", at = @At("TAIL"))
    private void taczexpands$init(ResourceLocation displayId, GunDisplay display, CallbackInfo ci) {
        this.taczexpands$hideMuzzleFlash = IAccessorGunDisplay.getHideMuzzleFlash(display);
        this.taczexpands$hideShell = IAccessorGunDisplay.getHideShell(display);
    }

    @Inject(method = "checkGunAmmo", at = @At("RETURN"))
    private void taczexpands$postCheckGunAmmo(GunDisplay display, CallbackInfo ci) {
        if (this.particle == null) return;
        var current = IAccessorAmmoParticle.getNext(this.particle);
        while (current != null) {
            try {
                String name = current.getName();
                if (StringUtils.isNoneBlank()) {
                    current.setParticleOptions(ParticleArgument.readParticle(new StringReader(name), BuiltInRegistries.PARTICLE_TYPE.asLookup()));
                    Preconditions.checkArgument(current.getCount() > 0, "particle count must be greater than 0");
                    Preconditions.checkArgument(current.getLifeTime() > 0, "particle life time must be greater than 0");
                }
            } catch (CommandSyntaxException e) {
                e.fillInStackTrace();
            }

            current = IAccessorAmmoParticle.getNext(current);
        }
    }

    @Redirect(method = "checkTextureAndModel", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/resource/ClientAssetsManager;getBedrockModelPOJO(Lnet/minecraft/resources/ResourceLocation;)Lcom/tacz/guns/client/resource/pojo/model/BedrockModelPOJO;"))
    public BedrockModelPOJO taczexpands$checkTextureAndModel$redirectGetBedrockModelPOJO(ClientAssetsManager instance, ResourceLocation id) {
        var pojo = instance.getBedrockModelPOJO(id);
        var pojoAccessor = (IAccessorBedrockModelPOJO) pojo;
        if (!pojoAccessor.taczexpands$isPatched()) {
            RenderHelper.INSTANCE.patchBedrockModel(pojo);
            pojoAccessor.taczexpands$setPatched(true);
        }
        return pojo;
    }
}
