package group.taczexpands.server.mixin;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
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
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = ClientMessageUnloadAttachment.class, remap = false, priority = 500)
public class MixinClientMessageUnloadAttachment {
    @Shadow
    @Final
    private int gunSlotIndex;

    @Shadow
    @Final
    private AttachmentType attachmentType;


    @Inject(method = "lambda$handle$0", at = @At("HEAD"), cancellable = true)
    private static void hookHandle(NetworkEvent.Context context, ClientMessageUnloadAttachment message, CallbackInfo ci) {
        ci.cancel();
        ServerPlayer player = context.getSender();
        if (player == null) {
            return;
        }
        Inventory inventory = CompatHelper.INSTANCE.hasTACZAddon() ? TACZAddonCompat.INSTANCE.getInventoryUnload(player.getInventory()) : player.getInventory();
        ItemStack gunItem = inventory.getItem(((MixinClientMessageUnloadAttachment) (Object) message).gunSlotIndex);
        var attachmentType = ((MixinClientMessageUnloadAttachment) (Object) message).attachmentType;
        GunManager.INSTANCE.unloadAttachment(attachmentType, gunItem, player, inventory);
    }
}
