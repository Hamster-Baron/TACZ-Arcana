package group.taczexpands.client.render.flashlight;

import com.mojang.blaze3d.vertex.PoseStack;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.*;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.model.*;
import com.tacz.guns.client.model.bedrock.*;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.*;
import com.tacz.guns.util.SlotAdapterHelper;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.world.item.*;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/** Reuses attachment locators evaluated in the high model's bind pose. */
public final class FlashlightLodRenderer {
    private static boolean listening;

    private static Context current;

    private record Key(
            BedrockGunModel low,
            BedrockGunModel high,
            BedrockAttachmentModel model,
            Object adapter,
            Object variant,
            AttachmentType type,
            float sx,
            float sy,
            float sz) {}

    private static final Map<Key, List<Matrix4f>> CACHE =
            new LinkedHashMap<>(32, .75f, true) {
                protected boolean removeEldestEntry(Map.Entry<Key, List<Matrix4f>> e) {
                    return size() > 128;
                }
            };

    private static final class Context {
        final Context parent;
        final ItemStack gun;
        final BedrockGunModel low;
        final GunDisplayInstance display;
        final Set<ResourceLocation> nativeLamps = new HashSet<>();

        Context(ItemStack gun, BedrockGunModel low, GunDisplayInstance display) {
            parent = current;
            this.gun = gun;
            this.low = low;
            this.display = display;
        }
    }

    public static void clear() {
        CACHE.clear();
        current = null;
    }

    public static void external(
            BedrockGunModel model,
            ItemStack gun,
            ItemDisplayContext context,
            PoseStack pose,
            MultiBufferSource buffers) {
        Object token = beginInternal(model, gun, context, true);
        if (token != null) {
            finish(token, pose, buffers, 1);
        }
    }

    public static Object begin(BedrockGunModel model, ItemStack gun, ItemDisplayContext context) {
        return beginInternal(model, gun, context, false);
    }

    private static Object beginInternal(
            BedrockGunModel model, ItemStack gun, ItemDisplayContext context, boolean external) {
        if (!FlashlightEmitterRenderer.eligible(gun, context)) return null;
        if (!listening) {
            listening = true;
            if (Minecraft.getInstance().getResourceManager()
                    instanceof ReloadableResourceManager rm)
                rm.registerReloadListener(
                        (ResourceManagerReloadListener)
                                manager -> CACHE.clear());
        }
        GunDisplayInstance display = TimelessAPI.getGunDisplay(gun).orElse(null);
        if (display == null) return null;
        var lod = display.getLodModel();
        if (!external && (lod == null || lod.getLeft() != model)) return null;
        return current = new Context(gun, model, display);
    }

    public static void mark(ItemStack gun, ItemStack attachment) {
        if (current == null || current.gun != gun || attachment == null) return;
        IAttachment api = IAttachment.getIAttachmentOrNull(attachment);
        if (api != null) current.nativeLamps.add(api.getAttachmentId(attachment));
    }

    public static void abort(Object token) {
        if (token instanceof Context c && current == c) current = c.parent;
    }

    public static void finish(
            Object token, PoseStack stack, MultiBufferSource buffers, float alpha) {
        if (!(token instanceof Context c)) return;
        abort(token);
        if (alpha <= 0 || buffers == null) return;
        IGun gun = IGun.getIGunOrNull(c.gun);
        if (gun == null) return;
        BedrockGunModel high = c.display.getGunModel();
        if (high == null) return;
        Vector3f scale = c.display.getTransform().getScale().getThirdPerson();
        if (scale == null) scale = new Vector3f(1);
        if (!scale.isFinite() || Math.abs(scale.x * scale.y * scale.z) < 1e-8f) return;
        for (AttachmentType type : AttachmentType.values()) {
            if (type == AttachmentType.NONE) continue;
            ItemStack item = gun.getAttachment(c.gun, type);
            if (item.isEmpty()) item = gun.getBuiltinAttachment(c.gun, type);
            IAttachment attachment = IAttachment.getIAttachmentOrNull(item);
            if (attachment == null) continue;
            ResourceLocation id = attachment.getAttachmentId(item);
            if (c.nativeLamps.contains(id)) {
                continue;
            }
            ClientAttachmentIndex index = TimelessAPI.getClientAttachmentIndex(id).orElse(null);
            if (index == null) continue;
            var variant = TimelessAPI.getClientAttachmentVariantIndex(item).orElse(null);
            BedrockAttachmentModel model = index.getAttachmentModel();
            if (variant != null
                    && variant.isReplaceMode()
                    && variant.getAttachmentModel() != null
                    && variant.getModelTexture() != null) model = variant.getAttachmentModel();
            if (model == null) continue;
            ResourceLocation adapterId = SlotAdapterHelper.getEffectiveSlotAdapter(c.gun, type, id);
            var adapter =
                    adapterId == null
                            ? null
                            : TimelessAPI.getClientSlotAdapterIndex(adapterId).orElse(null);
            Key key =
                    new Key(c.low, high, model, adapter, variant, type, scale.x, scale.y, scale.z);
            List<Matrix4f> transforms = CACHE.get(key);
            if (transforms == null) {
                transforms = build(key, scale, adapter, variant);
                CACHE.put(key, transforms);
            }
            for (Matrix4f transform : transforms) {
                FlashlightEmitterRenderer.renderCached(stack, transform, buffers, alpha);
            }
        }
    }

