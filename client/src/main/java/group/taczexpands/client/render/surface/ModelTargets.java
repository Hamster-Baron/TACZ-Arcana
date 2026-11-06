package group.taczexpands.client.render.surface;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.ClientIndexManager;
import group.taczexpands.client.accessor.IAccessorSurfaceCube;
import group.taczexpands.client.accessor.IAccessorSurfaceDisplay;
import group.taczexpands.client.accessor.IAccessorSurfacePart;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Resolve authoring names once, against each display's own model instances. */
public final class ModelTargets {
    private static volatile boolean anyFaces;
    private ModelTargets() {}

    public static void clear() {
        anyFaces = false;
        SurfaceBoneAnchor.clear();
    }

    /** Index reload has completed, including displays whose geometry is still loaded lazily. */
    public static void refresh() {
        anyFaces = ClientAssetsManager.INSTANCE.getGunDisplays().stream().anyMatch(e -> hasFaces(e.getValue()))
                || ClientIndexManager.ATTACHMENT_INDEX.values().stream().anyMatch(ModelTargets::hasFaces)
                || ClientIndexManager.ATTACHMENT_VARIANT_INDEX.values().stream().anyMatch(ModelTargets::hasFaces);
    }

    public static boolean dynamic(ItemStack stack) {
        return dynamic(stack, ItemDisplayContext.NONE);
    }

    public static boolean dynamic(ItemStack stack, ItemDisplayContext context) {
        if (!anyFaces || SurfaceRenderer.secondaryPass()) return false;
        boolean boneFaces = SurfaceBoneAnchor.mainFirstPerson(stack, context);
        var gun = IGun.getIGunOrNull(stack);
        if (gun == null) return attachmentDynamic(stack, boneFaces);
        if (activeFaces(TimelessAPI.getGunDisplay(stack).orElse(null), boneFaces)) return true;
        // Read current equipment each time so attachment, resource and NBT changes
        // immediately update the dynamic-face decision.
        for (var type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            var attachment = gun.getAttachment(stack, type);
            if (attachment.isEmpty()) attachment = gun.getBuiltinAttachment(stack, type);
            if (attachmentDynamic(attachment, boneFaces)) return true;
        }
        return false;
    }

    private static boolean attachmentDynamic(ItemStack stack, boolean boneFaces) {
        var attachment = IAttachment.getIAttachmentOrNull(stack);
        if (attachment == null) return false;
        return activeFaces(
                        TimelessAPI.getClientAttachmentIndex(attachment.getAttachmentId(stack))
                                .orElse(null), boneFaces)
                || activeFaces(TimelessAPI.getClientAttachmentVariantIndex(stack).orElse(null), boneFaces);
    }

    private static boolean activeFaces(Object display, boolean boneFaces) {
        if (!(display instanceof IAccessorSurfaceDisplay surface)) return false;
        for (var face : surface.taczexpands$getSurfaceFaces()) {
            var feed = SurfaceEngine.feed(face.feed);
            if (feed != null && (boneFaces || !feed.boneCamera())) return true;
        }
        return false;
    }

    private static boolean hasFaces(Object display) {
        return display instanceof IAccessorSurfaceDisplay surface
                && !surface.taczexpands$getSurfaceFaces().isEmpty();
    }

    public static void bind(BedrockModel model, List<SurfaceConfig.Face> faces) {
        if (model == null) return;
        Map<String, Map<Integer, List<SurfaceConfig.Face>>> bindings = new HashMap<>();
        for (var face : faces) {
            bindings.computeIfAbsent(face.bone, key -> new HashMap<>())
                    .computeIfAbsent(face.cube, key -> new ArrayList<>())
                    .add(face);
        }
        for (String bone : model.getIndexBones().keySet()) {
            var part = model.getNode(bone);
            if (part == null) continue;
            var cubes = bindings.getOrDefault(bone, Map.of());
            boolean dynamic = false;
            for (int index = 0; index < part.cubes.size(); index++) {
                if (part.cubes.get(index) instanceof IAccessorSurfaceCube cube) {
                    var definitions = cubes.getOrDefault(index, List.of());
                    cube.taczexpands$bindSurfaceFaces(definitions);
                    dynamic |= !definitions.isEmpty();
                }
            }
            ((IAccessorSurfacePart) part).taczexpands$setSurfaceDynamic(dynamic);
        }
    }
}
