package group.taczexpands.common.mixin;

import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Resolve the invocation's own stack, including concurrent client/server cache builds. */
@Mixin(remap = false, targets = {
    "com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier",
    "com.tacz.guns.resource.modifier.custom.ArmorIgnoreModifier",
    "com.tacz.guns.resource.modifier.custom.DamageModifier",
    "com.tacz.guns.resource.modifier.custom.EffectiveRangeModifier",
    "com.tacz.guns.resource.modifier.custom.HeadShotModifier",
    "com.tacz.guns.resource.modifier.custom.KnockbackModifier",
    "com.tacz.guns.resource.modifier.custom.PierceModifier",
    "com.tacz.guns.resource.modifier.custom.ExplosionModifier",
    "com.tacz.guns.resource.modifier.custom.IgniteModifier"
})
public class MixinInitOnlyModifiers {
    @Redirect(method = "initCache", at = @At(value = "INVOKE", target =
        "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    public BulletData hookGetBulletDataInitCache(GunData receiver, ItemStack gunStack, GunData invocationData) {
        // Redirect handlers can receive the enclosing method's arguments. No
        // singleton scratch field, ThreadLocal, allocation, or new public API.
        return IAccessorGunData.getCurrentBulletData(receiver, gunStack);
    }
}
