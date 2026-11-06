package group.taczexpands.client.mixin;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.SoundData;
import com.tacz.guns.client.sound.SoundPlayManager;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundPlayManager.class, remap = false)
public class MixinSoundPlayManager {
    @Inject(method = "playShootSound", at = @At("HEAD"))
    private static void hookPlayShootSound(LivingEntity entity, GunDisplayInstance gunIndex, GunData gunData, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playSilenceSound", at = @At("HEAD"))
    private static void hookPlaySilenceSound(LivingEntity entity, GunDisplayInstance gunIndex, GunData gunData, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playDryFireSound", at = @At("HEAD"))
    private static void hookPlayDryFireSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playReloadSound", at = @At("HEAD"))
    private static void hookPlayReloadSound(LivingEntity entity, GunDisplayInstance display, boolean noAmmo, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playInspectSound", at = @At("HEAD"))
    private static void hookPlayInspectSound(LivingEntity entity, GunDisplayInstance display, boolean noAmmo, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playBoltSound", at = @At("HEAD"))
    private static void hookPlayBoltSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playDrawSound", at = @At("HEAD"))
    private static void hookPlayDrawSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playPutAwaySound", at = @At("HEAD"))
    private static void hookPlayPutAwaySound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playFireSelectSound", at = @At("HEAD"))
    private static void hookPlayFireSelectSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playMeleeBayonetSound", at = @At("HEAD"))
    private static void hookPlayMeleeBayonetSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playMeleePushSound", at = @At("HEAD"))
    private static void hookPlayMeleePushSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playMeleeStockSound", at = @At("HEAD"))
    private static void hookPlayMeleeStockSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playHeadHitSound", at = @At("HEAD"))
    private static void hookPlayHeadHitSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playFleshHitSound", at = @At("HEAD"))
    private static void hookPlayFleshHitSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Inject(method = "playKillSound", at = @At("HEAD"))
    private static void hookPlayKillSound(LivingEntity entity, GunDisplayInstance gunIndex, CallbackInfo ci) {
        saveEntity(entity);
    }

    @Redirect(method = "playShootSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayShootSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playSilenceSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlaySilenceSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playDryFireSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayDryFireSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playReloadSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayReloadSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playInspectSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayInspectSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playBoltSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayBoltSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playDrawSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayDrawSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playPutAwaySound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayPutAwaySound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playFireSelectSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayFireSelectSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playMeleeBayonetSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayMeleeBayonetSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playMeleePushSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayMeleePushSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playMeleeStockSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayMeleeStockSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playHeadHitSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayHeadHitSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playFleshHitSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayFleshHitSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Redirect(method = "playKillSound", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/client/resource/GunDisplayInstance;getSounds(Ljava/lang/String;)Lcom/tacz/guns/client/resource/pojo/display/SoundData;"))
    private static SoundData redirectPlayKillSound(GunDisplayInstance instance, String name) {
        return hookGunDisplay(instance, name);
    }

    @Unique
    private static LivingEntity taczexpands$savedEntity = null;

    @Unique
    private static void saveEntity(LivingEntity entity) {
        taczexpands$savedEntity = entity;
    }

    @Unique
    private static SoundData hookGunDisplay(GunDisplayInstance instance, String name) {
        var entity = taczexpands$savedEntity;
        taczexpands$savedEntity = null;

        if (entity == null) return instance.getSounds(name);
        var item = entity.getMainHandItem();
        if (item.isEmpty() || GunExtras.INSTANCE.getUnderBarrel(item) == null || GunExtras.INSTANCE.getUsingUnderBarrel(item))
            return instance.getSounds(name);
        var displayId = GunExtras.INSTANCE.getSoundGunDisplayId(item);
        if (displayId == null) return instance.getSounds(name);
        var optional = TimelessAPI.getGunDisplay(displayId, GunExtras.INSTANCE.getGunId(item));
        if (optional.isPresent()) {
            var display = optional.get();
            var overrideSound = display.getSounds(name);
            return overrideSound == null ? instance.getSounds(name) : overrideSound;
        }
        return instance.getSounds(name);
    }
}
