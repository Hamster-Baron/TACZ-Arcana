package group.taczexpands.client.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.gui.GunRefitScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

@Mixin(value = GunRefitScreen.class, remap = false)
public class MixinGunRefitScreen {
    @Shadow
    @Final
    public static int ICON_UV_SIZE;


    @Inject(method = "getSlotTextureXOffset", at = @At("HEAD"), cancellable = true)
    private static void getSlotTextureXOffset(ItemStack gunItem, AttachmentType attachmentType, CallbackInfoReturnable<Integer> cir) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        } else if (!iGun.allowAttachmentType(gunItem, attachmentType)) {
            return;
        }

        cir.setReturnValue(innerGetSlotTextureXOffset(gunItem, attachmentType));
    }

    @Unique
    private static int innerGetSlotTextureXOffset(ItemStack gunItem, AttachmentType attachmentType) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return -1;
        }
        if (!iGun.allowAttachmentType(gunItem, attachmentType)) {
            return ICON_UV_SIZE * 6;
        }
        switch (attachmentType) {
            case GRIP -> {
                return 0;
            }
            case LASER -> {
                return ICON_UV_SIZE;
            }
            case MUZZLE -> {
                return ICON_UV_SIZE * 2;
            }
            case SCOPE -> {
                return ICON_UV_SIZE * 3;
            }
            case STOCK -> {
                return ICON_UV_SIZE * 4;
            }
            case EXTENDED_MAG -> {
                return ICON_UV_SIZE * 5;
            }
        }
        return -1;
    }
}
