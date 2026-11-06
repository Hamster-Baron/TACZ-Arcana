package group.taczexpands.client.mixin;

import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.resource.index.ClientAttachmentVariantIndex;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;

import group.taczexpands.client.accessor.IAccessorSurfaceDisplay;
import group.taczexpands.client.render.surface.ModelTargets;
import group.taczexpands.client.render.surface.SurfaceConfig;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Replacement and overlay variants own their display/model bindings just like base attachments. */
@Mixin(value = ClientAttachmentVariantIndex.class, remap = false)
public class MixinSurfaceAttachmentVariant implements IAccessorSurfaceDisplay {
    @Shadow private AttachmentDisplay display;
    @Shadow private BedrockAttachmentModel attachmentModel;

    @Override
    public List<SurfaceConfig.Face> taczexpands$getSurfaceFaces() {
        return ((IAccessorSurfaceDisplay) display).taczexpands$getSurfaceFaces();
    }

    @Inject(
            method = "loadModelsIfNecessary",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lcom/tacz/guns/client/resource/index/ClientAttachmentVariantIndex;modelsLoaded:Z",
                            opcode = Opcodes.PUTFIELD))
    private void taczexpands$bindSurfaces(CallbackInfo ci) {
        ModelTargets.bind(attachmentModel, taczexpands$getSurfaceFaces());
    }
}
