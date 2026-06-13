package group.taczexpands.common.mixin;

import com.tacz.guns.api.entity.ReloadState;
import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.attachment.EffectData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.GunReloadData;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(value = ModernKineticGunItem.class, remap = false)
public abstract class MixinModernKineticGunItem {
    @Shadow
    protected abstract void defaultReloadFinishing(ModernKineticGunScriptAPI api, boolean isTactical);


    @Shadow protected abstract void doMelee(LivingEntity user, float gunDistance, float meleeDistance, float rangeAngle, float knockback, float damage, List<EffectData> effects);

    @Inject(method = "defaultTickReload", at = @At("HEAD"), cancellable = true)
    public void defaultTickReload(ModernKineticGunScriptAPI api, CallbackInfoReturnable<ReloadState> cir) {
        cir.cancel();
        CommonGunIndex gunIndex = api.getGunIndex();
        GunData gunData = gunIndex.getGunData();
        GunReloadData reloadData = gunData.getReloadData();
        long countDown;
        ReloadState.StateType stateType;
        ReloadState.StateType oldStateType = ReloadState.StateType.values()[api.getReloadStateType()];
        long progressTime = api.getReloadTime();
        Float modifier = GunExtras.INSTANCE.getReloadTimeModifier(api.getShooter(), api.getShooter().getMainHandItem());
        if (modifier == null) {
            modifier = 1.0f;
        }
        if (oldStateType.isReloadingEmpty()) {
            long feedTime = (long) (reloadData.getFeed().getEmptyTime() * modifier * 1000);
            long finishingTime = (long) (reloadData.getCooldown().getEmptyTime() * modifier * 1000);
            if (progressTime < feedTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FEEDING;
                countDown = feedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.EMPTY_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
            }
        } else if (oldStateType.isReloadingTactical()) {
            long feedTime = (long) (reloadData.getFeed().getTacticalTime() * modifier * 1000);
            long finishingTime = (long) (reloadData.getCooldown().getTacticalTime() * modifier * 1000);
            if (progressTime < feedTime) {
                stateType = ReloadState.StateType.TACTICAL_RELOAD_FEEDING;
                countDown = feedTime - progressTime;
            } else if (progressTime < finishingTime) {
                stateType = ReloadState.StateType.TACTICAL_RELOAD_FINISHING;
                countDown = finishingTime - progressTime;
            } else {
                stateType = ReloadState.StateType.NOT_RELOADING;
                countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
            }
        } else {
            stateType = ReloadState.StateType.NOT_RELOADING;
            countDown = ReloadState.NOT_RELOADING_COUNTDOWN;
        }
        if (oldStateType == ReloadState.StateType.EMPTY_RELOAD_FEEDING && oldStateType != stateType) {
            this.defaultReloadFinishing(api, false);
        }
        if (oldStateType == ReloadState.StateType.TACTICAL_RELOAD_FEEDING && oldStateType != stateType) {
            this.defaultReloadFinishing(api, true);
        }
        ReloadState reloadState = new ReloadState();
        reloadState.setStateType(stateType);
        reloadState.setCountDown(countDown);
        cir.setReturnValue(reloadState);
    }

    @Inject(method = "defaultTickBolt", at = @At("HEAD"), cancellable = true)
    private void defaultTickBolt(ModernKineticGunScriptAPI api, CallbackInfoReturnable<Boolean> cir) {
        cir.cancel();
        GunData gunData = api.getGunIndex().getGunData();
        Float modifier = GunExtras.INSTANCE.getBoltTimeModifier(api.getShooter(), api.getShooter().getMainHandItem());
        if (modifier == null) {
            modifier = 1.0f;
        }
        long boltActionTime = (long) (gunData.getBoltActionTime() * modifier * 1000);
        float rawBoltFeedTime = gunData.getBoltFeedTime() * modifier;
        long boltFeedTime = rawBoltFeedTime == -1 ? boltActionTime : (long) (gunData.getBoltFeedTime() * modifier * 1000);
        if (api.getBoltTime() < boltFeedTime) {
            cir.setReturnValue(true);
        }
        if (!api.hasAmmoInBarrel()) {
            if (api.useInventoryAmmo()) {
                if (api.consumeAmmoFromPlayer(1) == 1) {
                    api.setAmmoInBarrel(true);
                }
            } else if (api.removeAmmoFromMagazine(1) != 0) {
                api.setAmmoInBarrel(true);
            }
        }
        cir.setReturnValue(api.getBoltTime() < boltActionTime);
    }
}
