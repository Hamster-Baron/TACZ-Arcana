package group.taczexpands.server.mixin;

import com.tacz.guns.item.ModernKineticGunItem;
import com.tacz.guns.resource.pojo.data.attachment.EffectData;
import group.taczexpands.server.event.MeleeHitEvent;
import group.taczexpands.server.event.MeleeKillEvent;
import group.taczexpands.server.util.ExtensionsKt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ModernKineticGunItem.class, remap = false)
public abstract class MixinModernKineticGunItem {
    @Shadow
    private static void doPerLivingHurt(LivingEntity user, LivingEntity target, float knockback, float damage, List<EffectData> effects) {
    }


    @Unique
    private static LivingEntity taczexpands$lastUser = null;
    @Unique
    private static float taczexpands$lastDamage = 0.0f;

    @Redirect(method = "doMelee", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/item/ModernKineticGunItem;doPerLivingHurt(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/LivingEntity;FFLjava/util/List;)V"))
    public void onMeleeDamage(LivingEntity user, LivingEntity living, float knockback, float realDamage, List<EffectData> effects) {
        if (user == living) return;
        var event = new MeleeHitEvent(user, living, realDamage);
        MinecraftForge.EVENT_BUS.post(event);
        taczexpands$lastUser = user;
        taczexpands$lastDamage = event.getDamage();
        doPerLivingHurt(user, living, knockback, event.getDamage(), effects);
    }

    @Inject(method = "doMelee", at = @At("HEAD"))
    public void onPreDoMelee(LivingEntity user, float gunDistance, float meleeDistance, float rangeAngle, float knockback, float damage, List<EffectData> effects, CallbackInfo ci) {
        taczexpands$lastUser = user;
    }

    @Redirect(method = "doMelee", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;getEntitiesOfClass(Ljava/lang/Class;Lnet/minecraft/world/phys/AABB;)Ljava/util/List;", remap = true))
    public List<LivingEntity> hookGetEntitiesOfClass(Level instance, Class aClass, AABB aabb) {
        var result = instance.getEntitiesOfClass(LivingEntity.class, aabb);
        var cursorEntity = ExtensionsKt.findTargetInLineOfSight(taczexpands$lastUser, 4.5, null);
        if (cursorEntity instanceof LivingEntity && !result.contains(cursorEntity)) {
            result.add((LivingEntity) cursorEntity);
        }
        return result;
    }

    @Redirect(method = "doPerLivingHurt", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;isAlive()Z", remap = true))
    private static boolean redirectIsAlive(LivingEntity living) {
        var result = living.isAlive();
        if (!result) {
            var event = new MeleeKillEvent(taczexpands$lastUser, living, taczexpands$lastDamage);
            MinecraftForge.EVENT_BUS.post(event);
        }
        return result;
    }
}
