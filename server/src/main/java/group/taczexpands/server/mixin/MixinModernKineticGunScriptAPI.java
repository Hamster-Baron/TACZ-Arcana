package group.taczexpands.server.mixin;

import com.tacz.guns.api.entity.IGunOperator;
import com.tacz.guns.api.item.gun.AbstractGunItem;
import com.tacz.guns.api.item.gun.FireMode;
import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.util.CycleTaskHelper;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.nbt.GunExtras;
import group.taczexpands.server.context.Context;
import group.taczexpands.server.event.BulletSpawnEvent;
import group.taczexpands.server.override.CycleTaskHelperOverride;
import group.taczexpands.server.skill.SkillManager;
import group.taczexpands.server.skill.TriggerType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.BooleanSupplier;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class MixinModernKineticGunScriptAPI {
    @Shadow
    private AbstractGunItem abstractGunItem;

    @Shadow
    private ItemStack itemStack;

    @Shadow
    private LivingEntity shooter;

    @Redirect(method = "lambda$shootOnce$2", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/Level;addFreshEntity(Lnet/minecraft/world/entity/Entity;)Z", remap = true))
    public boolean onBulletSpawn(Level instance, Entity entity) {
        var result = instance.addFreshEntity(entity);
        if (entity instanceof EntityKineticBullet bullet) {
            MinecraftForge.EVENT_BUS.post(new BulletSpawnEvent(bullet));
        }
        return result;
    }

    @Redirect(method = "shootOnce", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/util/CycleTaskHelper;addCycleTask(Ljava/util/function/BooleanSupplier;JI)V"))
    public void redirectAddCycleTask(BooleanSupplier task, long periodMs, int cycles) {
        if (!(this.shooter instanceof ServerPlayer)) {
            CycleTaskHelper.addCycleTask(task, periodMs, cycles);
            return;
        }

        FireMode fireMode = this.abstractGunItem.getFireMode(this.itemStack);
        if (fireMode != FireMode.BURST) {
            CycleTaskHelper.addCycleTask(task, periodMs, cycles);
            return;
        }

        SkillManager.INSTANCE.trigger(TriggerType.ON_BURST_SHOOT, new Context((ServerPlayer) this.shooter), 0);
        CycleTaskHelperOverride.addCycleTask(task, periodMs, cycles, () -> {
            SkillManager.INSTANCE.triggerReverse(TriggerType.ON_BURST_SHOOT, new Context((ServerPlayer) this.shooter), 0);
        });
    }

    @Redirect(method = "shootOnce", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/BulletData;getBulletAmount()I"))
    public int redirectGetBulletAmount(BulletData bulletData) {
        var override = GunExtras.INSTANCE.getOverrideBulletAmount(itemStack);
        if (override > 0) {
            return override;
        }
        return bulletData.getBulletAmount();
    }

    @Inject(method = "lambda$shootOnce$2", at = @At("HEAD"), cancellable = true)
    public void preShootOnce(boolean consumeAmmo, GunData gunData, int bulletAmount, BulletData bulletData, IGunOperator gunOperator, float shotDamageMultiplier, float processedSpeed, float inaccuracy, int soundDistance, boolean useSilenceSound, CallbackInfoReturnable<Boolean> cir) {
        var extra = IAccessorGunData.getExtraHolder(itemStack);
        if (extra == null) return;
        if (extra.durability <= 0) return;
        var currentDamage = GunExtras.INSTANCE.getDurabilityDamage(itemStack);
        if (currentDamage >= extra.durability) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "lambda$shootOnce$2", at = @At("TAIL"), cancellable = true)
    public void postShootOnce(boolean consumeAmmo, GunData gunData, int bulletAmount, BulletData bulletData, IGunOperator gunOperator, float shotDamageMultiplier, float processedSpeed, float inaccuracy, int soundDistance, boolean useSilenceSound, CallbackInfoReturnable<Boolean> cir) {
        var extra = IAccessorGunData.getExtraHolder(itemStack);
        if (extra == null) return;
        if (extra.durability <= 0) return;
        if (Math.random() >= extra.damageProbability) return;
        var currentDamage = GunExtras.INSTANCE.getDurabilityDamage(itemStack);
        currentDamage++;
        GunExtras.INSTANCE.setDurabilityDamage(itemStack, currentDamage);
        if (currentDamage >= extra.durability) {
            cir.setReturnValue(false);
        }

        if (shooter instanceof ServerPlayer) {
            ((ServerPlayer) shooter).inventoryMenu.broadcastChanges();
        }
    }
}
