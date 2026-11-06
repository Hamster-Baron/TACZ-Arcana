package group.taczexpands.client.paint;

import java.util.Arrays;

/** All pixel arrays use 0xAARRGGBB, row-major. NativeImage ABGR conversion belongs at the IO boundary. */
public final class PaintCompositor {
    private PaintCompositor() {}
    private static final double[] LINEAR = new double[256];
    private static final float[] SHADOW = new float[256], HIGHLIGHT = new float[256];
    static {
        for (int i = 0; i < LINEAR.length; i++) {
            SHADOW[i] = (float) (Math.max(0, (128 - i) / 128.0) * 0.90);
            HIGHLIGHT[i] = (float) (Math.max(0, (i - 128) / 127.0) * 0.25);
            double value = i / 255.0;
            LINEAR[i] = value <= 0.04045 ? value / 12.92 : Math.pow((value + 0.055) / 1.055, 2.4);
        }
    }

    /** Computes statistics and detail factors for the complete immutable template. */
    public static final class Template {
        private final int width, height, invalidPixels;
        private final byte[] region, alpha, detail;
        private final int[] counts, medians;

        private Template(int width, int height, int invalidPixels, byte[] region, byte[] alpha,
                         byte[] detail, int[] counts, int[] medians) {
            this.width = width;
            this.height = height;
            this.invalidPixels = invalidPixels;
            this.region = region;
            this.alpha = alpha;
            this.detail = detail;
            this.counts = counts;
            this.medians = medians;
        }
        public int width() {
            return width;
        }
        public int height() {
            return height;
        }
        public int invalidPixels() {
            return invalidPixels;
        }
        public boolean hasRegion(int index) {
            return counts[index] > 0;
        }
        public int count(int index) {
            return counts[index];
        }
        /** -1 denotes an absent region. */
        public int median(int index) {
            return medians[index];
        }
    }

    /** An immutable, opaque continuous pattern. Unsupported alpha is rejected before composition. */
    public static final class Pattern {
        private final int width, height;
        private final int[] argb;
        public Pattern(int width, int height, int[] argb) {
            checkSize(width, height, argb);
            for (int pixel : argb) if ((pixel >>> 24) != 255)
                throw new IllegalArgumentException("Paint patterns must be fully opaque");
            this.width = width;
            this.height = height;
            this.argb = argb.clone();
        }
        public int width() {
            return width;
        }
        public int height() {
            return height;
        }
        private int sample(double u, double v) {
            int x = Math.min(width - 1, (int) (fract(u) * width));
            int y = Math.min(height - 1, (int) (fract(v) * height));
            return argb[y * width + x];
        }
    }

    public static Template prepare(int width, int height, int[] maskArgb) {
        checkSize(width, height, maskArgb);
        int size = maskArgb.length, invalid = 0;
        byte[] regions = new byte[size], alphas = new byte[size];
        Arrays.fill(regions, (byte) -1);
        int[][] histogram = new int[3][256];
        int[] counts = new int[3], medians = {-1, -1, -1};
        for (int i = 0; i < size; i++) {
            int pixel = maskArgb[i], alpha = pixel >>> 24;
            if (alpha == 0) continue;
            int r = (pixel >>> 16) & 255, g = (pixel >>> 8) & 255, b = pixel & 255;
            int active = (r > 0 ? 1 : 0) + (g > 0 ? 1 : 0) + (b > 0 ? 1 : 0);
            if (active == 0) continue;
            if (active > 1) {
                invalid++;
                continue;
            }
            int region = r > 0 ? 0 : g > 0 ? 1 : 2, gray = r + g + b;
            regions[i] = (byte) region;
            alphas[i] = (byte) alpha;
            histogram[region][gray]++;
            counts[region]++;
        }
        for (int region = 0; region < 3; region++) {
            if (counts[region] == 0) continue;
            int target = (counts[region] - 1) / 2, cumulative = 0;
            for (int gray = 1; gray < 256; gray++) {
                cumulative += histogram[region][gray];
                if (cumulative > target) {
                    medians[region] = gray;
                    break;
                }
            }
        }
        byte[] details = new byte[size];
        for (int i = 0; i < size; i++) {
            int region = regions[i];
            if (region < 0) continue;
            int gray = ((maskArgb[i] >>> 16) & 255) + ((maskArgb[i] >>> 8) & 255) + (maskArgb[i] & 255);
            double residual = Math.log((LINEAR[gray] + 0.001) / (LINEAR[medians[region]] + 0.001)) / Math.log(2);
            long detail = Math.round(Math.max(0, Math.min(255, 128 + 24 * residual)));
            details[i] = (byte) detail;
        }
        return new Template(width, height, invalid, regions, alphas, details, counts, medians);
    }

