package group.taczexpands.client.render.paint;

import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.StaticMeshRenderer;
import com.github.mcmodderanchor.simplebedrockmodel.v2.client.mesh.world.WorldMeshGroup;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.block.entity.DisplayGunSlot;
import group.taczexpands.client.mixin.accessor.IAccessorDisplayGunMeshGroup;
import group.taczexpands.client.mixin.accessor.IAccessorDisplayGunRenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.Set;

/** TacZ's retained owners. Called once before removing a paint generation. */
final class PaintExternalOwners {
    private PaintExternalOwners() {}

    /** Only cached display slots waiting for these inputs need a new material key. */
    @SuppressWarnings("unchecked")
    static void inputsReady(Set<PaintResources.Surface> surfaces) {
        var group = (WorldMeshGroup<DisplayGunSlot>)
                IAccessorDisplayGunMeshGroup.taczexpands$getGroup();
        IAccessorDisplayGunMeshGroup.taczexpands$getAppearances().keySet().removeIf(key -> {
            var slot = (DisplayGunSlot) key;
            var stack = slot.owner().getItem(slot.index());
            if (PaintQueries.preparedRecipe(stack) == null) return false;
            var display = TimelessAPI.getGunDisplay(stack).orElse(null);
            var gun = display == null ? null : PaintResources.gun(display.getDisplayId());
            boolean changed = gun != null && (surfaces.contains(gun.near()) || surfaces.contains(gun.far()));
            if (!changed) {
                for (var layer : PaintAttachmentSelection.installedNear(stack, type -> true)) {
                    var attachment = PaintResources.attachment(layer.index());
                    if (attachment != null && (surfaces.contains(attachment.near()) || surfaces.contains(attachment.far()))) {
                        changed = true;
                        break;
                    }
                }
            }
            if (changed && group != null) group.markDirty(slot);
            return changed;
        });
    }

    static void clear() {
        StaticMeshRenderer.invalidateOwner(new ResourceLocation("tacz", "external_item_guns"));
        var group = IAccessorDisplayGunMeshGroup.taczexpands$getGroup();
        if (group != null) group.clear();
        IAccessorDisplayGunMeshGroup.taczexpands$getAppearances().clear();
        IAccessorDisplayGunRenderType.taczexpands$getCache().keySet().removeIf(id ->
                id.getNamespace().equals("taczexpands") && id.getPath().startsWith("paint/"));
    }
}
