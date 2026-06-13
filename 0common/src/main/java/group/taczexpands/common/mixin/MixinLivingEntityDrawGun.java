package group.taczexpands.common.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.entity.shooter.LivingEntityDrawGun;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LivingEntityDrawGun.class, remap = false)
public class MixinLivingEntityDrawGun {
    @Shadow
    @Final
    private LivingEntity shooter;

    @Unique
    ItemStack taczexpands$lastDrawItem = null;

    @Redirect(method = "getDrawCoolDown", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    public ResourceLocation storeGetDrawItemStack(IGun instance, ItemStack itemStack) {
        taczexpands$lastDrawItem = itemStack;
        return instance.getGunId(itemStack);
    }

    @Redirect(method = "lambda$getDrawCoolDown$0", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getDrawTime()F"))
    public float modifyDrawTime(GunData instance) {
        var drawTime = instance.getDrawTime();
        Float modifier = GunExtras.INSTANCE.getDrawTimeModifier(shooter, taczexpands$lastDrawItem);
        if (modifier != null) {
            return drawTime * modifier;
        }
        return drawTime;
    }

    @Unique
    private static LivingEntity taczexpands$lastShooter = null;

    @Unique
    private static ItemStack taczexpands$lastPutAwayItem = null;

    @Redirect(method = "updatePutAwayTime", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    public ResourceLocation storeGetPutAwayItemStack(IGun instance, ItemStack itemStack) {
        taczexpands$lastShooter = shooter;
        taczexpands$lastPutAwayItem = itemStack;
        return instance.getGunId(itemStack);
    }

    @Redirect(method = "lambda$updatePutAwayTime$1", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getPutAwayTime()F"))
    private static float modifyPutAwayTime(GunData instance) {
        var putAwayTime = instance.getPutAwayTime();
        Float modifier = GunExtras.INSTANCE.getDrawTimeModifier(taczexpands$lastShooter, taczexpands$lastPutAwayItem);
        if (modifier != null) {
            return putAwayTime * modifier;
        }
        return putAwayTime;
    }

}
