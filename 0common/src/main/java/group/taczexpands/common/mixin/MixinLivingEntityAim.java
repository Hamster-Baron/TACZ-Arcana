package group.taczexpands.common.mixin;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.entity.shooter.LivingEntityAim;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = LivingEntityAim.class, remap = false)
public class MixinLivingEntityAim {
    @Shadow
    @Final
    private LivingEntity shooter;

    @ModifyVariable(method = "tickAimingProgress", at = @At(value = "LOAD"), ordinal = 0)
    private float modifyAimTime(float aimTime) {
        Float modifier = GunExtras.INSTANCE.getAimTimeModifier(shooter, shooter.getMainHandItem());
        if (modifier != null) {
            return aimTime * modifier;
        }
        return aimTime;
    }

    @Redirect(method = "zoom", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getAttachmentId(Lnet/minecraft/world/item/ItemStack;Lcom/tacz/guns/api/item/attachment/AttachmentType;)Lnet/minecraft/resources/ResourceLocation;"))
    private ResourceLocation hookGetAttachmentId(IGun instance, ItemStack itemStack, AttachmentType attachmentType) {
        var scopeID = instance.getAttachmentId(itemStack, attachmentType);
        if (scopeID.equals(DefaultAssets.EMPTY_ATTACHMENT_ID)) {
            scopeID = instance.getBuiltInAttachmentId(itemStack, attachmentType);
        }
        return scopeID;
    }
}
