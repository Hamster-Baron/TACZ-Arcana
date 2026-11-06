package group.taczexpands.client.render.paint;

import group.taczexpands.client.paint.PaintParameters;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/** Stable formal identities survive GPU eviction; entry limits and reloads remove them. */
final class PaintTextureCache {
    private static final Map<Key, PaintEntry> entries = new HashMap<>();
    private static long sequence;

    private PaintTextureCache() {}

    private record Key(long epoch, String kind, ResourceLocation display, ResourceLocation model,
                       ResourceLocation original, ResourceLocation mask, ResourceLocation recipe,
                       PaintParameters.Resolved parameters) {}

    static ResourceLocation resolve(String kind, ResourceLocation display, ResourceLocation model,
                                    ResourceLocation original, ResourceLocation mask,
                                    ResourceLocation recipe, PaintInput input) {
        Key key = new Key(PaintResources.epoch(), kind, display, model, original, mask, recipe, input.parameters());
        PaintEntry entry = entries.computeIfAbsent(key, ignored -> PaintEntry.create(
                new ResourceLocation("taczexpands", "paint/generated/" + sequence++), input));
        entry.texture().touch();
        return entry.location();
    }

    static Collection<PaintEntry> entries() {
        return entries.values();
    }

    static void remove(Collection<PaintEntry> expired) {
        if (expired.isEmpty()) return;
        PaintExternalOwners.clear();
        entries.values().removeAll(expired);
        expired.forEach(PaintEntry::close);
        PaintMaterialKeys.removeTextures(expired);
    }

    static void clear() {
        remove(new ArrayList<>(entries.values()));
        sequence = 0;
    }
}

record PaintEntry(ResourceLocation location, PaintTexture texture) {
    static PaintEntry create(ResourceLocation location, PaintInput input) {
        PaintTexture texture = new PaintTexture(input);
        Minecraft.getInstance().getTextureManager().register(location, texture);
        return new PaintEntry(location, texture);
    }

    void close() {
        texture.close();
        Minecraft.getInstance().getTextureManager().release(location);
    }
}
