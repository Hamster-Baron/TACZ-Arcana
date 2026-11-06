package group.taczexpands.client.render.flashlight;

import com.mojang.math.Axis;
import group.taczexpands.client.render.FlashlightData;
import group.taczexpands.client.render.surface.SurfaceFeed;
import net.minecraft.client.Camera;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import java.util.ArrayList;
import java.util.List;

/** Cone support against the planes of the main view and this frame's scheduled feeds. */
public final class FlashlightViews {
    private static Vector4f[] planes(Matrix4f matrix) {
        var result = new Vector4f[6];
        for (int i = 0; i < result.length; i++) {
            result[i] = matrix.frustumPlane(i, new Vector4f());
        }
        return result;
    }

    private record View(Vec3 origin, Vector4f[] planes) {
        View(Vec3 origin, Matrix4f projection, Matrix4f view) {
            this(origin, FlashlightViews.planes(new Matrix4f(projection).mul(view)));
        }

        boolean intersects(Vec3 origin, Vector3f direction, float range, float angle) {
            var relative = origin.subtract(this.origin);
            double cosine = Math.cos(Math.toRadians(angle));
            double sine = Math.sin(Math.toRadians(angle));
            for (var p : planes) {
                double apex = p.x * relative.x + p.y * relative.y + p.z * relative.z + p.w;
                double axial = p.x * direction.x + p.y * direction.y + p.z * direction.z;
                double radial = Math.sqrt(Math.max(0, p.x * p.x + p.y * p.y + p.z * p.z - axial * axial));
                double normal = Math.sqrt(p.x * p.x + p.y * p.y + p.z * p.z);
                double support = axial >= normal * cosine ? normal : axial * cosine + radial * sine;
                if (apex + range * Math.max(0, support) < 0) {
                    return false;
                }
            }
            return true;
        }
    }

    private final List<View> views = new ArrayList<>();

    public FlashlightViews(Camera camera, Matrix4f projection, Matrix4f view, List<SurfaceFeed> feeds) {
        views.add(new View(camera.getPosition(), projection, view));
        for (var source : feeds) {
            var feed = source.cameraPose();
            var rotation = new Matrix4f().rotate(Axis.ZP.rotationDegrees(feed.roll()))
                    .rotate(Axis.XP.rotationDegrees(feed.pitch())).rotate(Axis.YP.rotationDegrees(feed.yaw() + 180));
            var lens = new Matrix4f().perspective((float) Math.toRadians(feed.fov()),
                    (float) source.definition().width() / source.definition().height(), 0.05f, feed.distance());
            views.add(new View(feed.position(), lens, rotation));
        }
    }

    public boolean affects(FlashlightData light) {
        for (var view : views) {
            if (view.intersects(light.getCamera().getPosition(), light.getCamera().getLookVector(),
                    light.getRange(), light.getAngle())) {
                return true;
            }
        }
        return false;
    }
}
