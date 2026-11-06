package group.taczexpands.client.render.paint;

import net.minecraft.resources.ResourceLocation;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Opaque equality identities for composite external-mesh materials. */
final class PaintMaterialKeys {
    private static final int MAX_ENTRIES = 1024;
    private static final Map<List<ResourceLocation>, ResourceLocation> keys = new LinkedHashMap<>(16, .75f, true);
    private static long sequence;

    private PaintMaterialKeys() {}

    static ResourceLocation resolve(List<ResourceLocation> materials) {
        return keys.computeIfAbsent(List.copyOf(materials), ignored ->
                new ResourceLocation("taczexpands", "paint/material/" + sequence++));
    }

    static void trim() {
        if (keys.size() <= MAX_ENTRIES) return;
        PaintExternalOwners.clear();
        var iterator = keys.keySet().iterator();
        while (keys.size() > MAX_ENTRIES) {
            iterator.next();
            iterator.remove();
        }
    }

    static void removeTextures(Collection<PaintEntry> entries) {
        Set<ResourceLocation> removed = new HashSet<>();
        entries.forEach(entry -> removed.add(entry.location()));
        keys.keySet().removeIf(materials -> materials.stream().anyMatch(removed::contains));
    }

    static void clear() {
        if (!keys.isEmpty()) PaintExternalOwners.clear();
        keys.clear();
        sequence = 0;
    }
}
