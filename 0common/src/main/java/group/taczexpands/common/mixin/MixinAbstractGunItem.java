package group.taczexpands.common.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.GunTabType;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.api.item.builder.GunItemBuilder;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.inventory.tooltip.GunTooltip;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.accessor.IAccessorMiscPOJO;
import group.taczexpands.common.manager.PackAllowAttachmentsModifyManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.core.NonNullList;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.*;

@Mixin(value = AbstractGunItem.class, remap = false)
public abstract class MixinAbstractGunItem extends Item {
    public MixinAbstractGunItem(Properties pProperties) {
        super(pProperties);
    }

    @Unique
    private ResourceLocation taczexpands$hookAmmoId = null;

    @Inject(method = "lambda$dropAllAmmo$3", at = @At(value = "INVOKE_ASSIGN", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoId()Lnet/minecraft/resources/ResourceLocation;"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void hookDropAllAmmo(ItemStack gunItem, int ammoCount, Player player, CommonGunIndex index, CallbackInfo ci) {
        taczexpands$hookAmmoId = IAccessorGunData.getCurrentAmmoId(index.getGunData(), gunItem);
    }

    @ModifyVariable(method = "lambda$dropAllAmmo$3", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/TimelessAPI;getCommonAmmoIndex(Lnet/minecraft/resources/ResourceLocation;)Ljava/util/Optional;"))
    private ResourceLocation getCommonAmmoId(ResourceLocation ammoId) {
        if (taczexpands$hookAmmoId != null) {
            ResourceLocation id = taczexpands$hookAmmoId;
            taczexpands$hookAmmoId = null;
            return id;
        }
        return ammoId;
    }

    @Inject(method = "getTooltipImage", at = @At("HEAD"), remap = true, cancellable = true)
    public void hookGetTooltipImage(ItemStack stack, CallbackInfoReturnable<Optional<TooltipComponent>> cir) {
        cir.cancel();
        if (stack.getItem() instanceof IGun iGun) {
            Optional<CommonGunIndex> optional = TimelessAPI.getCommonGunIndex(iGun.getGunId(stack));
            if (optional.isPresent()) {
                CommonGunIndex gunIndex = optional.get();
                ResourceLocation ammoId = gunIndex.getGunData().getAmmoId();
                cir.setReturnValue(Optional.of(new GunTooltip(stack, iGun, ammoId, gunIndex)));
                return;
            }
        }
        cir.setReturnValue(Optional.empty());
    }

    @Redirect(method = "allowAttachment", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    public ResourceLocation hookAllowAttachmentGetGunId(IGun instance, ItemStack itemStack) {
        return GunExtras.INSTANCE.getGunId(itemStack);
    }

    @Redirect(method = "allowAttachmentType", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    public ResourceLocation hookAllowAttachmentTypeGetGunId(IGun instance, ItemStack itemStack) {
        return GunExtras.INSTANCE.getGunId(itemStack);
    }

    @Unique
    private static ItemStack taczexpands$gun = null;

    @Inject(method = "allowAttachmentType", at = @At("HEAD"))
    public void taczexpands$allowAttachmentType$store(ItemStack gun, AttachmentType type, CallbackInfoReturnable<Boolean> cir) {
        taczexpands$gun = gun;
    }

    @Redirect(method = "lambda$allowAttachmentType$4", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAllowAttachments()Ljava/util/List;"))
    private static @Nullable List<AttachmentType> taczexpands$allowAttachmentType$redirect(GunData instance) {
        var original = instance.getAllowAttachments();
        var iGun = IGun.getIGunOrNull(taczexpands$gun);
        if (iGun != null) {
            var gunId = GunExtras.INSTANCE.getGunId(taczexpands$gun);

            var extraAllowList = PackAllowAttachmentsModifyManager.INSTANCE.getAllowAttachments(gunId);

            if (extraAllowList != null && !extraAllowList.isEmpty()) {
                if (original == null) {
                    ((group.taczexpands.common.mixin.accessor.IAccessorGunData) instance).taczexpands$setAllowAttachments(new ArrayList<>());
                    original = instance.getAllowAttachments();
                }

                var finalAllowAttachments = original;
                extraAllowList.forEach(attachment -> {
                    try {
                        var attachmentType = AttachmentType.valueOf(attachment.toUpperCase(Locale.ROOT));
                        if (!finalAllowAttachments.contains(attachmentType)) {
                            finalAllowAttachments.add(attachmentType);
                        }
                    } catch (Exception e) {
                    }
                });
            }
        }

        return original;
    }

    @Inject(method = "lambda$fillItemCategory$5", at = @At("HEAD"), cancellable = true)
    private static void rewriteFillCondition(GunTabType type, NonNullList<ItemStack> stacks, Map.Entry<ResourceLocation, CommonGunIndex> entry, CallbackInfo ci) {
        ci.cancel();

        CommonGunIndex index = entry.getValue();
        GunData gunData = index.getGunData();

        if (IAccessorMiscPOJO.isHidden(index.getPojo())) {
            return;
        }

        if (IAccessorMiscPOJO.isMisc(index.getPojo())) {
            return;
        }

        String key = type.name().toLowerCase(Locale.US);
        String indexType = index.getType();
        if (key.equals(indexType)) {
            ItemStack itemStack = GunItemBuilder.create()
                    .setId(entry.getKey())
                    .setFireMode(gunData.getFireModeSet().get(0))
                    .setAmmoCount(gunData.getAmmoAmount())
                    .setHeatData(gunData.hasHeatData())
                    .setAmmoInBarrel(true)
                    .setCount(1)
                    .build();
            stacks.add(itemStack);
        }
    }


    @Override
    public int getBarWidth(ItemStack stack) {
        var gunExtra = IAccessorGunData.getExtraHolder(stack);
        if (gunExtra == null) return 13;
        var maxDamage = gunExtra.durability;
        if (maxDamage <= 0) return 13;

        return Math.round(13.0F - (float) GunExtras.INSTANCE.getDurabilityDamage(stack) * 13.0F / (float) maxDamage);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        var gunExtra = IAccessorGunData.getExtraHolder(stack);
        if (gunExtra == null) return Mth.hsvToRgb(1.0f / 3.0F, 1.0F, 1.0F);
        var maxDamage = gunExtra.durability;
        if (maxDamage <= 0) return Mth.hsvToRgb(1.0f / 3.0F, 1.0F, 1.0F);
        float f = Math.max(0.0F, ((float) maxDamage - (float) GunExtras.INSTANCE.getDurabilityDamage(stack)) / (float) maxDamage);
        return Mth.hsvToRgb(f / 3.0F, 1.0F, 1.0F);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        var iGun = IGun.getIGunOrNull(stack);
        if (iGun == null) return false;
        var gunIndex = TimelessAPI.getCommonGunIndex(iGun.getGunId(stack)).orElse(null);
        if (gunIndex == null || gunIndex.getGunData() == null) return false;
        var gunExtra = IAccessorGunData.getExtraHolder(gunIndex.getGunData());

        return gunExtra.showVanillaDamageBar && gunExtra.durability > 0 && GunExtras.INSTANCE.getDurabilityDamage(stack) > 0;
    }
}
