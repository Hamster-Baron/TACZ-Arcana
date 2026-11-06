package group.taczexpands.client.accessor;

import group.taczexpands.client.render.surface.SurfaceConfig;

import java.util.List;

/** Binding is performed before TacZ publishes the loaded model. */
public interface IAccessorSurfaceCube {
    void taczexpands$bindSurfaceFaces(List<SurfaceConfig.Face> faces);
}
