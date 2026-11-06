package group.taczexpands.client.render.surface;

/** Optional renderer integration, installed by client setup. All calls use the render thread. */
public interface SurfaceBackend {
    SurfaceBackend VANILLA = new SurfaceBackend() {};

    static SurfaceBackend unavailable(String reason) {
        return new SurfaceBackend() {
            @Override public String cameraUnavailableReason() { return reason; }
        };
    }
    default boolean usesShaders(SurfaceDefinition.Camera camera) { return false; }
    default String cameraUnavailableReason() { return null; }
    default void configureCameraViews(boolean enabled) {}
    default void beginView() {}
    default void endView() {}
    default void restoreMain() {}
    default void release() {}
}
