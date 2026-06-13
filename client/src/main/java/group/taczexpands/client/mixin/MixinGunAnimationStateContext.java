package group.taczexpands.client.mixin;

import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import group.taczexpands.client.accessor.IAccessorGunAnimationStateContext;
import group.taczexpands.common.api.GunScriptAPICommon;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = GunAnimationStateContext.class, remap = false)
public class MixinGunAnimationStateContext implements IAccessorGunAnimationStateContext {

    @Shadow private ItemStack currentGunItem;

    @Override
    public ItemStack taczexpands$getCurrentItem() {
        return this.currentGunItem;
    }

    @Unique
    public int getPlayerScoreInt(String scoreName) {
        LocalPlayer shooter = Minecraft.getInstance().player;

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
        LocalPlayer shooter = Minecraft.getInstance().player;

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
        LocalPlayer shooter = Minecraft.getInstance().player;

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
}
