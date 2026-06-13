package group.taczexpands.client.mixin;

import com.tacz.guns.api.modifier.IAttachmentModifier;
import com.tacz.guns.resource.modifier.AttachmentCacheProperty;
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

import java.util.List;

@Mixin(remap = false, targets = {
        "com.tacz.guns.resource.modifier.custom.AmmoSpeedModifier",
        "com.tacz.guns.resource.modifier.custom.ArmorIgnoreModifier",
        "com.tacz.guns.resource.modifier.custom.DamageModifier",
        "com.tacz.guns.resource.modifier.custom.EffectiveRangeModifier",
        "com.tacz.guns.resource.modifier.custom.HeadShotModifier",
        "com.tacz.guns.resource.modifier.custom.KnockbackModifier",
        "com.tacz.guns.resource.modifier.custom.PierceModifier",
})
public class MixinModifiers {
    @Unique
    private ItemStack taczexpands$itemStack = null;

    @Inject(method = "getPropertyDiagramsData", at = @At("HEAD"))
    public void storeArgGetPropertyDiagramsData(ItemStack gunItem, GunData gunData, AttachmentCacheProperty cacheProperty, CallbackInfoReturnable<List<IAttachmentModifier.DiagramsData>> cir) {
        taczexpands$itemStack = gunItem;
    }

    @Redirect(method = "getPropertyDiagramsData", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    public BulletData hookGetBulletDataGetPropertyDiagramsData(GunData instance) {
        return IAccessorGunData.getCurrentBulletData(instance, taczexpands$itemStack);
    }
}
