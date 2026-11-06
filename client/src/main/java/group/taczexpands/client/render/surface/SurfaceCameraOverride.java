package group.taczexpands.client.render.surface;

/** Stores per-feed overrides separately from resource definitions across resource replacement. */
final class SurfaceCameraOverride {
    final SurfaceCameraUpdate values;
    final SurfaceCameraPose start;
    final long startedNanos, durationMillis;

    SurfaceCameraOverride(SurfaceCameraUpdate values, SurfaceCameraPose start, long now, long duration) {
        this.values = values;
        this.start = start;
        this.startedNanos = now;
        this.durationMillis = duration;
    }

    // A null update explicitly disables the Java API's camera until the override is removed.
    boolean enabled() { return values != null; }

    SurfaceCameraPose interpolate(SurfaceCameraPose target, long now) {
        if (target == null || start == null || durationMillis == 0) return target;
        double progress = (now - startedNanos) / 1_000_000.0 / durationMillis;
        return progress >= 1 ? target : target.blendFrom(start, (float) progress);
    }
}
