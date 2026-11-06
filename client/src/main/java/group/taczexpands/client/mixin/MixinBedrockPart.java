package group.taczexpands.client.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import group.taczexpands.client.gui.GunContextManager;
import group.taczexpands.client.gui.ScopeManager;
import group.taczexpands.client.render.scope.ScopeLensComposition;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import javax.annotation.Nullable;

@Mixin(value = BedrockPart.class, remap = false)
public class MixinBedrockPart {
    @Shadow
    @Final
    @Nullable
    public String name;

    @Shadow
    public boolean visible;

    @Shadow
    public float offsetX;
    @Shadow
    public float offsetY;
    @Shadow
    public float offsetZ;
    @Unique
    private float taczexpands$storedOffsetX = 0f;
    @Unique
    private float taczexpands$storedOffsetY = 0f;
    @Unique
    private float taczexpands$storedOffsetZ = 0f;

    @ModifyVariable(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V",
            at = @At("HEAD"), argsOnly = true)
    private VertexConsumer taczexpands$trackingConsumer(VertexConsumer original,
            @Local(argsOnly = true) MultiBufferSource buffers) {
        return name != null && name.startsWith("util_track_")
                ? ScopeLensComposition.trackingConsumer(original, buffers) : original;
    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("HEAD"), cancellable = true)
    public void onPreRender(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha, @Nullable MultiBufferSource bufferSource, CallbackInfo ci) {
        if (this.name == null) return;

        if (this.name.startsWith("util_api_")) {
            this.visible = GunContextManager.INSTANCE.shouldRenderScopeElement(this.name);
        }

        if (GunContextManager.INSTANCE.isAmmoModelElementName(this.name)) {
            this.visible = GunContextManager.INSTANCE.shouldRenderAmmoModelElement((BedrockPart) (Object) this);
        }

        if (this.name.startsWith("util_track_")) {
            taczexpands$storedOffsetX = this.offsetX;
            taczexpands$storedOffsetY = this.offsetY;
            taczexpands$storedOffsetZ = this.offsetZ;

            var result = ScopeManager.INSTANCE.onRenderTrack((BedrockPart) (Object) this, poseStack);
            if (this.name.startsWith("util_track_hidedefault_"))
                this.visible = result;
        }

        if (this.name.startsWith("drop_point")) {
            taczexpands$storedOffsetX = this.offsetX;
            taczexpands$storedOffsetY = this.offsetY;
            taczexpands$storedOffsetZ = this.offsetZ;

            var result = ScopeManager.INSTANCE.onRenderDropPoint((BedrockPart) (Object) this, poseStack);
            this.visible = result;
        }

    }

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/world/item/ItemDisplayContext;Lcom/mojang/blaze3d/vertex/VertexConsumer;IIFFFFLnet/minecraft/client/renderer/MultiBufferSource;)V", at = @At("TAIL"), cancellable = true)
    public void onPostRender(PoseStack poseStack, ItemDisplayContext transformType, VertexConsumer consumer, int light, int overlay, float red, float green, float blue, float alpha, @Nullable MultiBufferSource bufferSource, CallbackInfo ci) {
        if (this.name != null) {
            if (this.name.startsWith("util_track_") || this.name.startsWith("drop_point")) {
                this.offsetX = taczexpands$storedOffsetX;
                this.offsetY = taczexpands$storedOffsetY;
                this.offsetZ = taczexpands$storedOffsetZ;
            }
        }
    }
}
