package group.taczexpands.client.render.surface;

import java.util.List;

/** Validated immutable runtime data copied from parsed JSON. */
public record SurfaceDefinition(int width, int height, int fps, Camera camera, Hud hud) {
    public record Camera(String space, double x, double y, double z, float yaw, float pitch,
                         float roll, float fov, float distance, String vision, boolean shaders) {
        static Camera copyOf(SurfaceConfig.Camera source) {
            if (source == null) return null;
            SurfaceConfig.validateCamera(source);
            return new Camera(source.space, source.position[0], source.position[1], source.position[2],
                    source.yaw, source.pitch, source.roll, source.fov, source.distance,
                    source.vision, source.shaders);
        }
    }

    public record Element(String text, String image, String anchor, String pivot,
                          double x, double y, float scale, int width, int height) {}

    public record Hud(int width, int height, String fit, List<Element> elements) {
        public Hud { elements = List.copyOf(elements); }
    }

    public static SurfaceDefinition fromConfig(String id, SurfaceConfig.Feed source) {
        SurfaceConfig.validateFeed(id, source);
        var hud = source.hud;
        return new SurfaceDefinition(source.width, source.height, source.fps, Camera.copyOf(source.camera),
                new Hud(hud.width, hud.height, hud.fit, hud.elements.stream().map(e ->
                        new Element(e.text, e.image, SurfaceConfig.anchor(e.anchor),
                                SurfaceConfig.anchor(e.pivot), e.x, e.y, e.scale, e.width, e.height)).toList()));
    }

}
