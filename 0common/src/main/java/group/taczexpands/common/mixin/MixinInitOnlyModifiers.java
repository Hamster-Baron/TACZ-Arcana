package group.taczexpands.common.mixin;

import com.tacz.guns.api.modifier.CacheValue;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(remap = false, targets = {
        "com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier",
        "com.tacz.guns.resource.modifier.custom.ArmorIgnoreModifier",
        "com.tacz.guns.resource.modifier.custom.DamageModifier",
        "com.tacz.guns.resource.modifier.custom.EffectiveRangeModifier",
        "com.tacz.guns.resource.modifier.custom.HeadShotModifier",
        "com.tacz.guns.resource.modifier.custom.KnockbackModifier",
        "com.tacz.guns.resource.modifier.custom.PierceModifier",

        "com.tacz.guns.resource.modifier.custom.ExplosionModifier",
        "com.tacz.guns.resource.modifier.custom.IgniteModifier",
})
public class MixinInitOnlyModifiers {
    @Unique
    private ItemStack taczexpands$itemStack = null;

    @Inject(method = "initCache", at = @At("HEAD"))
    public void storeArgInitCache(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<CacheValue<Float>> cir) {
        taczexpands$itemStack = gunItem;
    }


    @Redirect(method = "initCache", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    public BulletData hookGetBulletDataInitCache(GunData instance) {
        return IAccessorGunData.getCurrentBulletData(instance, taczexpands$itemStack);
    }
}
