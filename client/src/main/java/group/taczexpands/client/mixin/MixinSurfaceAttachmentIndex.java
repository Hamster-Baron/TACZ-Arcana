package group.taczexpands.client.mixin;

import com.tacz.guns.client.model.BedrockAttachmentModel;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import com.tacz.guns.client.resource.pojo.display.attachment.AttachmentDisplay;

import group.taczexpands.client.accessor.IAccessorSurfaceDisplay;
import group.taczexpands.client.render.surface.ModelTargets;
import group.taczexpands.client.render.surface.SurfaceConfig;

import net.minecraft.resources.ResourceLocation;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = ClientAttachmentIndex.class, remap = false)
public class MixinSurfaceAttachmentIndex implements IAccessorSurfaceDisplay {
    @Shadow private AttachmentDisplay display;
    @Shadow private BedrockAttachmentModel attachmentModel;
    @Shadow private Pair<BedrockAttachmentModel, ResourceLocation> lodModel;

    @Override
    public List<SurfaceConfig.Face> taczexpands$getSurfaceFaces() {
        return ((IAccessorSurfaceDisplay) display).taczexpands$getSurfaceFaces();
    }

    // Publish fully bound main and LOD models together through TacZ's volatile flag.
    @Inject(
            method = "loadModelsIfNecessary",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lcom/tacz/guns/client/resource/index/ClientAttachmentIndex;modelsLoaded:Z",
                            opcode = Opcodes.PUTFIELD))
    private void taczexpands$bindSurfaces(CallbackInfo ci) {
        var faces = taczexpands$getSurfaceFaces();
        ModelTargets.bind(attachmentModel, faces);
        if (lodModel != null) ModelTargets.bind(lodModel.getLeft(), faces);
    }
}