    private static List<Matrix4f> build(
            Key k,
            Vector3f scale,
            ClientSlotAdapterIndex adapter,
            ClientAttachmentVariantIndex variant) {
        List<BedrockPart> mount =
                path(k.high.getNode(k.type.name().toLowerCase(Locale.ROOT) + "_pos"));
        if (mount.isEmpty()) return List.of();
        Matrix4f from = positioned(k.low, scale), to = positioned(k.high, scale);
        if (!from.isFinite() || Math.abs(from.determinant()) < 1e-8f) return List.of();
        Matrix4f base = from.invert().mul(to).mul(rest(mount)).translate(0, -1.5f, 0);
        if (adapter != null) offset(base, adapter.getMountOffset());
        List<Matrix4f> result = new ArrayList<>();
        if (variant != null && variant.isOverlayMode()) {
            if (variant.getAttachmentModel() != null && variant.getModelTexture() != null)
                add(result, base, variant.getAttachmentModel());
            offset(base, variant.getMountOffset());
        }
        add(result, base, k.model);
        return List.copyOf(result);
    }

    private static void add(List<Matrix4f> result, Matrix4f base, BedrockAttachmentModel model) {
        List<BedrockPart> p = path(model.getNode("flashlight"));
        if (p.isEmpty()) return;
        Matrix4f matrix = new Matrix4f(base).mul(rest(p));
        if (matrix.isFinite()) result.add(matrix);
    }

    private static void offset(Matrix4f m, Vector3f o) {
        if (o != null) m.translate(o.x / 16, -o.y / 16, o.z / 16);
    }

    private static List<BedrockPart> path(BedrockPart p) {
        ArrayList<BedrockPart> result = new ArrayList<>();
        Set<BedrockPart> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        while (p != null) {
            if (!visited.add(p)) throw new IllegalArgumentException("Cyclic bone parent chain");
            result.add(p);
            p = p.getParent();
        }
        Collections.reverse(result);
        return result;
    }

    private static Matrix4f rest(List<BedrockPart> path) {
        Matrix4f m = new Matrix4f();
        for (BedrockPart p : path)
            m.translate(p.x / 16, p.y / 16, p.z / 16)
                    .rotateZ(p.getInitRotZ())
                    .rotateY(p.getInitRotY())
                    .rotateX(p.getInitRotX());
        return m;
    }

    // Same positioning math as the pinned TaCZ renderer, evaluated in bind pose.
    private static Matrix4f positioned(BedrockGunModel model, Vector3f s) {
        Matrix4f m = new Matrix4f();
        List<BedrockPart> path = model.getThirdPersonHandOriginPath();
        if (path != null) {
            m.translate(0, 1.5f, 0);
            for (int i = path.size() - 1; i >= 0; i--) {
                BedrockPart p = path.get(i);
                m.rotateX(-p.getInitRotX()).rotateY(-p.getInitRotY()).rotateZ(-p.getInitRotZ());
                m.translate(
                        -p.x * s.x / 16,
                        p.getParent() != null ? -p.y * s.y / 16 : (1.5f - p.y / 16) * s.y,
                        -p.z * s.z / 16);
            }
            m.translate(0, -1.5f, 0);
        }
        return m.translate(0, 1.5f, 0).scale(s).translate(0, -1.5f, 0);
    }
}