package group.taczexpands.client.paint;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/** Compiled display values with full-path resource IDs. */
public final class PaintConfig {
    private PaintConfig() {}

    public record RegionLabels(String r, String g, String b) {
        public static final RegionLabels EMPTY = new RegionLabels(null, null, null);
        public String label(int index) {
            return switch (index) {
                case 0 -> r;
                case 1 -> g;
                case 2 -> b;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    }

    public record Pbr(ResourceLocation normal, ResourceLocation specular) {}

    public record Base(ResourceLocation texture, ResourceLocation lodTexture, Pbr pbr) {}

    public record Surface(ResourceLocation mask, double repeatU, double repeatV,
                          ResourceLocation lodMask, RegionLabels regionLabels, Pbr pbr, Pbr lodPbr,
                          Map<String, Base> bases) {
        public Surface {
            if (regionLabels == null) regionLabels = RegionLabels.EMPTY;
            bases = Map.copyOf(bases);
        }
        public Surface(ResourceLocation mask, double repeatU, double repeatV, ResourceLocation lodMask,
                       RegionLabels regionLabels, Pbr pbr, Pbr lodPbr) {
            this(mask, repeatU, repeatV, lodMask, regionLabels, pbr, lodPbr, Map.of());
        }
        public Surface(ResourceLocation mask, double repeatU, double repeatV, ResourceLocation lodMask) {
            this(mask, repeatU, repeatV, lodMask, RegionLabels.EMPTY, null, null);
        }
        /** A pack translation key or literal text, with stable generic player-facing fallbacks. */
        public String regionLabel(int index) {
            String label = regionLabels.label(index);
            return label == null ? defaultRegionLabel(index) : label;
        }
    }

    public static String defaultRegionLabel(int index) {
        if (index < 0 || index > 2) throw new IndexOutOfBoundsException(index);
        return "gui.taczexpands.paint.region_" + (index + 1);
    }

    public record Axis(double preset, double min, double max, boolean ranged) {
        public boolean editable() { return ranged && min < max; }
        public double resolve(int q) {
            return editable() && q >= 0 ? min + (max - min) * (q / 65535.0) : preset;
        }
        public int quantize(double value) {
            if (!editable()) return -1;
            return (int) Math.round(65535 * Math.max(0, Math.min(1, (value - min) / (max - min))));
        }
    }

    public record Channel(int baseRgb, Axis h, Axis s, Axis v, Axis mix,
                          ResourceLocation texture, Axis scale, Pbr pbr) {
        public Channel(int baseRgb, Axis h, Axis s, Axis v, Axis mix,
                       ResourceLocation texture, Axis scale) {
            this(baseRgb, h, s, v, mix, texture, scale, null);
        }
        public Axis axis(int index) {
            return switch (index) {
                case 0 -> h;
                case 1 -> s;
                case 2 -> v;
                case 3 -> mix;
                case 4 -> scale;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    }

    public record Recipe(Channel r, Channel g, Channel b, boolean switchPbr, String base) {
        public Recipe(Channel r, Channel g, Channel b, boolean switchPbr) {
            this(r, g, b, switchPbr, null);
        }
        public Recipe(Channel r, Channel g, Channel b) {
            this(r, g, b, false);
        }
        public Channel channel(int index) {
            return switch (index) {
                case 0 -> r;
                case 1 -> g;
                case 2 -> b;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
        public boolean empty() { return r == null && g == null && b == null && base == null; }
    }

    public static Surface compileSurface(JsonElement input, ResourceLocation displayId, Consumer<String> diagnostics) {
        if (input == null || input.isJsonNull()) return null;
        try {
            JsonObject paint = object(input, "paint");
            ResourceLocation mask = resource(paint.get("mask"), "paint.mask");
            double u = 16, v = 16;
            if (paint.has("uv_repeat")) {
                double[] repeat = pair(paint.get("uv_repeat"), "paint.uv_repeat");
                u = positive(repeat[0], "paint.uv_repeat[0]");
                v = positive(repeat[1], "paint.uv_repeat[1]");
            }
            ResourceLocation lod = paint.has("lod_mask")
                    ? resource(paint.get("lod_mask"), "paint.lod_mask") : null;
            return new Surface(mask, u, v, lod, compileLabels(paint.get("region_labels"), displayId, diagnostics),
                    compilePbr(paint.get("pbr"), "paint.pbr", displayId, diagnostics),
                    compilePbr(paint.get("lod_pbr"), "paint.lod_pbr", displayId, diagnostics),
                    compileBases(paint.get("bases"), displayId, diagnostics));
        } catch (Invalid ex) {
            diagnostics.accept(displayId + ": " + ex.getMessage());
            return null;
        }
    }

    private static Map<String, Base> compileBases(JsonElement input, ResourceLocation displayId,
                                                   Consumer<String> diagnostics) {
        if (input == null) return Map.of();
        JsonObject bases;
        try {
            bases = object(input, "paint.bases");
        } catch (Invalid ex) {
            diagnostics.accept(displayId + ": " + ex.getMessage() + "; keeping original base");
            return Map.of();
        }
        Map<String, Base> result = new LinkedHashMap<>();
        for (var entry : bases.entrySet()) {
            String path = "paint.bases." + entry.getKey();
            try {
                if (entry.getKey().isBlank()) throw invalid(path, "expected nonblank base name");
                JsonObject base = object(entry.getValue(), path);
                ResourceLocation texture = resource(base.get("texture"), path + ".texture");
                ResourceLocation lod = base.has("lod_texture")
                        ? resource(base.get("lod_texture"), path + ".lod_texture") : null;
                result.put(entry.getKey(), new Base(texture, lod,
                        compilePbr(base.get("pbr"), path + ".pbr", displayId, diagnostics)));
            } catch (Invalid ex) {
                diagnostics.accept(displayId + ": " + ex.getMessage() + "; keeping original base");
            }
        }
        return result;
    }

    // Skip malformed optional UI labels while retaining the paint surface.
    private static RegionLabels compileLabels(JsonElement input, ResourceLocation displayId, Consumer<String> diagnostics) {
        if (input == null) return RegionLabels.EMPTY;
        JsonObject labels;
        try {
            labels = object(input, "paint.region_labels");
        } catch (Invalid ex) {
            diagnostics.accept(displayId + ": " + ex.getMessage() + "; using generic region labels");
            return RegionLabels.EMPTY;
        }
        String[] values = new String[3];
        String[] names = {"r", "g", "b"};
        for (int i = 0; i < 3; i++) {
            if (!labels.has(names[i])) continue;
            String path = "paint.region_labels." + names[i];
            try {
                String value = string(labels.get(names[i]), path);
                if (value.isBlank()) throw invalid(path, "expected nonblank translation key or literal text");
                values[i] = value;
            } catch (Invalid ex) {
                diagnostics.accept(displayId + ": " + ex.getMessage() + "; using generic region label");
            }
        }
        return new RegionLabels(values[0], values[1], values[2]);
    }

    private static Pbr compilePbr(JsonElement input, String path, ResourceLocation displayId,
                                  Consumer<String> diagnostics) {
        return compilePbr(input, path, message -> diagnostics.accept(displayId + ": " + message));
    }

    private static Pbr compilePbr(JsonElement input, String path, Consumer<String> diagnostics) {
        if (input == null) return null;
        try {
            JsonObject value = object(input, path);
            ResourceLocation normal = value.has("normal") ? resource(value.get("normal"), path + ".normal") : null;
            ResourceLocation specular = value.has("specular") ? resource(value.get("specular"), path + ".specular") : null;
            if (normal == null && specular == null) throw invalid(path, "expected normal and/or specular");
            return new Pbr(normal, specular);
        } catch (Invalid ex) {
            diagnostics.accept(ex.getMessage() + "; keeping original PBR");
            return null;
        }
    }

    public static Recipe compileRecipe(JsonElement input, ResourceLocation displayId, Consumer<String> diagnostics) {
        if (input == null || input.isJsonNull()) return null;
        JsonObject channels;
        boolean switchPbr = false;
        String base = null;
        try {
            JsonObject paint = object(input, "paint");
            channels = paint.has("channels") ? object(paint.get("channels"), "paint.channels") : new JsonObject();
            if (paint.has("base")) {
                JsonElement selected = paint.get("base");
                if (selected.isJsonPrimitive() && selected.getAsJsonPrimitive().isString()
                        && !selected.getAsString().isBlank()) base = selected.getAsString();
                else diagnostics.accept(displayId + ": paint.base: expected nonblank string; keeping original base");
            }
            if (paint.has("switch_pbr")) {
                JsonElement flag = paint.get("switch_pbr");
                if (flag.isJsonPrimitive() && flag.getAsJsonPrimitive().isBoolean()) switchPbr = flag.getAsBoolean();
                else diagnostics.accept(displayId + ": paint.switch_pbr: expected boolean; keeping original PBR");
            }
        } catch (Invalid ex) {
            diagnostics.accept(displayId + ": " + ex.getMessage());
            return null;
        }
        Channel[] result = new Channel[3];
        String[] names = {"r", "g", "b"};
        for (int region = 0; region < 3; region++) {
            String name = names[region];
            if (!channels.has(name)) continue;
            String path = "paint.channels." + name;
            List<String> warnings = new ArrayList<>();
            try {
                result[region] = channel(object(channels.get(name), path), path, warnings);
                warnings.forEach(message -> diagnostics.accept(displayId + ": " + message));
            } catch (Invalid ex) {
                diagnostics.accept(displayId + ": " + ex.getMessage() + "; region skipped");
            }
        }
        return new Recipe(result[0], result[1], result[2], switchPbr, base);
    }

    private static Channel channel(JsonObject input, String path, List<String> warnings) {
        JsonObject base = object(input.get("base_color"), path + ".base_color");
        String hex = string(base.get("value"), path + ".base_color.value");
        if (!hex.matches("#[0-9a-fA-F]{6}")) throw invalid(path + ".base_color.value", "expected #RRGGBB");
        int rgb = Integer.parseInt(hex.substring(1), 16);
        double[] hsv = PaintParameters.toHsv(rgb);
        JsonObject range = base.has("range") ? object(base.get("range"), path + ".base_color.range") : new JsonObject();
        Axis h = axis(hsv[0], range.get("h"), path + ".base_color.range.h", true, hsv[1] == 0, warnings);
        Axis s = axis(hsv[1], range.get("s"), path + ".base_color.range.s", false, false, warnings);
        Axis v = axis(hsv[2], range.get("v"), path + ".base_color.range.v", false, false, warnings);
        Axis scale = scale(input.get("scale"), path + ".scale", warnings);
        ResourceLocation texture = input.has("texture") ? resource(input.get("texture"), path + ".texture") : null;
        Axis mix = new Axis(0, 0, 0, false);
        if (texture != null) {
            JsonObject blend = object(input.get("mix"), path + ".mix");
            double preset = number(blend.get("value"), path + ".mix.value");
            // Presets project to legal boundaries, while illegal ranges invalidate a region.
            double legal = clamp(preset, 0, 1);
            if (legal != preset) warnings.add(path + ".mix.value: preset projected to [0,1]");
            mix = axis(legal, blend.get("range"), path + ".mix.range", false, false, warnings);
        }
        Pbr pbr = compilePbr(input.get("pbr"), path + ".pbr", warnings::add);
        if (pbr != null && texture == null) {
            warnings.add(path + ".pbr: expected a pattern texture; keeping surface PBR");
            pbr = null;
        }
        return new Channel(rgb, h, s, v, mix, texture, scale, pbr);
    }

    private static Axis scale(JsonElement input, String path, List<String> warnings) {
        if (input == null) return new Axis(1, 1, 1, false);
        if (input.isJsonPrimitive()) {
            double value = positive(number(input, path), path);
            return new Axis(value, value, value, false);
        }
        JsonObject object = object(input, path);
        double preset = positive(number(object.get("value"), path + ".value"), path + ".value");
        if (!object.has("range")) return new Axis(preset, preset, preset, false);
        double[] endpoints = pair(object.get("range"), path + ".range");
        double min = positive(endpoints[0], path + ".range[0]");
        double max = positive(endpoints[1], path + ".range[1]");
        if (min > max) throw invalid(path + ".range", "expected increasing positive interval");
        double projected = clamp(preset, min, max);
        if (projected != preset) warnings.add(path + ".value: preset projected to range");
        return new Axis(projected, min, max, true);
    }

    private static Axis axis(double preset, JsonElement range, String path, boolean hue,
                             boolean grayHue, List<String> warnings) {
        if (range == null) return new Axis(preset, preset, preset, false);
        double[] endpoints = pair(range, path);
        double min = endpoints[0], max = endpoints[1];
        if (min > max || (hue ? max - min > 360 : min < 0 || max > 1))
            throw invalid(path, hue ? "expected increasing interval of at most 360 degrees" : "expected interval inside [0,1]");
        if (hue) preset = grayHue ? min : nearestHue(preset, min, max);
        double projected = clamp(preset, min, max);
        if (projected != preset) warnings.add(path + ": preset projected to range");
        return new Axis(projected, min, max, true);
    }

    /** Closest equivalent hue to an unfolded interval; a tie selects the lower angle. */
    static double nearestHue(double hue, double min, double max) {
        double lower = hue + 360 * Math.floor((min - hue) / 360);
        double upper = lower + 360;
        double lowerDistance = distance(lower, min, max), upperDistance = distance(upper, min, max);
        return lowerDistance <= upperDistance ? lower : upper;
    }

    private static double distance(double x, double min, double max) { return Math.max(min - x, Math.max(x - max, 0)); }
    private static double clamp(double x, double min, double max) { return Math.max(min, Math.min(max, x)); }

    private static JsonObject object(JsonElement value, String path) {
        if (value == null || !value.isJsonObject()) throw invalid(path, "expected object");
        return value.getAsJsonObject();
    }
    private static String string(JsonElement value, String path) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString())
            throw invalid(path, "expected string");
        return value.getAsString();
    }
    private static double number(JsonElement value, String path) {
        if (value == null || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isNumber())
            throw invalid(path, "expected finite number");
        double number = value.getAsDouble();
        if (!Double.isFinite(number)) throw invalid(path, "expected finite number");
        return number;
    }
    private static double positive(double value, String path) {
        if (!(value > 0)) throw invalid(path, "expected positive finite number");
        return value;
    }
    private static double[] pair(JsonElement value, String path) {
        if (value == null || !value.isJsonArray() || value.getAsJsonArray().size() != 2)
            throw invalid(path, "expected two numbers");
        return new double[] {number(value.getAsJsonArray().get(0), path + "[0]"), number(value.getAsJsonArray().get(1), path + "[1]")};
    }
    private static ResourceLocation resource(JsonElement value, String path) {
        String id = string(value, path);
        // Require an explicit namespace for author-supplied resource locations.
        ResourceLocation parsed = id.indexOf(':') > 0 ? ResourceLocation.tryParse(id) : null;
        if (parsed == null) throw invalid(path, "expected full namespace:path resource location");
        return parsed;
    }
    private static Invalid invalid(String path, String message) { return new Invalid(path + ": " + message); }
    private static final class Invalid extends RuntimeException {
        private Invalid(String message) { super(message); }
    }
}
