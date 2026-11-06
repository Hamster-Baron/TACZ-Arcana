package group.taczexpands.client.render.surface;

import java.util.Set;

/** Evaluated optional values. Resource JSON retains its separate player/world schema. */
public record SurfaceCameraUpdate(String space, Integer anchor, Double x, Double y, Double z,
                                  Float yaw, Float pitch, Float roll, Long durationMillis,
                                  Float fov, Float distance, String vision, Boolean shaders, String bone) {
    public SurfaceCameraUpdate(String space, Integer anchor, Double x, Double y, Double z,
                               Float yaw, Float pitch, Float roll, Long durationMillis,
                               Float fov, Float distance, String vision, Boolean shaders) {
        this(space, anchor, x, y, z, yaw, pitch, roll, durationMillis, fov, distance, vision, shaders, null);
    }
    public SurfaceCameraUpdate {
        if (space != null && !Set.of("world", "player", "entity", "bone").contains(space))
            throw new IllegalArgumentException("Unknown surface camera space: " + space);
        if ("bone".equals(space) && anchor != null || bone != null && (bone.isBlank() || space != null && !space.equals("bone")))
            throw new IllegalArgumentException("Bone cameras use the receiver's main-hand gun and a bone name");
        if (vision != null && !Set.of("normal", "night", "thermal").contains(vision))
            throw new IllegalArgumentException("Unknown surface camera vision: " + vision);
        if ((anchor != null && anchor < -1) || (durationMillis != null && durationMillis < 0))
            throw new IllegalArgumentException("Invalid camera anchor/duration");
        for (Number value : new Number[]{x, y, z, yaw, pitch, roll, fov, distance})
            if (value != null && !Double.isFinite(value.doubleValue()))
                throw new IllegalArgumentException("Camera values must be finite");
        if (fov != null && (fov <= 1 || fov >= 179) || distance != null && (distance < 16 || distance > 512))
            throw new IllegalArgumentException("Invalid camera FOV/distance");
    }

    public static SurfaceCameraUpdate fromConfig(SurfaceConfig.Camera source) {
        SurfaceConfig.validateCamera(source);
        return new SurfaceCameraUpdate(source.space, null, source.position[0], source.position[1], source.position[2],
                source.yaw, source.pitch, source.roll, 0L, source.fov, source.distance, source.vision, source.shaders);
    }

    private static <T> T value(T update, T previous) { return update == null ? previous : update; }

    SurfaceCameraUpdate merge(SurfaceCameraUpdate previous) {
        if (previous == null) return this;
        Integer nextAnchor = space != null && !space.equals(previous.space) ? anchor : value(anchor, previous.anchor);
        return new SurfaceCameraUpdate(value(space, previous.space), nextAnchor,
                value(x, previous.x), value(y, previous.y), value(z, previous.z),
                value(yaw, previous.yaw), value(pitch, previous.pitch), value(roll, previous.roll),
                durationMillis, value(fov, previous.fov), value(distance, previous.distance),
                value(vision, previous.vision), value(shaders, previous.shaders),
                space != null && !space.equals(previous.space) ? bone : value(bone, previous.bone));
    }

    SurfaceDefinition.Camera settings(SurfaceDefinition.Camera resource) {
        var base = resource == null ? SurfaceDefinition.Camera.copyOf(new SurfaceConfig.Camera()) : resource;
        return new SurfaceDefinition.Camera(value(space, base.space()), value(x, base.x()), value(y, base.y()), value(z, base.z()),
                value(yaw, base.yaw()), value(pitch, base.pitch()), value(roll, base.roll()),
                value(fov, base.fov()), value(distance, base.distance()), value(vision, base.vision()), value(shaders, base.shaders()));
    }
}
