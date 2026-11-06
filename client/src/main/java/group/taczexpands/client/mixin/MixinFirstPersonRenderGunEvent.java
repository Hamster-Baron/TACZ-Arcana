package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.event.FirstPersonRenderGunEvent;
import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.resource.pojo.display.gun.AdsPath;
import group.taczexpands.client.accessor.IAccessorBedrockGunModel;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.common.nbt.GunExtras;
import java.util.List;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = FirstPersonRenderGunEvent.class, remap = false)
public abstract class MixinFirstPersonRenderGunEvent {
    @Unique
    private static ItemStack taczexpands$lastStack = null;

    @Inject(method = "applyFirstPersonPositioningTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/tacz/guns/client/model/BedrockGunModel;Lnet/minecraft/world/item/ItemStack;FFLcom/tacz/guns/client/resource/pojo/display/gun/AdsPath;)V", at = @At(value = "HEAD"))
    private static void applyFirstPersonPositioningTransform(PoseStack poseStack, BedrockGunModel model, ItemStack stack, float aimingProgress, float refitScreenOpeningProgress, AdsPath adsPath, CallbackInfo ci) {
        taczexpands$lastStack = stack;
        ScopeManager.INSTANCE.setLastScopeViewPath(null);
    }

    @Redirect(method = "applyFirstPersonPositioningTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/tacz/guns/client/model/BedrockGunModel;Lnet/minecraft/world/item/ItemStack;FFLcom/tacz/guns/client/resource/pojo/display/gun/AdsPath;)V", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockGunModel;getIronSightPath()Ljava/util/List;"))
    private static List<BedrockPart> redirectGetIronSightPath(BedrockGunModel instance) {
        if (taczexpands$lastStack != null) {
            List<BedrockPart> subIronSightPath;
            if (GunExtras.INSTANCE.getUsingUnderBarrel(taczexpands$lastStack) && (subIronSightPath = ((IAccessorBedrockGunModel)instance).taczexpands$getSubIronSightPath()) != null) {
                taczexpands$lastStack = null;
                return subIronSightPath;
            }
            taczexpands$lastStack = null;
        }
        return instance.getIronSightPath();
    }

    @Redirect(method = "applyFirstPersonPositioningTransform(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/tacz/guns/client/model/BedrockGunModel;Lnet/minecraft/world/item/ItemStack;FFLcom/tacz/guns/client/resource/pojo/display/gun/AdsPath;)V", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/BedrockAttachmentModel;getScopeViewPath(I)Ljava/util/List;"))
    private static List<BedrockPart> onGetScopeView(BedrockAttachmentModel instance, int viewSwitchCount) {
        List<BedrockPart> view = instance.getScopeViewPath(viewSwitchCount);
        ScopeManager.INSTANCE.setLastScopeModel(instance);
        ScopeManager.INSTANCE.setLastScopeViewPath(view);
        return view;
    }
}
