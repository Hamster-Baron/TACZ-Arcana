package group.taczexpands.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockAnimatedModel;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.bedrock.ModelRendererWrapper;
import com.tacz.guns.client.model.functional.AttachmentRender;
import com.tacz.guns.client.model.functional.BeamRenderer;
import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BedrockVersion;
import com.tacz.guns.compat.ar.ARCompat;
import group.taczexpands.client.accessor.IAccessorBedrockGunModel;
import group.taczexpands.client.compat.CompatHelper;
import group.taczexpands.client.compat.iris.IrisCompat;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.client.override.BeamRendererOverride;
import group.taczexpands.client.util.RenderHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;
import java.util.EnumMap;
import java.util.List;

@Mixin(value = BedrockGunModel.class, remap = false)
public abstract class MixinBedrockGunModel extends BedrockAnimatedModel implements IAccessorBedrockGunModel {
    @Shadow
    @Final
    private EnumMap<AttachmentType, ItemStack> currentAttachmentItem;
    @Shadow
    @Nullable
    protected List<BedrockPart> scopePosPath;
    @Shadow
    private ItemStack currentGunItem;
    @Unique
    private List<BedrockPart> taczexpands$subIronSightPath = null;

    @Unique
    private List<BedrockPart> taczexpands$hookPath = null;

    public MixinBedrockGunModel(BedrockModelPOJO pojo, BedrockVersion version) {
        super(pojo, version);
    }

    @Inject(method = "cacheOtherPath", at = @At("TAIL"))
    public void onCacheOtherPath(CallbackInfo ci) {
        taczexpands$subIronSightPath = this.getPath(modelMap.get("sub_iron_view"));
        taczexpands$hookPath = this.getPath(modelMap.get("hook"));
    }

    @Override
    public List<BedrockPart> taczexpands$getSubIronSightPath() {
        return taczexpands$subIronSightPath;
    }

    @Override
    public List<BedrockPart> taczexpands$getHookPath() {
        return taczexpands$hookPath;
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/client/model/functional/BeamRenderer;renderLaserBeam(Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Ljava/util/List;)V"))
    public void redirectRenderLaserBeam(ItemStack item, PoseStack poseStack, ItemDisplayContext context, List<BedrockPart> path) {
        if (ClientConfig.INSTANCE.getEnableAdvancedBeamRendering().get()) {
            BeamRendererOverride.renderLaserBeam(item, item, poseStack, context, path);
        } else {
            BeamRenderer.renderLaserBeam(item, poseStack, context, path);
        }
    }

    @Redirect(method = "render", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/systems/RenderSystem;clear(IZ)V"))
    public void redirectClear(int p_69422_, boolean p_69423_) {
        if (p_69422_ != GL11.GL_STENCIL_BUFFER_BIT) RenderSystem.clear(p_69422_, p_69423_);
    }


}
