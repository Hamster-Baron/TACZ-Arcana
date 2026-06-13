package group.taczexpands.common.mixin;

import com.google.common.base.Preconditions;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import group.taczexpands.common.accessor.IAccessorAttachmentData;
import group.taczexpands.common.accessor.IAccessorBulletData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(value = CommonAttachmentIndex.class, remap = false)
public class MixinCommonAttachmentIndex {
    @ModifyVariable(method = "checkData", at = @At("STORE"), ordinal = 0)
    private static AttachmentData hookCheckData(AttachmentData attachmentData) {
        if (attachmentData != null) {
            var holder = IAccessorAttachmentData.getExtraHolder(attachmentData);
            if (holder != null) {
                if (holder.underBarrel != null) {
                    Preconditions.checkArgument(holder.underBarrel.gunId != null, "gun id is empty");
                }

                if (holder.ammoTypes != null) {
                    holder.ammoTypes.forEach(extraAmmo -> {
                        Preconditions.checkArgument(extraAmmo.getAmmoId() != null, "ammo id is empty");
                        Preconditions.checkArgument(extraAmmo.getAmmoAmount() >= 1, "ammo count must >= 1");
                        int[] extendedMagAmmoAmount = extraAmmo.getExtendedMagAmmoAmount();
                        Preconditions.checkArgument(extendedMagAmmoAmount == null || extendedMagAmmoAmount.length >= 3, "extended_mag_ammo_amount size must is 3");
                        taczexpands$checkBulletData(extraAmmo.getBulletData());
                    });

                }

            }
        }
        return attachmentData;
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
