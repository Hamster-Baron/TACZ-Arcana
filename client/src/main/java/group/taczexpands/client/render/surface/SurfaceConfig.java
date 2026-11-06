package group.taczexpands.client.render.surface;

import com.google.gson.annotations.SerializedName;

import java.util.*;

/** Small, resource-pack friendly authoring format. Coordinates use Minecraft conventions. */
public final class SurfaceConfig {
    @SerializedName("feeds")
    public Map<String, Feed> feeds = new LinkedHashMap<>();

    public static final class Feed {
        @SerializedName("width")
        public int width = 480;

        @SerializedName("height")
        public int height = 270;

        @SerializedName("fps")
        public int fps = 20;

        @SerializedName("camera")
        public Camera camera;

        @SerializedName("hud")
        public Hud hud = new Hud();
    }

    public static final class Camera {
        @SerializedName("space")
        public String space = "player";

        @SerializedName("position")
        public double[] position = {0, 0, 0};

        @SerializedName("yaw")
        public float yaw = 0;

        @SerializedName("pitch")
        public float pitch = 0;

        @SerializedName("roll")
        public float roll = 0;

        @SerializedName("fov")
        public float fov = 70;

        @SerializedName("distance")
        public float distance = 96;

        @SerializedName("vision")
        public String vision = "normal";

        @SerializedName("shaders")
        public boolean shaders = false;
    }

    public static final class Hud {
        @SerializedName("width")
        public int width = 480;

        @SerializedName("height")
        public int height = 270;

        @SerializedName("fit")
        public String fit = "stretch";

        @SerializedName("elements")
        public List<Element> elements = new ArrayList<>();
    }

    public static final class Element {
        @SerializedName("text")
        public String text;

        @SerializedName("image")
        public String image;

        @SerializedName("anchor")
        public String anchor = "center";

        @SerializedName("pivot")
        public String pivot = "center";

        @SerializedName("x")
        public double x = 0;

        @SerializedName("y")
        public double y = 0;

        @SerializedName("scale")
        public float scale = 1;

        @SerializedName("width")
        public int width = 16;

        @SerializedName("height")
        public int height = 16;
    }

    public static final class Face {
        @SerializedName("bone")
        public String bone;

        @SerializedName("feed")
        public String feed;

        @SerializedName("cube")
        public int cube = 0;

        @SerializedName("face")
        public String face = "north";

        @SerializedName("rotation")
        public int rotation = 0;

        @SerializedName("mirror")
        public boolean mirror = false;
    }

    public void validate() {
        if (feeds == null) throw new IllegalArgumentException("feeds must be an object");
        feeds.forEach(SurfaceConfig::validateFeed);
    }

    public static void validateFeed(String id, Feed f) {
        try {
            if (id == null || id.isBlank() || f == null)
                throw new IllegalArgumentException("Empty feed");
            if (f.width < 16
                    || f.height < 16
                    || f.width > 2048
                    || f.height > 2048
                    || f.fps < 1
                    || f.fps > 120)
                throw new IllegalArgumentException("size must be 16..2048 and fps 1..120");
            if (f.hud == null
                    || f.hud.width <= 0
                    || f.hud.height <= 0
                    || f.hud.fit == null
                    || !Set.of("stretch", "contain", "cover", "center_crop").contains(f.hud.fit))
                throw new IllegalArgumentException("invalid HUD canvas/fit");
            if (f.camera != null) validateCamera(f.camera);
            if (f.hud.elements == null)
                throw new IllegalArgumentException("hud.elements must be an array");
            for (int i = 0; i < f.hud.elements.size(); i++) {
                Element e = f.hud.elements.get(i);
                if (e == null)
                    throw new IllegalArgumentException("hud.elements[" + i + "] must be an object");
                anchor(e.anchor);
                anchor(e.pivot);
                if (e.scale <= 0
                        || !Float.isFinite(e.scale)
                        || !Double.isFinite(e.x)
                        || !Double.isFinite(e.y))
                    throw new IllegalArgumentException(
                            "hud.elements[" + i + "]: invalid placement");
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(id + ": " + e.getMessage(), e);
        }
    }

    public static List<Face> parseFaces(Face[] definitions) {
        if (definitions == null) return List.of();
        List<Face> faces = Arrays.asList(definitions);
        validateFaces(faces);
        return List.copyOf(faces);
    }

    public static void validateFaces(List<Face> faces) {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < faces.size(); i++) {
            Face f = faces.get(i);
            if (f == null) throw new IllegalArgumentException("faces[" + i + "] must be an object");
            if (f.bone == null
                    || f.bone.isBlank()
                    || f.cube < 0
                    || f.feed == null
                    || f.feed.isBlank()) throw new IllegalArgumentException("Invalid face binding");
            faceIndex(f.face);
            if (f.rotation % 90 != 0)
                throw new IllegalArgumentException("Face rotation must be multiples of 90");
            if (!seen.add(f.bone + "/" + f.cube + "/" + f.face))
                throw new IllegalArgumentException("Duplicate face binding: " + f.bone);
        }
    }

    public static void validateCamera(Camera c) {
        if (c == null
                || c.space == null
                || c.vision == null
                || !Set.of("player", "world").contains(c.space)
                || c.position == null
                || c.position.length != 3
                || !Set.of("normal", "night", "thermal").contains(c.vision))
            throw new IllegalArgumentException("Invalid camera space/position/vision");
        for (double x : c.position)
            if (!Double.isFinite(x))
                throw new IllegalArgumentException("Camera position must be finite");
        if (!Float.isFinite(c.yaw)
                || !Float.isFinite(c.pitch)
                || !Float.isFinite(c.roll)
                || !Float.isFinite(c.fov)
                || c.fov <= 1
                || c.fov >= 179
                || !Float.isFinite(c.distance)
                || c.distance < 16
                || c.distance > 512)
            throw new IllegalArgumentException("Invalid camera angles/FOV/distance");
    }

    public static int faceIndex(String name) {
        if (name == null) throw new IllegalArgumentException("Face name must not be null");
        return switch (name) {
            case "east" -> 0;
            case "west" -> 1;
            case "down" -> 2;
            case "up" -> 3;
            case "north" -> 4;
            case "south" -> 5;
            default -> throw new IllegalArgumentException("Unknown face: " + name);
        };
    }

    public static String anchor(String value) {
        if (value == null) throw new IllegalArgumentException("HUD anchor/pivot must not be null");
        String s = value.toUpperCase(Locale.ROOT);
        if (!Set.of(
                        "TOP_LEFT",
                        "TOP_CENTER",
                        "TOP_RIGHT",
                        "CENTER_LEFT",
                        "CENTER",
                        "CENTER_RIGHT",
                        "BOTTOM_LEFT",
                        "BOTTOM_CENTER",
                        "BOTTOM_RIGHT")
                .contains(s)) throw new IllegalArgumentException("Unknown anchor: " + value);
        return s;
    }
}
