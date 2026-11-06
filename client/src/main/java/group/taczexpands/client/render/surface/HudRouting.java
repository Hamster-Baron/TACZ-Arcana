package group.taczexpands.client.render.surface;

import java.util.Set;

/** A HUD identifier belongs to the longest registered feed path before its component name. */
public final class HudRouting {
    public static String owner(String identifier, Set<String> feedIds) {
        if (!identifier.startsWith("surface:")) return null;
        String path = identifier.substring("surface:".length());
        for (int slash = path.lastIndexOf('/');
                slash >= 0;
                slash = path.lastIndexOf('/', slash - 1)) {
            String candidate = path.substring(0, slash);
            if (feedIds.contains(candidate)) return candidate;
        }
        return null;
    }
}
