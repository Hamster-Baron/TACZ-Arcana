package group.taczexpands.common.mixin;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.modifier.AttachmentPropertyManager;
import com.tacz.guns.resource.modifier.custom.WeightModifier;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import com.tacz.guns.resource.pojo.data.attachment.Modifier;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.AttachmentDataUtils;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.nbt.GunExtras;
import group.taczexpands.common.util.ExtensionsKt;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Mixin(value = AttachmentDataUtils.class, remap = false)
public abstract class MixinAttachmentDataUtils {
    @Unique
    private static ItemStack taczexpands$itemStack = null;

    @Unique
    private static GunData taczexpands$gunData = null;

    @Shadow
    public static int getMagExtendLevel(ItemStack gunItem, GunData gunData) {
        return 0;
    }

    @Unique
    private static AttachmentData taczexpands$emptuAttachmentData = new AttachmentData();


    @Inject(method = "getAllAttachmentData", at = @At("HEAD"), cancellable = true)
    private static void getAllAttachmentData(ItemStack gunItem, GunData gunData, Consumer<AttachmentData> dataConsumer, CallbackInfo ci) {
        ci.cancel();

        taczexpands$storeArgGetAllAttachmentData(gunItem, gunData, dataConsumer, ci);
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        }
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) {
                continue;
            }
            ResourceLocation attachmentId = taczexpands$hookIDGetAllAttachmentData(iGun, gunItem, type);
            if (DefaultAssets.isEmptyAttachmentId(attachmentId)) {
                continue;
            }
            AttachmentData attachmentData = gunData.getExclusiveAttachments().get(attachmentId);
            if (attachmentData == null) {
                var index = TimelessAPI.getCommonAttachmentIndex(attachmentId).orElse(null);
                if (index != null) {
                    attachmentData = index.getData();
                }

            }
            if (attachmentData != null) {
                if (ExtensionsKt.isLaser(type) && !GunExtras.INSTANCE.getLaser(gunItem)) {
                    attachmentData = taczexpands$emptuAttachmentData;
                }
                dataConsumer.accept(attachmentData);
            }

        }


        var currentAmmo = IAccessorGunData.getCurrentAmmo(gunData, gunItem);
        if (currentAmmo != null) {
            var hiddenAttachment = currentAmmo.getHiddenAttachment();
            if (hiddenAttachment != null) {
                dataConsumer.accept(hiddenAttachment);
            }
        }
    }


    @Unique
    private static void taczexpands$storeArgGetAllAttachmentData(ItemStack gunItem, GunData gunData, Consumer<AttachmentData> dataConsumer, CallbackInfo ci) {
        taczexpands$gunData = gunData;
    }

    @Unique
    private static ResourceLocation taczexpands$hookIDGetAllAttachmentData(IGun instance, ItemStack itemStack, AttachmentType attachmentType) {
        var result = instance.getAttachmentId(itemStack, attachmentType);
        if (DefaultAssets.isEmptyAttachmentId(result) && IAccessorGunData.getExtraHolder(taczexpands$gunData).applyBuiltinAttachmentsModifiers) {
            result = instance.getBuiltInAttachmentId(itemStack, attachmentType);
        }
        return result;
    }

    @Inject(method = "getMagExtendLevel", at = @At("HEAD"))
    private static void storeArgGetMagExtendLevel(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<Integer> cir) {
        taczexpands$gunData = gunData;
    }

    @Redirect(method = "getMagExtendLevel", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getAttachmentId(Lnet/minecraft/world/item/ItemStack;Lcom/tacz/guns/api/item/attachment/AttachmentType;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation hookIDGetMagExtendLevel(IGun instance, ItemStack itemStack, AttachmentType attachmentType) {
        if (ExtensionsKt.isLaser(attachmentType) && !GunExtras.INSTANCE.getLaser(itemStack)) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        var result = instance.getAttachmentId(itemStack, attachmentType);
        if (DefaultAssets.isEmptyAttachmentId(result) && IAccessorGunData.getExtraHolder(taczexpands$gunData).applyBuiltinAttachmentsModifiers) {
            result = instance.getBuiltInAttachmentId(itemStack, attachmentType);
        }
        return result;
    }


    @Unique
    private static ResourceLocation taczexpands$hookIDGetWightWithAttachment(IGun instance, ItemStack itemStack, AttachmentType attachmentType) {
        var result = instance.getAttachmentId(itemStack, attachmentType);
        if (DefaultAssets.isEmptyAttachmentId(result) && IAccessorGunData.getExtraHolder(taczexpands$gunData).applyBuiltinAttachmentsModifiers) {
            result = instance.getBuiltInAttachmentId(itemStack, attachmentType);
        }
        return result;
    }

    @Inject(method = "getWightWithAttachment", at = @At("HEAD"), cancellable = true)
    private static void rewriteGetWightWithAttachment(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<Double> cir) {
        cir.setReturnValue(taczexpands$getWightWithAttachment(gunItem, gunData));
    }

    @Unique
    private static double taczexpands$getWightWithAttachment(ItemStack gunItem, GunData gunData) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return gunData.getWeight();
        }

        List<Modifier> modifiers = new ArrayList<>();
        for (AttachmentType type : AttachmentType.values()) {
            ResourceLocation id = taczexpands$hookIDGetWightWithAttachment(iGun, gunItem, type);
            AttachmentData attachmentData = gunData.getExclusiveAttachments().get(id);
            if (attachmentData != null) {
                var m = attachmentData.getModifier().get(WeightModifier.ID);
                if (m != null && m.getValue() instanceof Modifier modifier) {
                    modifiers.add(modifier);
                } else {
                    Modifier modifier = new Modifier();
                    modifier.setAddend(attachmentData.getWeight());
                    modifiers.add(modifier);
                }
            } else {
                TimelessAPI.getCommonAttachmentIndex(id).ifPresent(index -> {
                    var m = index.getData().getModifier().get(WeightModifier.ID);
                    if (m != null && m.getValue() instanceof Modifier modifier) {
                        modifiers.add(modifier);
                    } else {
                        Modifier modifier = new Modifier();
                        modifier.setAddend(index.getData().getWeight());
                        modifiers.add(modifier);
                    }
                });
            }
        }

        var currentAmmo = IAccessorGunData.getCurrentAmmo(gunData, gunItem);
        if (currentAmmo != null) {
            var hiddenAttachment = currentAmmo.getHiddenAttachment();
            if (hiddenAttachment != null) {
                var m = hiddenAttachment.getModifier().get(WeightModifier.ID);
                if (m != null && m.getValue() instanceof Modifier modifier) {
                    modifiers.add(modifier);
                } else {
                    Modifier modifier = new Modifier();
                    modifier.setAddend(hiddenAttachment.getWeight());
                    modifiers.add(modifier);
                }
            }
        }

        return AttachmentPropertyManager.eval(modifiers, gunData.getWeight());
    }

    @Inject(method = "getModifiers", at = @At("HEAD"))
    private static void storeArgGetModifiers(ItemStack gunItem, GunData gunData, String id, CallbackInfoReturnable<List<Modifier>> cir) {
        taczexpands$gunData = gunData;
    }

    @Redirect(method = "getModifiers", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getAttachmentId(Lnet/minecraft/world/item/ItemStack;Lcom/tacz/guns/api/item/attachment/AttachmentType;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation hookIDGetModifiers(IGun instance, ItemStack itemStack, AttachmentType attachmentType) {
        if (ExtensionsKt.isLaser(attachmentType) && !GunExtras.INSTANCE.getLaser(itemStack)) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        var result = instance.getAttachmentId(itemStack, attachmentType);
        if (DefaultAssets.isEmptyAttachmentId(result) && IAccessorGunData.getExtraHolder(taczexpands$gunData).applyBuiltinAttachmentsModifiers) {
            result = instance.getBuiltInAttachmentId(itemStack, attachmentType);
        }
        return result;
    }

    @Inject(method = "calcBooleanValue", at = @At("HEAD"))
    private static <T> void storeArgCalcBooleanValue(ItemStack gunItem, GunData gunData, String id, Class<T> clazz, AttachmentDataUtils.BooleanResolver<T> resolver, CallbackInfoReturnable<Boolean> cir) {
        taczexpands$gunData = gunData;
    }

    @Redirect(method = "calcBooleanValue", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getAttachmentId(Lnet/minecraft/world/item/ItemStack;Lcom/tacz/guns/api/item/attachment/AttachmentType;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation hookIDCalcBooleanValue(IGun instance, ItemStack itemStack, AttachmentType attachmentType) {
        if (ExtensionsKt.isLaser(attachmentType) && !GunExtras.INSTANCE.getLaser(itemStack)) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID;
        }
        var result = instance.getAttachmentId(itemStack, attachmentType);
        if (DefaultAssets.isEmptyAttachmentId(result) && IAccessorGunData.getExtraHolder(taczexpands$gunData).applyBuiltinAttachmentsModifiers) {
            result = instance.getBuiltInAttachmentId(itemStack, attachmentType);
        }
        return result;
    }

    @Inject(method = "isExplodeEnabled", at = @At("HEAD"))
    private static void storeArgIsExplodeEnabled(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<Boolean> cir) {
        taczexpands$itemStack = gunItem;
    }

    @Inject(method = "getArmorIgnoreWithAttachment", at = @At("HEAD"))
    private static void storeArgGetArmorIgnoreWithAttachment(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<Double> cir) {
        taczexpands$itemStack = gunItem;
    }

    @Inject(method = "getHeadshotMultiplier", at = @At("HEAD"))
    private static void storeArgGetHeadshotMultiplier(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<Double> cir) {
        taczexpands$itemStack = gunItem;
    }

    @Inject(method = "getDamageWithAttachment", at = @At("HEAD"))
    private static void storeArgGetDamageWithAttachment(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<Double> cir) {
        taczexpands$itemStack = gunItem;
    }

    @Inject(method = "getAmmoCountWithAttachment", at = @At("HEAD"))
    private static void storeArgGetAmmoCountWithAttachment(ItemStack gunItem, GunData gunData, CallbackInfoReturnable<Integer> cir) {
        taczexpands$itemStack = gunItem;
    }

    @Redirect(method = "isExplodeEnabled", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    private static BulletData hookGetBulletDataIsExplodeEnabled(GunData instance) {
        return IAccessorGunData.getCurrentBulletData(instance, taczexpands$itemStack);
    }

    @Redirect(method = "getArmorIgnoreWithAttachment", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    private static BulletData hookGetBulletDataGetArmorIgnoreWithAttachment(GunData instance) {
        return IAccessorGunData.getCurrentBulletData(instance, taczexpands$itemStack);
    }

    @Redirect(method = "getHeadshotMultiplier", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    private static BulletData hookGetBulletDataGetHeadshotMultiplier(GunData instance) {
        return IAccessorGunData.getCurrentBulletData(instance, taczexpands$itemStack);
    }

    @Redirect(method = "getDamageWithAttachment", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    private static BulletData hookGetBulletDataGetDamageWithAttachment(GunData instance) {
        return IAccessorGunData.getCurrentBulletData(instance, taczexpands$itemStack);
    }

    @Redirect(method = "getAmmoCountWithAttachment", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoAmount()I"))
    private static int hookGetAmmoCountGetAmmoCountWithAttachment(GunData instance) {
        return IAccessorGunData.getCurrentAmmoAmount(instance, taczexpands$itemStack);
    }

    @Redirect(method = "getAmmoCountWithAttachment", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getExtendedMagAmmoAmount()[I"))
    private static int[] hookGetExtendedMagAmmoAmountGetAmmoCountWithAttachment(GunData instance) {
        return IAccessorGunData.getCurrentExtendedMagAmmoAmount(instance, taczexpands$itemStack);
    }
}
