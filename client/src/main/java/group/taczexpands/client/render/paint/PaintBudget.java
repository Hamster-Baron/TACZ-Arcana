package group.taczexpands.client.render.paint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;

/** Render-thread LRU budget; materials used in the current frame remain pinned. */
public final class PaintBudget {
    // Nine 2K guns with six 2K accessory materials each occupy 1008 MiB.
    private static final long RESIDENT_BYTES = 1024L * 1024 * 1024;
    private static final int FORMAL_ENTRIES = 256;
    private static long frame;

    private PaintBudget() {}

    static long frame() {
        return frame;
    }

    public static void endFrame() {
        PaintResources.endFrame();
        var formal = PaintTextureCache.entries();
        var residents = new ArrayList<>(formal);
        residents.addAll(PaintPreviewCache.entries());
        residents.forEach(entry -> entry.texture().installPrepared());
        long bytes = residents.stream().mapToLong(entry -> entry.texture().bytes()).sum();
        int count = formal.size();
        if (bytes > RESIDENT_BYTES || count > FORMAL_ENTRIES) {
            residents.sort(Comparator.comparingLong(entry -> entry.texture().touched()));
            var formalSet = new HashSet<>(formal);
            var expired = new ArrayList<PaintEntry>();
            for (var entry : residents) {
                if (bytes <= RESIDENT_BYTES && count <= FORMAL_ENTRIES) break;
                if (entry.texture().touched() >= frame) continue;
                if (formalSet.contains(entry) && count > FORMAL_ENTRIES) {
                    bytes -= entry.texture().bytes();
                    count--;
                    expired.add(entry);
                } else if (bytes > RESIDENT_BYTES) {
                    bytes -= entry.texture().bytes();
                    entry.texture().releaseResident();
                }
            }
            PaintTextureCache.remove(expired);
        }
        PaintMaterialKeys.trim();
        frame++;
    }
}
