package group.taczexpands.client.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.client.tooltip.ClientGunTooltip;
import com.tacz.guns.item.GunTooltipPart;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.accessor.IAccessorGunIndexPOJO;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ClientGunTooltip.class, remap = false)
public abstract class MixinClientGunTooltip {
    @Shadow
    @Final
    private ItemStack gun;

    @Shadow
    protected abstract void getText();

    @Mutable
    @Shadow
    @Final
    private ItemStack ammo;

    @Shadow
    @Final
    private CommonGunIndex gunIndex;

    @Redirect(method = "getText", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/index/CommonGunIndex;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    public BulletData hookCommonGunIndexGetBulletData(CommonGunIndex instance) {
        return IAccessorGunData.getCurrentBulletData(instance.getGunData(), gun);
    }

    @Redirect(method = "getText", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    public BulletData hookGunDataGetBulletData(GunData instance) {
        return IAccessorGunData.getCurrentBulletData(instance, gun);
    }

    @Redirect(method = "getText", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/GunIndexPOJO;getTooltip()Ljava/lang/String;"))
    public String hookGetTooltip(GunIndexPOJO instance) {
        var optional = TimelessAPI.getCommonGunIndex(GunExtras.INSTANCE.getGunId(gun));
        if (optional.isPresent()) {
            return optional.get().getPojo().getTooltip();
        }
        return instance.getTooltip();
    }

    @Redirect(method = "getText", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/BulletData;getBulletAmount()I"))
    public int hookGetBulletAmount(BulletData instance) {
        if (gun != null) {
            var overrideAmount = GunExtras.INSTANCE.getOverrideBulletAmount(gun);
            if (overrideAmount > 0) {
                return overrideAmount;
            }
        }
        return instance.getBulletAmount();
    }

    @Redirect(method = "<init>", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/tooltip/ClientGunTooltip;getText()V"))
    public void onGetText(ClientGunTooltip instance) {
        this.ammo = AmmoItemBuilder.create().setId(IAccessorGunData.getCurrentAmmoId(gunIndex.getGunData(), gun)).build();
        getText();
    }

    @Inject(method = "shouldShow", at = @At("HEAD"), cancellable = true)
    public void taczexpands$preShouldShow(GunTooltipPart part, CallbackInfoReturnable<Boolean> cir) {
        var hideFlags = IAccessorGunIndexPOJO.getTooltipHideFlags(this.gunIndex.getPojo());
        if (hideFlags.contains(part.name().toLowerCase())) {
            cir.setReturnValue(false);
        }
    }
}
