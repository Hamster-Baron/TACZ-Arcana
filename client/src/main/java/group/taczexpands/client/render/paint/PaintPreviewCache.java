package group.taczexpands.client.render.paint;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Mutable editor textures are isolated from immutable installed-paint materials. */
public final class PaintPreviewCache {
    private static final Map<Key, PaintEntry> entries = new HashMap<>();
    private static ItemStack stack;
    private static boolean gunPreview;
    private static long sequence;

    private PaintPreviewCache() {}

    private record Key(String kind, ResourceLocation display, ResourceLocation model,
                       ResourceLocation original, ResourceLocation mask) {}

    public static void begin(ItemStack preview) {
        stack = preview;
    }

    static boolean matches(ItemStack candidate) {
        return candidate == stack;
    }

    static ResourceLocation resolve(String kind, ResourceLocation display, ResourceLocation model,
                                    ResourceLocation original, ResourceLocation mask, PaintInput input) {
        input.surface().owner().pin(input);
        Key key = new Key(kind, display, model, original, mask);
        PaintEntry entry = entries.get(key);
        if (entry == null) {
            boolean primary = kind.equals("gun") && !gunPreview;
            ResourceLocation location = new ResourceLocation("taczexpands",
                    primary ? "paint/preview" : "paint/preview/" + sequence++);
            entry = PaintEntry.create(location, input);
            entries.put(key, entry);
            gunPreview |= primary;
        } else {
            entry.texture().update(input);
        }
        entry.texture().touch();
        return entry.location();
    }

    static Collection<PaintEntry> entries() {
        return entries.values();
    }

    public static void clear() {
        stack = null;
        if (!entries.isEmpty()) PaintExternalOwners.clear();
        PaintMaterialKeys.removeTextures(entries.values());
        entries.values().forEach(PaintEntry::close);
        entries.clear();
        gunPreview = false;
        sequence = 0;
    }
}
