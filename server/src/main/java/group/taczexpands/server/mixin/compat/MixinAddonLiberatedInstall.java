package group.taczexpands.server.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import group.taczexpands.server.util.GunManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;

@Pseudo
@Mixin(targets = "com.mafuyu404.taczaddon.network.server.LiberateAttachmentInstallHandler", remap = false)
public abstract class MixinAddonLiberatedInstall {
    @WrapOperation(method = "handleOnServer", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/IGun;installAttachment(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void taczexpands$install(IGun gun, ItemStack stack, ItemStack attachment, Operation<Void> original,
            @Share("previousGun") LocalRef<ItemStack> previous) {
        previous.set(stack.copy());
        original.call(gun, stack, attachment);
    }

    @WrapOperation(method = "handleOnServer", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/resource/modifier/AttachmentPropertyManager;postChangeEvent(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void taczexpands$complete(LivingEntity player, ItemStack stack, Operation<Void> original,
            @Local AttachmentType type, @Share("previousGun") LocalRef<ItemStack> previous) {
        GunManager.INSTANCE.completeAttachmentChange(previous.get(), stack, (ServerPlayer) player, type);
    }

    @WrapOperation(method = "handleOnServer", at = @At(value = "INVOKE",
            target = "Lcom/tacz/guns/api/item/IGun;dropAllAmmo(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)V"))
    private static void taczexpands$ammoAlreadyReturned(IGun gun, Player player, ItemStack stack, Operation<Void> original) {
        // completeAttachmentChange refunds the old ammunition and clears the committed gun.
    }
}
