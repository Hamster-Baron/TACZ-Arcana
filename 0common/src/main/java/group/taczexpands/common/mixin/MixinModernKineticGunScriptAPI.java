package group.taczexpands.common.mixin;

import com.tacz.guns.item.ModernKineticGunScriptAPI;
import com.tacz.guns.resource.index.CommonGunIndex;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType;
import group.taczexpands.common.accessor.IAccessorGunData;
import group.taczexpands.common.api.GunScriptAPICommon;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = ModernKineticGunScriptAPI.class, remap = false)
public class MixinModernKineticGunScriptAPI {
    @Shadow
    private LivingEntity shooter;

    @Shadow
    private ItemStack itemStack;

    @ModifyArg(method = "lambda$shootOnce$2", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/gun/AbstractGunItem;doBulletSpread(Lcom/tacz/guns/entity/shooter/ShooterDataHolder;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/entity/projectile/Projectile;IFFFF)V"), index = 6)
    public float modifySpread(float value) {
        var inaccuracyType = InaccuracyType.getInaccuracyType(shooter);
        Float modifier = GunExtras.INSTANCE.getSpreadModifier(shooter, shooter.getMainHandItem(), inaccuracyType);

        if (modifier != null) {
            return value * modifier;
        }

        return value;
    }

    @Redirect(method = "lambda$shootOnce$2", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/pojo/data/gun/GunData;getAmmoId()Lnet/minecraft/resources/ResourceLocation;"))
    public ResourceLocation hookGetAmmoId(GunData instance) {
        return IAccessorGunData.getCurrentAmmoId(instance, itemStack);
    }

    @Redirect(method = "shootOnce", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/resource/index/CommonGunIndex;getBulletData()Lcom/tacz/guns/resource/pojo/data/gun/BulletData;"))
    public BulletData hookGetBulletData(CommonGunIndex instance) {
        return IAccessorGunData.getCurrentBulletData(instance.getGunData(), itemStack);
    }

    @Unique
    public float getReloadTimeModifier() {
        return GunExtras.INSTANCE.getReloadTimeModifier(shooter, itemStack);
    }

    @Unique
    public float getBoltTimeModifier() {
        return GunExtras.INSTANCE.getBoltTimeModifier(shooter, itemStack);
    }

    @Unique
    public int getPlayerScoreInt(String scoreName) {
        if (shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getGetPlayerScoreIntClientDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName);
            }
            return 0;
        } else {
            var delegate = GunScriptAPICommon.INSTANCE.getGetPlayerScoreIntServerDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName);
            }
            return 0;
        }
    }

    @Unique
    public float getPlayerScoreFloat(String scoreName) {
        if (shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getGetPlayerScoreFloatClientDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName);
            }
            return 0.0f;
        } else {
            var delegate = GunScriptAPICommon.INSTANCE.getGetPlayerScoreFloatServerDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName);
            }
            return 0.0f;
        }
    }

    @Unique
    public String getPlayerScoreString(String scoreName) {
        if (shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getGetPlayerScoreStringClientDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName);
            }
            return "";
        } else {
            var delegate = GunScriptAPICommon.INSTANCE.getGetPlayerScoreStringServerDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName);
            }
            return "";
        }
    }

    @Unique
    public boolean setPlayerScoreInt(String scoreName, int value) {
        if (!shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getSetPlayerScoreIntDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName, value);
            }
            return false;
        }
        return false;
    }

    @Unique
    public boolean setPlayerScoreFloat(String scoreName, float value) {
        if (!shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getSetPlayerScoreFloatDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName, value);
            }
            return false;
        }
        return false;
    }

    @Unique
    public boolean setPlayerScoreString(String scoreName, String value) {
        if (!shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getSetPlayerScoreStringDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, scoreName, value);
            }
            return false;
        }
        return false;
    }

    @Unique
    public boolean dispatchSignal(String signal, int duration) {
        if (!shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getDispatchSignalDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, signal, duration);
            }
            return false;
        }
        return false;
    }

    @Nullable
    @Unique
    public String parseExpression(String expression) {
        if (!shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getParseExpressionDelegate();
            if (delegate != null) {
                return delegate.invoke(shooter, expression);
            }
        }
        return null;
    }

    @Unique
    public void refreshVariable(String variable) {
        if (shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getRefreshVariableDelegate();
            if (delegate != null) {
                delegate.invoke(shooter, variable);
            }
        }
    }

    @Unique
    public void refreshAllVariable() {
        if (shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getRefreshAllVariableDelegate();
            if (delegate != null) {
                delegate.invoke(shooter);
            }
        }
    }

    @Unique
    public void modify(String name, String action, String expression) {
        if (!shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getModifyDelegate();
            if (delegate != null) {
                delegate.invoke(shooter, name, action, expression);
            }
        }
    }

    @Unique
    public void storeLockingTarget() {
        if (!shooter.level().isClientSide) {
            var delegate = GunScriptAPICommon.INSTANCE.getStoreLockingTarget();
            if (delegate != null) {
                delegate.invoke(shooter);
            }
        }
    }


}
