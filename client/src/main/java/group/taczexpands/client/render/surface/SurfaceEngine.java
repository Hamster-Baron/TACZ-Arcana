package group.taczexpands.client.render.surface;

import group.taczexpands.client.gui.CustomHUDOverlay;
import group.taczexpands.client.render.Flashlight;
import java.util.*;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;

/** Render-thread registry and refresh scheduler. GPU drawing belongs to SurfaceRenderer. */
public final class SurfaceEngine {
    private static final Map<String, SurfaceFeed> feeds = new LinkedHashMap<>();
    private static final Map<String, SurfaceCameraOverride> cameraOverrides = new HashMap<>();
    private static Boolean configuredCameraViews;
    private static long frame;
    private static ClientLevel level;

    private SurfaceEngine() {}

    public static Collection<SurfaceFeed> feeds() { return Collections.unmodifiableCollection(feeds.values()); }
    public static Set<String> feedIds() { return Collections.unmodifiableSet(feeds.keySet()); }
    public static SurfaceFeed feed(String id) { return feeds.get(id); }
    public static long frame() { return frame; }

    /** A visible HUD component schedules its source even before the first target allocation. */
    public static ResourceLocation hudTexture(String id) {
        var feed = feeds.get(id);
        if (feed == null || feed == SurfaceRenderer.activeFeed()) return null;
        feed.markSeen(frame);
        if (feed.boneCamera() && feed.cameraPose() == null) return null;
        return feed.getId() < 0 ? null : feed.texture();
    }

    public static void installBackend(SurfaceBackend backend) {
        Objects.requireNonNull(backend);
        if (SurfaceRenderer.isRendering()) throw new IllegalStateException("Cannot replace backend during a surface draw");
        releaseTargets();
        SurfaceRenderer.installBackend(backend);
        configuredCameraViews = null;
        configureViews();
    }

    public static void install(SurfaceConfig config) {
        config.validate();
        Map<String, SurfaceFeed> next = new LinkedHashMap<>();
        int serial = 0;
        for (var entry : config.feeds.entrySet())
            next.put(entry.getKey(), new SurfaceFeed(entry.getKey(), entry.getValue(), serial++, config.feeds.size()));
        releaseTargets();
        feeds.clear();
        feeds.putAll(next);
        CustomHUDOverlay.INSTANCE.reindexFeeds();
        feeds.forEach((id, feed) -> feed.cameraOverride(cameraOverrides.get(id)));
        configureViews();
    }

    private static void configureViews() {
        boolean enabled = feeds.values().stream().anyMatch(SurfaceFeed::hasCamera);
        if (configuredCameraViews == null || configuredCameraViews != enabled) {
            SurfaceRenderer.configureCameraViews(enabled);
            configuredCameraViews = enabled;
        }
    }

    private static void releaseTargets() {
        SurfaceBoneAnchor.clear();
        SurfaceRenderer.releaseViews();
        feeds.values().forEach(SurfaceFeed::release);
    }

    public static void leaveWorld() {
        releaseTargets();
        removeCamera(null);
        level = null;
    }

    public static void render(float partialTick, long nanos) {
        if (SurfaceRenderer.isRendering()) return;
        frame++;
        var mc = Minecraft.getInstance();
        synchronizeWorld();
        if (mc.level == null || mc.player == null || mc.isPaused()) return;
        long now = System.nanoTime();
        var scheduled = new ArrayList<SurfaceFeed>();
        for (var feed : feeds.values()) {
            SurfaceRenderer.prepare(feed, partialTick, now);
            if (feed.due(frame, now)) scheduled.add(feed);
        }
        var camera = scheduled.stream().filter(SurfaceFeed::readyCamera)
                .min(Comparator.comparingLong(SurfaceFeed::nextUpdate)).orElse(null);
        scheduled.removeIf(feed -> feed.readyCamera() && feed != camera);
        Flashlight.INSTANCE.beginFrame(partialTick, camera == null ? List.of() : List.of(camera));
        for (var feed : scheduled) {
            update(feed, partialTick, nanos);
            feed.updated(now);
        }
    }

    /** The frame scheduler draws world views; providers update camera overrides. */
    static void update(SurfaceFeed feed, float partialTick, long nanos) {
        if (feeds.get(feed.id()) != feed) throw new IllegalArgumentException("Feed is not installed: " + feed.id());
        SurfaceRenderer.update(feed, partialTick, nanos, feedIds());
    }

    /** Copies the input; null explicitly selects HUD-only until removeCamera restores the resource. */
    public static void setCamera(String id, SurfaceConfig.Camera camera) {
        if (camera != null) {
            setCameraUpdate(id, SurfaceCameraUpdate.fromConfig(camera));
            return;
        }
        synchronizeWorld();
        putCamera(id, new SurfaceCameraOverride(null, null, System.nanoTime(), 0));
    }

    /** Unknown IDs retain their override for a later client resource reload. */
    public static void setCameraUpdate(String id, SurfaceCameraUpdate update) {
        Objects.requireNonNull(update);
        synchronizeWorld();
        var feed = feeds.get(id);
        long now = System.nanoTime();
        SurfaceCameraPose start = null;
        if (feed != null) {
            feed.resolveCamera(Minecraft.getInstance().getFrameTime(), now);
            start = feed.cameraPose();
        }
        var previous = cameraOverrides.get(id);
        var values = update.merge(previous == null ? null : previous.values);
        putCamera(id, new SurfaceCameraOverride(values, start, now,
                update.durationMillis() == null ? 0 : update.durationMillis()));
    }

    /** A packet arriving just after respawn belongs to the new level before its first render. */
    private static void synchronizeWorld() {
        var current = Minecraft.getInstance().level;
        if (current != level) {
            boolean replacingWorld = level != null;
            level = current;
            releaseTargets();
            if (replacingWorld) removeCamera(null);
        }
    }

    private static void putCamera(String id, SurfaceCameraOverride override) {
        if (id == null || ResourceLocation.tryParse(id) == null)
            throw new IllegalArgumentException("Invalid surface identifier: " + id);
        cameraOverrides.put(id, override);
        var feed = feeds.get(id);
        if (feed != null) feed.cameraOverride(override);
        configureViews();
    }

    /** A null identifier removes all overrides; every feed resumes its immutable resource camera. */
    public static void removeCamera(String id) {
        SurfaceBoneAnchor.forget(id);
        if (id == null) {
            cameraOverrides.clear();
            feeds.values().forEach(feed -> feed.cameraOverride(null));
        } else {
            cameraOverrides.remove(id);
            var feed = feeds.get(id);
            if (feed != null) feed.cameraOverride(null);
        }
        configureViews();
    }
}
