package group.taczexpands.server.mixin;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import group.taczexpands.server.compat.CompatHelper;
import group.taczexpands.server.compat.taczaddon.TACZAddonCompat;
import group.taczexpands.server.util.GunManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMessageRefitGun.class, remap = false)
public class MixinClientMessageRefitGun {

    @Shadow
    @Final
    private int attachmentSlotIndex;

    @Shadow
    @Final
    private int gunSlotIndex;

    @Shadow
    @Final
    private AttachmentType attachmentType;

    @Unique
    private int taczexpands$getAttachmentSlotIndex() {
        return this.attachmentSlotIndex;
    }

    @Unique
    private int taczexpands$getGunSlotIndex() {
        return this.gunSlotIndex;
    }

    @Unique
    private AttachmentType taczexpands$getAttachmentType() {
        return this.attachmentType;
    }

    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true)
    private static void hookHandle(NetworkEvent.Context context, ClientMessageRefitGun message, CallbackInfo ci) {
        ci.cancel();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        Inventory inventory = CompatHelper.INSTANCE.hasTACZAddon() ? TACZAddonCompat.INSTANCE.getInventoryRefit(player.getInventory()) : player.getInventory();
        var attachmentSlotIndex = ((MixinClientMessageRefitGun) (Object) message).attachmentSlotIndex;
        ItemStack attachmentItem = inventory.getItem(attachmentSlotIndex);
        ItemStack gunItem = inventory.getItem(((MixinClientMessageRefitGun) (Object) message).gunSlotIndex);
        var attachmentType = ((MixinClientMessageRefitGun) (Object) message).attachmentType;

        GunManager.INSTANCE.refitAttachment(attachmentType, attachmentItem, attachmentSlotIndex, gunItem, player, false, inventory);
    }
}
