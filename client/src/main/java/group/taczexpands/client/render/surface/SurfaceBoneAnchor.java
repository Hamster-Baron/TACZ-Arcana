package group.taczexpands.client.render.surface;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IAttachment;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.BedrockGunModel;
import com.tacz.guns.client.model.bedrock.BedrockModel;
import com.tacz.guns.client.model.bedrock.BedrockPart;
import com.tacz.guns.util.SlotAdapterHelper;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** Previous-frame animation snapshots in eye space, independent of hand presentation. */
public final class SurfaceBoneAnchor {
    private static final Map<String, Snapshot> snapshots = new HashMap<>();
    private static final Map<String, Optional<Path>> paths = new HashMap<>();
    private static Equipment equipment;
    private static Equipment frameEquipment;
    private static long equipmentFrame = -1;
    private static boolean firstPersonDraw;

    private record Layer(BedrockModel model, String mount, float x, float y, float z) {}
    private record Equipment(LocalPlayer player, Object level, int slot,
                             BedrockGunModel model, List<Layer> layers) {}
    private record Path(List<BedrockPart> mount, Layer layer, List<BedrockPart> bone,
                        Matrix4f inverseRoot) {}
    private record Snapshot(long frame, Equipment equipment, String bone, Vec3 eye, Matrix4f relative) {}

    private SurfaceBoneAnchor() {}

    public static void clear() {
        snapshots.clear();
        paths.clear();
        equipment = null;
        frameEquipment = null;
        equipmentFrame = -1;
    }

    static void forget(String id) {
        if (id == null) snapshots.clear();
        else snapshots.remove(id);
    }

    public static boolean mainFirstPerson(ItemStack stack, ItemDisplayContext context) {
        var mc = Minecraft.getInstance();
        return mc.player != null && mc.level != null && context == mainHandContext(mc.player)
                && mc.options.getCameraType().isFirstPerson() && mc.getCameraEntity() == mc.player
                && IGun.getIGunOrNull(stack) != null && !SurfaceRenderer.secondaryPass();
    }

    private static ItemDisplayContext mainHandContext(LocalPlayer player) {
        return player.getMainArm() == HumanoidArm.RIGHT
                ? ItemDisplayContext.FIRST_PERSON_RIGHT_HAND : ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
    }

    public static boolean beginDraw(ItemStack stack, ItemDisplayContext context, boolean firstPersonCall) {
        boolean previous = firstPersonDraw;
        firstPersonDraw = firstPersonCall && mainFirstPerson(stack, context);
        return previous;
    }

    public static void endDraw(boolean previous) {
        firstPersonDraw = previous;
    }
    static boolean displays(SurfaceFeed feed) {
        return !feed.boneCamera() || firstPersonDraw && feed.cameraPose() != null;
    }

    /** Called once after the complete native animation update, before root sway/ADS transforms. */
    public static void capture(ItemStack stack, LocalPlayer player, float partialTick,
                               ItemDisplayContext context, BedrockGunModel model) {
        if (!mainFirstPerson(stack, context) || player != Minecraft.getInstance().player) return;
        Set<String> requested = new HashSet<>();
        for (var feed : SurfaceEngine.feeds()) if (feed.boneCamera()) requested.add(feed.id());
        snapshots.keySet().retainAll(requested);
        if (requested.isEmpty()) return;
        Equipment current = equipment();
        if (current == null || current.model != model) {
            clear();
            return;
        }
        if (!current.equals(equipment)) {
            snapshots.clear();
            paths.clear();
            equipment = current;
        }
        Matrix4f eyeRotation = new Matrix4f()
                .rotateY((float)Math.toRadians(-player.getViewYRot(partialTick) - 180))
                .rotateX((float)Math.toRadians(-player.getViewXRot(partialTick)));
        Vec3 eye = player.getEyePosition(partialTick);
        for (String id : requested) {
            var feed = SurfaceEngine.feed(id);
            String name = feed.boneName();
            if (name == null) {
                snapshots.remove(id);
                continue;
            }
            Path path = paths.computeIfAbsent(name, key -> Optional.ofNullable(resolve(current, key))).orElse(null);
            if (path == null) {
                snapshots.remove(id);
                continue;
            }
            Matrix4f relative = new Matrix4f(eyeRotation).rotateZ((float)Math.PI).mul(path.inverseRoot);
            if (path.layer.mount != null) {
                relative.mul(animated(path.mount)).translate(path.layer.x, path.layer.y, path.layer.z);
            }
            relative.mul(animated(path.bone)).rotateZ(-(float)Math.PI);
            if (!relative.isFinite() || Math.abs(relative.determinant3x3()) < 1e-8f) {
                snapshots.remove(id);
                continue;
            }
            snapshots.put(id, new Snapshot(SurfaceEngine.frame(), current, name, eye, relative));
        }
    }

