package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import group.taczexpands.client.accessor.IAccessorBedrockGunModel;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = FirstPersonRenderGunEvent.class, remap = false)
public abstract class MixinFirstPersonRenderGunEvent {
    @Unique
    private static ItemStack taczexpands$lastStack = null;



    @Inject(method = "applyFirstPersonPositioningTransform", at = @At("HEAD"))
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float aimingProgress, float refitScreenOpeningProgress, CallbackInfo ci) {
        taczexpands$lastStack = stack;
        ScopeManager.INSTANCE.setLastScopeViewPath(null);
    }

    @Redirect(method = "applyFirstPersonPositioningTransform", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;getIronSightPath()Ljava/util/List;"))
    private static List<BedrockPart> redirectGetIronSightPath(BedrockGunModel instance) {
        if (taczexpands$lastStack != null) {
            if (GunExtras.INSTANCE.getUsingUnderBarrel(taczexpands$lastStack)) {
                var subIronSightPath = ((IAccessorBedrockGunModel) instance).taczexpands$getSubIronSightPath();
                if (subIronSightPath != null) {
                    taczexpands$lastStack = null;
                    return subIronSightPath;
                }
            }
            taczexpands$lastStack = null;
        }
        return instance.getIronSightPath();
    }

    @Redirect(method = "applyFirstPersonPositioningTransform", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockAttachmentModel;getScopeViewPath(I)Ljava/util/List;"))
    private static List<BedrockPart> onGetScopeView(BedrockAttachmentModel instance, int viewSwitchCount) {
        var view = instance.getScopeViewPath(viewSwitchCount);
        ScopeManager.INSTANCE.setLastScopeViewPath(view);
        return view;
    }
}
