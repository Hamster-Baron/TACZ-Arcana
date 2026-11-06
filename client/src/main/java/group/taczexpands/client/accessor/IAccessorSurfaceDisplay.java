package group.taczexpands.client.accessor;

import group.taczexpands.client.render.surface.SurfaceConfig;

import java.util.List;

/** Per-display bindings, shared by its primary and LOD model instances. */
public interface IAccessorSurfaceDisplay {
    List<SurfaceConfig.Face> taczexpands$getSurfaceFaces();
}