    static SurfaceCameraPose pose(String id, String bone, SurfaceDefinition.Camera spec) {
        var mc = Minecraft.getInstance();
        Snapshot snapshot = snapshots.get(id);
        if (mc.player == null || !mainFirstPerson(mc.player.getMainHandItem(), mainHandContext(mc.player))
                || snapshot == null || snapshot.frame != SurfaceEngine.frame() - 1
                || !snapshot.bone.equals(bone)
                || !snapshot.equipment.equals(equipment())) {
            snapshots.remove(id);
            return null;
        }
        Matrix4f m = snapshot.relative;
        // Gram-Schmidt removes model scale/shear from lens axes and local camera offsets.
        Vector3f right = m.getColumn(0, new Vector3f());
        Vector3f up = m.getColumn(1, new Vector3f());
        Vector3f back = m.getColumn(2, new Vector3f());
        if (right.lengthSquared() < 1e-10f) return null;
        right.normalize();
        up.sub(new Vector3f(right).mul(up.dot(right)));
        if (up.lengthSquared() < 1e-10f) return null;
        up.normalize();
        Vector3f orthogonalBack = right.cross(up, new Vector3f());
        if (orthogonalBack.dot(back) <= 1e-8f) return null;
        Quaternionf rotation = new Quaternionf().setFromNormalized(new Matrix3f(right, up, orthogonalBack));
        if (!rotation.isFinite()) return null;
        Vec3 position = snapshot.eye.add(
                m.m30() + right.x * spec.x() + up.x * spec.y() + orthogonalBack.x * spec.z(),
                m.m31() + right.y * spec.x() + up.y * spec.y() + orthogonalBack.y * spec.z(),
                m.m32() + right.z * spec.x() + up.z * spec.y() + orthogonalBack.z * spec.z());
        if (!Double.isFinite(position.x) || !Double.isFinite(position.y) || !Double.isFinite(position.z)) return null;
        rotation.rotateY((float)Math.toRadians(-spec.yaw())).rotateX((float)Math.toRadians(-spec.pitch()))
                .rotateZ((float)Math.toRadians(-spec.roll()));
        Vector3f forward = rotation.transform(new Vector3f(0, 0, -1));
        float yaw = (float)Math.toDegrees(Math.atan2(-forward.x, forward.z));
        float pitch = (float)Math.toDegrees(Math.asin(Math.max(-1, Math.min(1, -forward.y))));
        Vector3f lensUp = rotation.transform(new Vector3f(0, 1, 0));
        new Matrix4f().rotateX((float)Math.toRadians(pitch)).rotateY((float)Math.toRadians(yaw + 180)).transformDirection(lensUp);
        float roll = (float)Math.toDegrees(Math.atan2(lensUp.x, lensUp.y));
        return new SurfaceCameraPose(position, yaw, pitch, roll, spec.fov(), spec.distance(), spec.vision(), spec.shaders());
    }

    private static Equipment equipment() {
        if (equipmentFrame != SurfaceEngine.frame()) {
            equipmentFrame = SurfaceEngine.frame();
            frameEquipment = readEquipment();
        }
        return frameEquipment;
    }

