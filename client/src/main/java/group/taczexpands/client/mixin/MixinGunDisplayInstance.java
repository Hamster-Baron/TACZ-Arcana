package group.taczexpands.client.mixin;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;

import group.taczexpands.client.accessor.IAccessorAmmoParticle;
import group.taczexpands.client.accessor.IAccessorBedrockModelPOJO;
import group.taczexpands.client.accessor.IAccessorGunDisplay;
import group.taczexpands.client.accessor.IAccessorSurfaceDisplay;
import group.taczexpands.client.render.surface.ModelTargets;
import group.taczexpands.client.render.surface.SurfaceConfig;
import group.taczexpands.client.util.RenderHelper;
import group.taczexpands.common.TACZExpandsCommon;

import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

import javax.annotation.Nullable;

@Mixin(
        value = GunDisplayInstance.class,
        remap = false)
public class MixinGunDisplayInstance implements IAccessorGunDisplay, IAccessorSurfaceDisplay {
    @Shadow @Nullable private AmmoParticle particle;
    @Unique private boolean taczexpands$hideMuzzleFlash = false;
    @Unique private boolean taczexpands$hideShell = false;

    public boolean taczexpands$getHideMuzzleFlash() {
        return this.taczexpands$hideMuzzleFlash;
    }

    public boolean taczexpands$getHideShell() {
        return this.taczexpands$hideShell;
    }

    @Inject(
            method = "<init>",
            at = @At(value = "TAIL"))
    private void taczexpands$init(ResourceLocation displayId, GunDisplay display, CallbackInfo ci) {
        this.taczexpands$hideMuzzleFlash =
                IAccessorGunDisplay.getHideMuzzleFlash((GunDisplay) display);
        this.taczexpands$hideShell = IAccessorGunDisplay.getHideShell((GunDisplay) display);
    }

    @Inject(
            method = "checkGunAmmo",
            at = @At(value = "RETURN"))
    private void taczexpands$postCheckGunAmmo(GunDisplay display, CallbackInfo ci) {
        if (this.particle == null) {
            return;
        }
        AmmoParticle current = IAccessorAmmoParticle.getNext(this.particle);
        while (current != null) {
            try {
                String name = current.getName();
                if (StringUtils.isNoneBlank(name)) {
                    current.setParticleOptions(ParticleArgument.readParticle(new StringReader(name), BuiltInRegistries.PARTICLE_TYPE.asLookup()));
                    Preconditions.checkArgument(current.getCount() > 0, "particle count must be greater than 0");
                    Preconditions.checkArgument(current.getLifeTime() > 0, "particle life time must be greater than 0");
                }
            } catch (CommandSyntaxException e) {
                TACZExpandsCommon.Companion.getLOGGER().error(
                        "Failed to parse particle '{}' in gun display '{}'",
                        current.getName(), ((GunDisplayInstance) (Object) this).getDisplayId(), e);
            }
            current = IAccessorAmmoParticle.getNext(current);
        }
    }

    @Redirect(
            method = "checkTextureAndModel",
            at = @At(
                value = "INVOKE",
                target = "Lcom/tacz/guns/client/resource/ClientAssetsManager;getBedrockModelPOJO(Lnet/minecraft/resources/ResourceLocation;)Lcom/tacz/guns/client/resource/pojo/model/BedrockModelPOJO;"))
    public BedrockModelPOJO taczexpands$checkTextureAndModel$redirectGetBedrockModelPOJO(
            ClientAssetsManager instance, ResourceLocation id) {
        BedrockModelPOJO pojo = instance.getBedrockModelPOJO(id);
        IAccessorBedrockModelPOJO pojoAccessor = (IAccessorBedrockModelPOJO) pojo;
        if (!pojoAccessor.taczexpands$isPatched()) {
            RenderHelper.INSTANCE.patchBedrockModel(pojo);
            pojoAccessor.taczexpands$setPatched(true);
        }
        return pojo;
    }

    @Shadow @Final private GunDisplay display;
    @Shadow private BedrockGunModel gunModel;
    @Shadow private Pair<BedrockGunModel, ResourceLocation> lodModel;

    @Override
    public List<SurfaceConfig.Face> taczexpands$getSurfaceFaces() {
        return ((IAccessorSurfaceDisplay) display).taczexpands$getSurfaceFaces();
    }

    // Complete binding before the volatile flag publishes this model to rendering.
    @Inject(
            method = "loadModelIfNecessary",
            at = @At(
                value = "FIELD",
                target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;modelLoaded:Z",
                opcode = Opcodes.PUTFIELD))
    private void taczexpands$bindSurfaces(CallbackInfo ci) {
        ModelTargets.bind(gunModel, taczexpands$getSurfaceFaces());
    }

    @Inject(
            method = "loadLodIfNecessary",
            at = @At(
                value = "FIELD",
                target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;lodLoaded:Z",
                opcode = Opcodes.PUTFIELD))
    private void taczexpands$bindLodSurfaces(CallbackInfo ci) {
        if (lodModel != null) ModelTargets.bind(lodModel.getLeft(), taczexpands$getSurfaceFaces());
    }
}
