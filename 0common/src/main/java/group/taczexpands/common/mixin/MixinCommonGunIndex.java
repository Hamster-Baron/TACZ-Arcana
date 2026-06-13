package group.taczexpands.common.mixin;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.manager.PackAllowAttachmentsModifyManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Locale;

@Mixin(value = CommonGunIndex.class, remap = false)
public class MixinCommonGunIndex {
    @ModifyVariable(method = "checkData", at = @At("STORE"), ordinal = 0)
    private static GunData hookCheckData(GunData gunData) {
        if (gunData != null) {
            taczexpands$checkBulletData(gunData.getBulletData());
            var extraHolder = IAccessorGunData.getExtraHolder(gunData);
            if (extraHolder.underBarrel != null) {
                Preconditions.checkArgument(extraHolder.underBarrel.gunId != null, "gun id is empty");
            }

            var list = extraHolder.ammoTypes;
            if (list != null) {
                list.forEach(extraAmmo -> {
                    Preconditions.checkArgument(extraAmmo.getAmmoId() != null, "ammo id is empty");
                    Preconditions.checkArgument(extraAmmo.getAmmoAmount() >= 1, "ammo count must >= 1");
                    int[] extendedMagAmmoAmount = extraAmmo.getExtendedMagAmmoAmount();
                    Preconditions.checkArgument(extendedMagAmmoAmount == null || extendedMagAmmoAmount.length >= 3, "extended_mag_ammo_amount size must is 3");
                    taczexpands$checkBulletData(extraAmmo.getBulletData());
                });
            }

            Preconditions.checkArgument(extraHolder.durability >= 0, "durability is negative");
            Preconditions.checkArgument(extraHolder.damageProbability >= 0.0f && extraHolder.damageProbability <= 1.0f, "damage_probability is out of range [0.0f, 1.0f]");
        }

        return gunData;
    }


    @Unique
    private static void taczexpands$checkBulletData(BulletData bulletData) {
        Preconditions.checkArgument(bulletData != null, "bullet data is null");

    }

    private static boolean isInteger(String str) {
        if (str == null) {
            return false;
        }
        try {
            Integer.parseInt(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
