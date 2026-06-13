package group.taczexpands.client.mixin;

import com.google.common.collect.Lists;
import com.tacz.guns.api.client.animation.AnimationController;
import com.tacz.guns.api.client.animation.ObjectAnimation;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateContext;
import com.tacz.guns.api.client.animation.statemachine.AnimationStateMachine;
import com.tacz.guns.client.animation.statemachine.GunAnimationStateContext;
import group.taczexpands.client.accessor.IAccessorAnimationStateContext;
import group.taczexpands.client.accessor.IAccessorGunAnimationStateContext;
import group.taczexpands.client.accessor.IAccessorObjectAnimationRunner;
import group.taczexpands.client.gui.GunContextManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.util.ArrayList;

@Mixin(value = AnimationStateContext.class, remap = false)
public class MixinAnimationStateContext implements IAccessorAnimationStateContext {
    @Unique
    private static final ArrayList<String> taczexpands$bypassList = Lists.newArrayList("idle", "run_start", "run", "run_hold", "run_end", "walk_aiming", "walk_aiming_2", "walk_forward", "walk_sideway", "walk_backward", "ADS_up", "ADS_down", "melee_bayonet_1", "melee_bayonet_2", "melee_bayonet_3", "slide", "slide_back", "slide_idle", "melee_push", "melee_stock");
    @Unique
    public final ArrayList<Integer> taczexpands$extraTracks = new ArrayList<>();

    @Override
    public ArrayList<Integer> taczexpands$getExtraTracks() {
        return taczexpands$extraTracks;
    }

    @Inject(method = "runAnimation", at = @At("TAIL"), locals = LocalCapture.CAPTURE_FAILHARD)
    public void runAnimation(String name, int track, boolean blending, int playType, float transitionTime, CallbackInfo ci, AnimationStateMachine stateMachine, ObjectAnimation.PlayType pt) {
        if ((Object) this instanceof GunAnimationStateContext gunContext) {
            if (name.contains("reload")) {
                var modifier = GunExtras.INSTANCE.getReloadTimeModifier(Minecraft.getInstance().player, gunContext.getNbtAccessor().nbt());

                var runner = stateMachine.getAnimationController().getAnimation(track);
                if (runner != null) {
                    ((IAccessorObjectAnimationRunner) runner).taczexpands$setSpeed(1.0f / modifier);
                    var newRunner = runner.getTransitionTo();
                    if (newRunner != null) {
                        ((IAccessorObjectAnimationRunner) newRunner).taczexpands$setSpeed(1.0f / modifier);
                    }
                }

            } else if (name.contains("aim") || name.contains("ads")) {
                var modifier = GunExtras.INSTANCE.getAimTimeModifier(Minecraft.getInstance().player, gunContext.getNbtAccessor().nbt());

                var runner = stateMachine.getAnimationController().getAnimation(track);
                if (runner != null) {
                    ((IAccessorObjectAnimationRunner) runner).taczexpands$setSpeed(1.0f / modifier);
                    var newRunner = runner.getTransitionTo();
                    if (newRunner != null) {
                        ((IAccessorObjectAnimationRunner) newRunner).taczexpands$setSpeed(1.0f / modifier);
                    }
                }

            } else if (name.contains("bolt")) {
                var modifier = GunExtras.INSTANCE.getBoltTimeModifier(Minecraft.getInstance().player, gunContext.getNbtAccessor().nbt());

                var runner = stateMachine.getAnimationController().getAnimation(track);
                if (runner != null) {
                    ((IAccessorObjectAnimationRunner) runner).taczexpands$setSpeed(1.0f / modifier);
                    var newRunner = runner.getTransitionTo();
                    if (newRunner != null) {
                        ((IAccessorObjectAnimationRunner) newRunner).taczexpands$setSpeed(1.0f / modifier);
                    }
                }

            } else if (name.contains("draw")) {
                var modifier = GunExtras.INSTANCE.getDrawTimeModifier(Minecraft.getInstance().player, gunContext.getNbtAccessor().nbt());

                var runner = stateMachine.getAnimationController().getAnimation(track);
                if (runner != null) {
                    ((IAccessorObjectAnimationRunner) runner).taczexpands$setSpeed(1.0f / modifier);
                    var newRunner = runner.getTransitionTo();
                    if (newRunner != null) {
                        ((IAccessorObjectAnimationRunner) newRunner).taczexpands$setSpeed(1.0f / modifier);
                    }
                }

            }
        }
    }

    @Redirect(method = "runAnimation", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/animation/AnimationController;runAnimation(ILjava/lang/String;Lcom/tacz/guns/api/client/animation/ObjectAnimation$PlayType;F)V"))
    public void runAnimation(AnimationController instance, int track, String animationName, ObjectAnimation.PlayType playType, float transitionTimeS) {
        var redirectName = GunContextManager.INSTANCE.getRedirect(animationName);
        if (redirectName != null) {
            animationName = redirectName;
        }

        if (!taczexpands$bypassList.contains(animationName)) {
            if ((Object) this instanceof GunAnimationStateContext gunContext) {
                var item = ((IAccessorGunAnimationStateContext) gunContext).taczexpands$getCurrentItem();
                if (item == null) {
                    instance.runAnimation(track, animationName, playType, transitionTimeS);
                    return;
                }
                if (!GunExtras.INSTANCE.getUsingUnderBarrel(item)) {
                    instance.runAnimation(track, animationName, playType, transitionTimeS);
                    return;
                }
                if (animationName.startsWith("sub_")) {
                    instance.runAnimation(track, animationName, playType, transitionTimeS);
                    return;
                }
                instance.runAnimation(track, "sub_" + animationName, playType, transitionTimeS);
                return;
            }
        }

        instance.runAnimation(track, animationName, playType, transitionTimeS);
    }

}