    private static Equipment readEquipment() {
        var mc = Minecraft.getInstance();
        var stack = mc.player.getMainHandItem();
        var gun = IGun.getIGunOrNull(stack);
        var display = TimelessAPI.getGunDisplay(stack).orElse(null);
        if (gun == null || display == null || display.getGunModel() == null) return null;
        var model = display.getGunModel();
        List<Layer> layers = new ArrayList<>();
        layers.add(new Layer(model, null, 0, 0, 0));
        for (var type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            var item = gun.getAttachment(stack, type);
            if (item.isEmpty()) item = gun.getBuiltinAttachment(stack, type);
            var api = IAttachment.getIAttachmentOrNull(item);
            if (api == null) continue;
            var id = api.getAttachmentId(item);
            var index = TimelessAPI.getClientAttachmentIndex(id).orElse(null);
            if (index == null) continue;
            String mount = type.name().toLowerCase(Locale.ROOT) + "_pos";
            if (model.getNode(mount) == null) continue;
            var adapterId = SlotAdapterHelper.getEffectiveSlotAdapter(stack, type, id);
            var adapter = adapterId == null ? null : TimelessAPI.getClientSlotAdapterIndex(adapterId).orElse(null);
            var variant = TimelessAPI.getClientAttachmentVariantIndex(item).orElse(null);
            Vector3f offset = new Vector3f(0, -1.5f, 0);
            if (adapter != null) {
                if (adapter.getModelTexture() != null) add(layers, adapter.getAdapterModel(), mount, offset);
                offset(offset, adapter.getMountOffset());
            }
            if (variant != null && variant.isOverlayMode()) {
                if (variant.getModelTexture() != null) add(layers, variant.getAttachmentModel(), mount, offset);
                offset(offset, variant.getMountOffset());
            }
            if (variant != null && variant.isReplaceMode() && variant.getAttachmentModel() != null && variant.getModelTexture() != null)
                add(layers, variant.getAttachmentModel(), mount, offset);
            else if (index.getModelTexture() != null) add(layers, index.getAttachmentModel(), mount, offset);
        }
        return new Equipment(mc.player, mc.level, mc.player.getInventory().selected, model, List.copyOf(layers));
    }

    private static void add(List<Layer> layers, BedrockModel model, String mount, Vector3f offset) {
        if (model != null) layers.add(new Layer(model, mount, offset.x, offset.y, offset.z));
    }
    private static void offset(Vector3f target, Vector3f value) {
        if (value != null) target.add(value.x / 16, -value.y / 16, value.z / 16);
    }
    private static Path resolve(Equipment source, String name) {
        Layer match = null;
        BedrockPart bone = null;
        for (Layer layer : source.layers) {
            var candidate = layer.model.getNode(name);
            if (candidate == null) continue;
            if (match != null) return null;
            match = layer;
            bone = candidate;
        }
        if (match == null) return null;
        var root = source.model.getRootNode();
        if (root == null) return null;
        Matrix4f inverse = new Matrix4f().translation(root.x / 16, root.y / 16, root.z / 16)
                .rotateZ(root.getInitRotZ()).rotateY(root.getInitRotY()).rotateX(root.getInitRotX()).invert();
        return new Path(match.mount == null ? List.of() : chain(source.model.getNode(match.mount)), match, chain(bone), inverse);
    }
    private static List<BedrockPart> chain(BedrockPart leaf) {
        List<BedrockPart> chain = new ArrayList<>();
        for (var part = leaf; part != null; part = part.getParent()) chain.add(part);
        Collections.reverse(chain);
        return List.copyOf(chain);
    }
    private static Matrix4f animated(List<BedrockPart> chain) {
        PoseStack pose = new PoseStack();
        for (var part : chain) part.translateAndRotateAndScale(pose);
        return new Matrix4f(pose.last().pose());
    }
}
