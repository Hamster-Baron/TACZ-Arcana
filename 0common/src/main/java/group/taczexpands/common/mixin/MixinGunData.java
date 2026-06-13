package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.data.GunExtraHolder;
import group.taczexpands.common.manager.PackAllowAttachmentsModifyManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.List;

@Mixin(value = GunData.class, remap = false)
public class MixinGunData implements IAccessorGunData {
    @Unique
    @SerializedName("extras")
    private GunExtraHolder taczexpands$extras = new GunExtraHolder();


    @Inject(method = "getShootInterval", at = @At(value = "RETURN"), cancellable = true, locals = LocalCapture.CAPTURE_FAILHARD)
    public void modifyRPM(LivingEntity shooter, FireMode fireMode, ItemStack gunStack, CallbackInfoReturnable<Long> cir, int rpm, AttachmentCacheProperty cacheProperty, IGun iGun) {
        ItemStack mainHand = shooter.getMainHandItem();
        var modifier = GunExtras.INSTANCE.getRPMModifier(shooter, mainHand);

        int newRpm = Mth.clamp((int) (rpm * modifier), 1, 1200);
        cir.setReturnValue(60_000L / newRpm);

    }

    @NotNull
    @Override
    public GunExtraHolder taczexpands$getExtraHolder() {
        return taczexpands$extras;
    }
}
