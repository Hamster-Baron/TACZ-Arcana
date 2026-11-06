package group.taczexpands.client.render.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Finds same-facing cube faces that overlap on one plane and assigns each upper face a depth layer.
 * Flush faces depth-fight wherever their texels differ, which camouflage makes visible everywhere;
 * lifting only those faces leaves every other face of the model where the author put it.
 */
public final class CoplanarFaceLayers {
    /**
     * Faces closer than this many model pixels are coplanar. Far below the 0.001 inflate modelers
     * already use to separate faces, so existing separations are respected rather than reordered.
     */
    private static final float PLANE_TOLERANCE = 1e-4f;
    /** In-plane overlap must exceed this many model pixels; faces only touching along an edge pass. */
    private static final float EDGE_TOLERANCE = 1e-3f;

    /** Face index {@code axis * 2 + side}; side 0 is the minimum. Bit of the same index marks a drawn face. */
    public static final int ALL_FACES = 0b111111;

    private CoplanarFaceLayers() {}

    /** An axis-aligned box in its own space; {@code toModel} places it in model space. */
    public record Box(Matrix4f toModel, Vector3f min, Vector3f max, int faces) {
        public Box(Matrix4f toModel, Vector3f min, Vector3f max) {
            this(toModel, min, max, ALL_FACES);
        }
    }

    /** Two overlapping coplanar faces, each as a box index and face index. */
    public record Conflict(int box, int face, int otherBox, int otherFace) {}

    private record Face(int box, int index, Vector3f normal, float distance, Vector3f[] corners) {}

    private record Plane(int x, int y, int z, long distance) {}

    public static List<Conflict> conflicts(List<Box> boxes) {
        List<Conflict> conflicts = new ArrayList<>();
        Map<Plane, List<Face>> planes = new HashMap<>();
        for (int i = 0; i < boxes.size(); i++) {
            for (Face face : faces(i, boxes.get(i))) {
                long slot = Math.round(face.distance / PLANE_TOLERANCE);
                for (long d = slot - 1; d <= slot + 1; d++) {
                    for (Face other : planes.getOrDefault(plane(face.normal, d), List.of())) {
                        if (other.box != i && overlap(face, other)) {
                            conflicts.add(new Conflict(other.box, other.index, i, face.index));
                        }
                    }
                }
                planes.computeIfAbsent(plane(face.normal, slot), key -> new ArrayList<>()).add(face);
            }
        }
        return conflicts;
    }

    /**
     * Layers per box and face index; 0 leaves a face in place. Boxes are ordered by volume, so
     * details on a flush surface rise above it. Each face sits one layer above the highest face it
     * overlaps from a larger box; layers are counted per plane, so a deep stack on one surface does
     * not push the same box's other faces.
     */
    public static int[][] lifts(List<Box> boxes) {
        Integer[] order = new Integer[boxes.size()];
        Arrays.setAll(order, i -> i);
        Arrays.sort(order, Comparator.comparingDouble((Integer i) -> -volume(boxes.get(i))).thenComparingInt(i -> i));
        int[] rank = new int[boxes.size()];
        for (int position = 0; position < order.length; position++) rank[order[position]] = position;
        // Below[upper face] lists the larger boxes' faces it overlaps, as box * 6 + face.
        Map<Integer, List<Integer>> below = new HashMap<>();
        for (Conflict conflict : conflicts(boxes)) {
            boolean firstUpper = rank[conflict.box] > rank[conflict.otherBox];
            int upper = firstUpper ? conflict.box * 6 + conflict.face : conflict.otherBox * 6 + conflict.otherFace;
            int lower = firstUpper ? conflict.otherBox * 6 + conflict.otherFace : conflict.box * 6 + conflict.face;
            below.computeIfAbsent(upper, key -> new ArrayList<>()).add(lower);
        }
        int[][] lifts = new int[boxes.size()][6];
        for (int box : order) {
            for (int face = 0; face < 6; face++) {
                for (int lower : below.getOrDefault(box * 6 + face, List.of())) {
                    lifts[box][face] = Math.max(lifts[box][face], lifts[lower / 6][lower % 6] + 1);
                }
            }
        }
        return lifts;
    }

    private static Plane plane(Vector3f normal, long distance) {
        return new Plane(Math.round(normal.x * 1e4f), Math.round(normal.y * 1e4f), Math.round(normal.z * 1e4f), distance);
    }

    private static double volume(Box box) {
        Vector3f size = new Vector3f(box.max).sub(box.min);
        return (double) size.x * size.y * size.z;
    }

    private static List<Face> faces(int index, Box box) {
        List<Face> faces = new ArrayList<>(6);
        float[] min = {box.min.x, box.min.y, box.min.z}, max = {box.max.x, box.max.y, box.max.z};
        for (int axis = 0; axis < 3; axis++) {
            int u = (axis + 1) % 3, v = (axis + 2) % 3;
            for (int side = 0; side < 2; side++) {
                // An undrawn face cannot depth-fight.
                if ((box.faces >> (axis * 2 + side) & 1) == 0) continue;
                Vector3f[] corners = new Vector3f[4];
                float[][] uv = {{min[u], min[v]}, {max[u], min[v]}, {max[u], max[v]}, {min[u], max[v]}};
                for (int c = 0; c < 4; c++) {
                    float[] point = new float[3];
                    point[axis] = side == 0 ? min[axis] : max[axis];
                    point[u] = uv[c][0];
                    point[v] = uv[c][1];
                    corners[c] = box.toModel.transformPosition(new Vector3f(point[0], point[1], point[2]));
                }
                Vector3f local = new Vector3f();
                local.setComponent(axis, side == 0 ? -1 : 1);
                Vector3f normal = box.toModel.transformDirection(local).normalize();
                faces.add(new Face(index, axis * 2 + side, normal, normal.dot(corners[0]), corners));
            }
        }
        return faces;
    }

    /** Separating-axis test on the two rectangles' edge directions; touching edges do not count. */
    private static boolean overlap(Face a, Face b) {
        for (Face face : new Face[] {a, b}) {
            for (int edge = 1; edge <= 3; edge += 2) {
                Vector3f axis = new Vector3f(face.corners[edge]).sub(face.corners[0]);
                if (axis.lengthSquared() == 0) return false;
                axis.normalize();
                float[] first = project(a, axis), second = project(b, axis);
                if (Math.min(first[1], second[1]) - Math.max(first[0], second[0]) <= EDGE_TOLERANCE) return false;
            }
        }
        return true;
    }

    private static float[] project(Face face, Vector3f axis) {
        float low = Float.POSITIVE_INFINITY, high = Float.NEGATIVE_INFINITY;
        for (Vector3f corner : face.corners) {
            float value = axis.dot(corner);
            low = Math.min(low, value);
            high = Math.max(high, value);
        }
        return new float[] {low, high};
    }
}
