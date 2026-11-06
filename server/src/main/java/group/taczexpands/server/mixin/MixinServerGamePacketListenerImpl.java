package group.taczexpands.server.mixin;

import group.taczexpands.common.network.v2.s2c.S2CActionLockState;
import group.taczexpands.server.skill.ActionManager;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import net.minecraft.world.inventory.ClickType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerGamePacketListenerImpl.class)
public abstract class MixinServerGamePacketListenerImpl {
    @Shadow
    public ServerPlayer player;

    @Inject(method = "handlePlayerAction", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER), cancellable = true)
    private void taczexpands$cancelDropAction(ServerboundPlayerActionPacket packet, CallbackInfo callbackInfo) {
        var action = packet.getAction();
        if ((action == ServerboundPlayerActionPacket.Action.DROP_ITEM || action == ServerboundPlayerActionPacket.Action.DROP_ALL_ITEMS) && taczexpands$isDropLocked()) {
            player.inventoryMenu.sendAllDataToRemote();
            callbackInfo.cancel();
        }
    }

    @Inject(method = "handleContainerClick", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;resetLastActionTime()V", shift = At.Shift.AFTER), cancellable = true)
    private void taczexpands$cancelContainerDrop(ServerboundContainerClickPacket packet, CallbackInfo callbackInfo) {
        ClickType clickType = packet.getClickType();
        boolean dropsFromSlot = clickType == ClickType.THROW;
        boolean dropsCarriedItem = packet.getSlotNum() == -999 && (clickType == ClickType.PICKUP || clickType == ClickType.QUICK_MOVE);
        if ((dropsFromSlot || dropsCarriedItem) && taczexpands$isDropLocked()) {
            player.containerMenu.sendAllDataToRemote();
            callbackInfo.cancel();
        }
    }

    @Unique
    private boolean taczexpands$isDropLocked() {
        return ActionManager.INSTANCE.shouldCancel(player, S2CActionLockState.Action.Drop);
    }
}
