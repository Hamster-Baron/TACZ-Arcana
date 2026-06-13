package group.taczexpands.client.mixin;

import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
import com.tacz.guns.resource.modifier.custom.AdsModifier;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Collections;
import java.util.List;

@Mixin(value = AdsModifier.class, remap = false)
public class MixinAdsModifier {


    @Inject(method = "getPropertyDiagramsData", at = @At("HEAD"), cancellable = true)
    public void hookGetPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty, CallbackInfoReturnable<List<IAttachmentModifier.DiagramsData>> cir) {
        cir.cancel();

        float aimTime = gunData.getAimTime();
        Float modifier = GunExtras.INSTANCE.getAimTimeModifier(Minecraft.getInstance().player, gunItem);
        if (modifier != null) {
            aimTime = aimTime * modifier;
        }
        float adsTime = cacheProperty.<Float>getCache(AdsModifier.ID);
        if (modifier != null) {
            adsTime = adsTime * modifier;
        }
        float adsTimeModifier = adsTime - aimTime;

        String titleKey = "gui.tacz.gun_refit.property_diagrams.ads";
        String positivelyString = String.format("%.2fs §c(+%.2f)", aimTime, adsTimeModifier);
        String negativelyString = String.format("%.2fs §a(%.2f)", aimTime, adsTimeModifier);
        String defaultString = String.format("%.2fs", aimTime);
        boolean positivelyBetter = false;

        IAttachmentModifier.DiagramsData diagramsData = new IAttachmentModifier.DiagramsData(aimTime, adsTimeModifier, adsTimeModifier, titleKey, positivelyString, negativelyString, defaultString, positivelyBetter);
        cir.setReturnValue(Collections.singletonList(diagramsData));
    }
}
