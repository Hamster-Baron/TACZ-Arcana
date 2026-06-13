package group.taczexpands.client.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.client.model.functional.BeamRenderer;
import group.taczexpands.common.accessor.IAccessorAttachmentData;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

@Mixin(value = BeamRenderer.class, remap = false)
public class MixinBeamRenderer {
}
