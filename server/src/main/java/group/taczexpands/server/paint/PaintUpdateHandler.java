package group.taczexpands.server.paint;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import group.taczexpands.common.nbt.GunExtras;
import group.taczexpands.common.network.c2s.C2SRaw;
import group.taczexpands.common.paint.PaintState;
import group.taczexpands.common.paint.PaintConstants;
import group.taczexpands.common.paint.PaintUpdate;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

/** Applies paint updates on the server thread through C2SRawImpl.enqueueWork. */
public final class PaintUpdateHandler {
    private PaintUpdateHandler() {}

    public static boolean handleRaw(C2SRaw packet, NetworkEvent.Context context) {
        if (packet.getType() != PaintUpdate.TYPE_PAINT_UPDATE) return false;
        PaintUpdate update = PaintUpdate.decode(packet.getData());
        ServerPlayer player = context.getSender();
        if (update != null && player != null) apply(player, update);
        return true;
    }

    public static boolean apply(ServerPlayer player, PaintUpdate update) {
        if (player.getInventory().selected != update.slot()) return false;
        ItemStack gun = player.getInventory().getItem(update.slot());
        IGun accessor = IGun.getIGunOrNull(gun);
        if (accessor == null || !update.gunUuid().equals(GunExtras.INSTANCE.getUUID(gun))) return false;
        AttachmentType module = AttachmentType.valueOf(PaintConstants.MODULE);
        if (!update.moduleId().equals(accessor.getAttachmentId(gun, module))
                || accessor.getAttachment(gun, module).isEmpty()) return false;
        CompoundTag tag = accessor.getAttachmentTag(gun, module);
        if (tag == null) return false;
        boolean removeDefaultTag = update.state().isDefault() && tag.getCompound("Extras").contains("Paint");
        if (!removeDefaultTag && PaintState.read(tag).equals(update.state()))
            return true;
        update.state().write(tag);
        player.getInventory().setChanged();
        player.containerMenu.broadcastChanges();
        return true;
    }
}
