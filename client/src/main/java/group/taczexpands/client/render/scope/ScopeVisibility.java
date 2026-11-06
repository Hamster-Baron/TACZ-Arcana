package group.taczexpands.client.render.scope;

import com.tacz.guns.api.DefaultAssets;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.event.CameraSetupEvent;
import com.tacz.guns.client.resource.index.ClientAttachmentIndex;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.GameType;
import net.minecraftforge.client.event.ViewportEvent;

public final class ScopeVisibility {
    private ScopeVisibility() {}

    public static boolean visible() {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        return player != null && IClientPlayerGunOperator.fromLocalPlayer(player)
                .getClientAimingProgress(minecraft.getFrameTime()) > 0.0f;
    }

    public static boolean gate(boolean advanced) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (!advanced || player == null || !minecraft.options.getCameraType().isFirstPerson()
                || minecraft.options.hideGui || minecraft.gameMode.getPlayerMode() == GameType.SPECTATOR
                || player.isSleeping()
                || minecraft.getCameraEntity() != player || !visible()) {
            return false;
        }
        var stack = player.getMainHandItem();
        var gun = IGun.getIGunOrNull(stack);
        if (gun == null) return false;
        var id = gun.getAttachmentId(stack, AttachmentType.SCOPE);
        if (DefaultAssets.EMPTY_ATTACHMENT_ID.equals(id)) {
            id = gun.getBuiltInAttachmentId(stack, AttachmentType.SCOPE);
        }
        return TimelessAPI.getClientAttachmentIndex(id)
                .map(ClientAttachmentIndex::isScope).orElse(false);
    }

    public static void updateClosedFov(ViewportEvent.ComputeFov event) {
        if (!visible() && event.usedConfiguredFov()) {
            CameraSetupEvent.WORLD_FOV_DYNAMICS.update((float) event.getFOV());
        }
    }
}
