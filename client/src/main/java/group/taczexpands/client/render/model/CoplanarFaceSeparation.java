package group.taczexpands.client.render.model;

import com.tacz.guns.client.resource.pojo.model.BedrockModelPOJO;
import com.tacz.guns.client.resource.pojo.model.BonesItem;
import com.tacz.guns.client.resource.pojo.model.CubesItem;
import com.tacz.guns.client.resource.pojo.model.FaceItem;
import com.tacz.guns.client.resource.pojo.model.FaceUVsItem;
import group.taczexpands.client.accessor.IAccessorBedrockModelPOJO;
import group.taczexpands.client.config.ClientConfig;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.Direction;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Separates flush coplanar faces of a model by extending each upper cube outward through that face
 * only. Runs on the shared POJO before TacZ builds any Bedrock or baked model from it.
 */
public final class CoplanarFaceSeparation {
    private CoplanarFaceSeparation() {}

    /** Separates a cached POJO the first time it is handed out; model loaders may ask concurrently. */
    public static void separateOnce(BedrockModelPOJO pojo) {
        synchronized (pojo) {
            if (((IAccessorBedrockModelPOJO) pojo).taczexpands$markDepthSeparated()) separate(pojo);
        }
    }

    static void separate(BedrockModelPOJO pojo) {
        float step = ClientConfig.INSTANCE.getCoplanarFaceSeparation().get().floatValue();
        // Legacy-format geometry uses other conventions; official and current packs use the new format.
        if (step <= 0 || pojo.getGeometryModelNew() == null || pojo.getGeometryModelNew().getBones() == null) return;
        List<CubesItem> cubes = new ArrayList<>();
        List<CoplanarFaceLayers.Box> boxes = new ArrayList<>();
        collect(pojo.getGeometryModelNew().getBones(), cubes, boxes);
        int[][] lifts = CoplanarFaceLayers.lifts(boxes);
        for (int i = 0; i < lifts.length; i++) {
            for (int face = 0; face < 6; face++) {
                if (lifts[i][face] > 0) extend(cubes.get(i), face / 2, face % 2, lifts[i][face] * step);
            }
        }
    }

    /**
     * Moves one face outward along its own axis; the neighbouring faces stretch with it, so the
     * cube stays closed and its other faces keep their positions. Sides are in TacZ's local frame,
     * where Y is flipped: its minimum Y face is the cube's Bedrock top.
     */
    static void extend(CubesItem cube, int axis, int side, float amount) {
        List<Float> origin = cube.getOrigin(), size = cube.getSize();
        float width = size.get(axis);
        // Box UV lays out faces by whole-pixel sizes; a lift crossing an integer would remap the texture.
        if (cube.getFaceUv() == null && (int) (width + amount) != (int) width) return;
        size.set(axis, width + amount);
        if (axis == 1 ? side == 1 : side == 0) origin.set(axis, origin.get(axis) - amount);
    }

    /** Model-space boxes following TacZ's BedrockModel conversion: Y flipped about 24, rotations applied Z, Y, X. */
    static void collect(List<BonesItem> bones, List<CubesItem> cubes, List<CoplanarFaceLayers.Box> boxes) {
        Map<String, BonesItem> byName = new HashMap<>();
        for (BonesItem bone : bones) byName.putIfAbsent(bone.getName(), bone);
        Map<BonesItem, Matrix4f> matrices = new IdentityHashMap<>();
        // A bone list may reuse one cube list for several bones; each cube is separated once.
        Set<CubesItem> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (BonesItem bone : bones) {
            if (bone.getCubes() == null) continue;
            Matrix4f boneMatrix = matrix(bone, byName, matrices);
            List<Float> pivot = bone.getPivot();
            for (CubesItem cube : bone.getCubes()) {
                if (!seen.add(cube)) continue;
                Matrix4f toModel = boneMatrix;
                List<Float> origin = cube.getOrigin(), size = cube.getSize();
                List<Float> base = pivot;
                if (cube.getRotation() != null) {
                    List<Float> cubePivot = cube.getPivot();
                    toModel = new Matrix4f(boneMatrix)
                            .translate(cubePivot.get(0) - pivot.get(0), pivot.get(1) - cubePivot.get(1),
                                    cubePivot.get(2) - pivot.get(2));
                    rotate(toModel, cube.getRotation());
                    base = cubePivot;
                }
                float inflate = cube.getInflate();
                Vector3f min = new Vector3f(origin.get(0) - base.get(0),
                        base.get(1) - origin.get(1) - size.get(1), origin.get(2) - base.get(2)).sub(inflate, inflate, inflate);
                Vector3f max = new Vector3f(min).add(size.get(0), size.get(1), size.get(2)).add(2 * inflate, 2 * inflate, 2 * inflate);
                cubes.add(cube);
                boxes.add(new CoplanarFaceLayers.Box(toModel, min, max, faces(cube.getFaceUv())));
            }
        }
    }

    /** Per-face UV cubes may omit faces; TacZ then draws nothing there. */
    private static int faces(FaceUVsItem uv) {
        if (uv == null) return CoplanarFaceLayers.ALL_FACES;
        // Bit order follows CoplanarFaceLayers: axis * 2 + side, matching TacZ's local min/max faces.
        Direction[] order = {Direction.WEST, Direction.EAST, Direction.DOWN, Direction.UP, Direction.NORTH, Direction.SOUTH};
        int mask = 0;
        for (int bit = 0; bit < order.length; bit++) {
            if (uv.getFace(order[bit]) != FaceItem.EMPTY) mask |= 1 << bit;
        }
        return mask;
    }

    private static Matrix4f matrix(BonesItem bone, Map<String, BonesItem> byName, Map<BonesItem, Matrix4f> matrices) {
        Matrix4f cached = matrices.get(bone);
        if (cached != null) return cached;
        BonesItem parent = bone.getParent() == null ? null : byName.get(bone.getParent());
        List<Float> pivot = bone.getPivot();
        Matrix4f matrix;
        if (parent == null) {
            matrix = new Matrix4f().translate(pivot.get(0), 24 - pivot.get(1), pivot.get(2));
        } else {
            List<Float> parentPivot = parent.getPivot();
            matrix = new Matrix4f(matrix(parent, byName, matrices)).translate(pivot.get(0) - parentPivot.get(0),
                    parentPivot.get(1) - pivot.get(1), pivot.get(2) - parentPivot.get(2));
        }
        if (bone.getRotation() != null) rotate(matrix, bone.getRotation());
        matrices.put(bone, matrix);
        return matrix;
    }

    private static void rotate(Matrix4f matrix, List<Float> degrees) {
        matrix.rotateZ((float) Math.toRadians(degrees.get(2)))
                .rotateY((float) Math.toRadians(degrees.get(1)))
                .rotateX((float) Math.toRadians(degrees.get(0)));
    }
}