    /** A missing pattern disables its region when effective c is nonzero. */
    public static int[] compose(int width, int height, int[] originalArgb, Template template,
                                PaintConfig.Surface gun, PaintConfig.Recipe recipe,
                                PaintParameters.Resolved parameters, Pattern[] patterns) {
        checkSize(width, height, originalArgb);
        int[] output = originalArgb.clone();
        if (template == null || gun == null || recipe == null || parameters == null
                || template.width != width || template.height != height) return output;
        if (patterns != null && patterns.length != 3) throw new IllegalArgumentException("Expected three pattern slots");
        double[][] base = new double[3][];
        boolean[] enabled = new boolean[3];
        for (int region = 0; region < 3; region++) {
            PaintConfig.Channel channel = recipe.channel(region);
            PaintParameters.Effective effective = parameters.channel(region);
            if (channel == null || effective == null) continue;
            enabled[region] = effective.mix() == 0 || (patterns != null && patterns[region] != null);
            int rgb = effective.rgb();
            base[region] = new double[] {LINEAR[(rgb >>> 16) & 255], LINEAR[(rgb >>> 8) & 255], LINEAR[rgb & 255]};
        }
        for (int i = 0; i < output.length; i++) {
            int region = template.region[i];
            if (region < 0 || !enabled[region]) continue;
            PaintParameters.Effective effective = parameters.channel(region);
            double mix = effective.mix();
            int pattern = 0;
            if (mix != 0) {
                double u = ((i % width + 0.5) / width) * (gun.repeatU() / effective.scale());
                double v = ((i / width + 0.5) / height) * (gun.repeatV() / effective.scale());
                pattern = patterns[region].sample(u, v);
            }
            int original = originalArgb[i], rgb = 0;
            double coverage = (template.alpha[i] & 255) / 255.0;
            int detail = template.detail[i] & 255;
            double shade = (1 - SHADOW[detail]) * (1 - HIGHLIGHT[detail]);
            for (int component = 0; component < 3; component++) {
                int shift = 16 - component * 8;
                double color = (1 - mix) * base[region][component] + mix * LINEAR[(pattern >>> shift) & 255];
                double painted = color * shade + HIGHLIGHT[detail];
                double linear = LINEAR[(original >>> shift) & 255] * (1 - coverage) + painted * coverage;
                rgb |= toSrgb(linear) << shift;
            }
            output[i] = (original & 0xff000000) | rgb;
        }
        return output;
    }

    /** Replaces encoded material pixels verbatim; transparent pattern texels retain the surface map. */
    public static int[] composePbr(Template template, PaintConfig.Surface gun,
                                    PaintParameters.Resolved parameters, PaintPbr.Image background,
                                    PaintPbr.Image[] patterns, boolean normal) {
        int width = template.width, height = template.height;
        int[] output = null;
        for (int i = 0; i < width * height; i++) {
            int region = template.region[i];
            if (region < 0 || parameters.channel(region) == null || patterns[region] == null) continue;
            double scale = parameters.channel(region).scale();
            double u = ((i % width + 0.5) / width) * (gun.repeatU() / scale);
            double v = ((i / width + 0.5) / height) * (gun.repeatV() / scale);
            int pixel = patterns[region].sample(u, v);
            if ((pixel >>> 24) == 0) continue;
            if (output == null) {
                output = new int[width * height];
                if (background == null) Arrays.fill(output, PaintPbr.defaultPixel(normal));
                else for (int j = 0; j < output.length; j++) {
                    output[j] = background.sample((j % width + 0.5) / width, (j / width + 0.5) / height);
                }
            }
            output[i] = pixel;
        }
        return output;
    }

    private static int toSrgb(double linear) {
        linear = Math.max(0, Math.min(1, linear));
        double srgb = linear <= 0.0031308 ? 12.92 * linear : 1.055 * Math.pow(linear, 1 / 2.4) - 0.055;
        return (int) Math.round(srgb * 255);
    }
    private static double fract(double value) {
        return value - Math.floor(value);
    }
    private static void checkSize(int width, int height, int[] pixels) {
        if (width <= 0 || height <= 0 || pixels == null || (long) width * height != pixels.length)
            throw new IllegalArgumentException("Pixel dimensions do not match array length");
    }
}
