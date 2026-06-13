package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.tacz.guns.client.model.IFunctionalRenderer;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.functional.TextShowRender;
import com.tacz.guns.client.model.papi.PapiManager;
import com.tacz.guns.client.resource.pojo.display.gun.TextShow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.apache.commons.lang3.StringUtils;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = TextShowRender.class, remap = false)
public class MixinTextShowRender {
    @Shadow
    @Final
    private TextShow textShow;

    @Shadow
    @Final
    private ItemStack gunStack;

    @Shadow
    @Final
    private BedrockModel bedrockModel;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void taczexpands$rewriteRender(PoseStack poseStack, VertexConsumer vertexBuffer, ItemDisplayContext transformType, int light, int overlay, CallbackInfo ci) {
        ci.cancel();

        if (!transformType.firstPerson()) {
            return;
        }
        String text = PapiManager.getTextShow(this.textShow.getTextKey(), this.gunStack);
        if (StringUtils.isBlank(text)) {
            return;
        }
        poseStack.mulPose(Axis.ZP.rotationDegrees(180f));
        Matrix3f normal = new Matrix3f(poseStack.last().normal());
        Matrix4f pose = new Matrix4f(poseStack.last().pose());

        this.bedrockModel.delegateRender((poseStack1, vertexBuffer1, transformType1, light1, overlay1) -> {
            Font font = Minecraft.getInstance().font;
            boolean shadow = textShow.isShadow();
            int color = textShow.getColorInt();
            float scale = textShow.getScale();
            int packLight = LightTexture.pack(textShow.getTextLight(), textShow.getTextLight());
            int width = font.width(text);
            int xOffset;
            switch (textShow.getAlign()) {
                case CENTER -> xOffset = width / 2;
                case RIGHT -> xOffset = width;
                default -> xOffset = 0;
            }

            PoseStack poseStack2 = new PoseStack();
            poseStack2.last().normal().mul(normal);
            poseStack2.last().pose().mul(pose);
            poseStack2.scale(2 / 300f * scale, -2 / 300f * scale, -2 / 300f);

            MultiBufferSource.BufferSource bufferSource = Minecraft.getInstance().renderBuffers().bufferSource();

            font.drawInBatch(text, -xOffset, -font.lineHeight / 2f, color, shadow, poseStack2.last().pose(), bufferSource, Font.DisplayMode.NORMAL, 0, packLight);
            bufferSource.endBatch();
        });
    }
}
