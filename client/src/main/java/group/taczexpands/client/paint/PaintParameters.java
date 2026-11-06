package group.taczexpands.client.paint;

import group.taczexpands.common.paint.PaintState;

/** Resolves the shared item Q layout into effective colors, blends and pattern scale. */
public final class PaintParameters {
    private PaintParameters() {}

    public record Effective(int rgb, double mix, double scale) {}
    public record Resolved(Effective r, Effective g, Effective b) {
        public Effective channel(int index) {
            return switch (index) {
                case 0 -> r;
                case 1 -> g;
                case 2 -> b;
                default -> throw new IndexOutOfBoundsException(index);
            };
        }
    }

    public static Resolved resolve(PaintConfig.Recipe recipe, int[] q) {
        if (q == null || q.length != PaintState.SIZE) throw new IllegalArgumentException("Q must have exactly " + PaintState.SIZE + " entries");
        for (int value : q) if (!PaintState.isValidQ(value)) throw new IllegalArgumentException("Q outside -1..65535");
        Effective[] values = new Effective[3];
        if (recipe != null) for (int region = 0; region < 3; region++) {
            PaintConfig.Channel channel = recipe.channel(region);
            if (channel == null) continue;
            int offset = PaintState.index(region, 0);
            double h = channel.h().resolve(q[offset]);
            double s = channel.s().resolve(q[offset + 1]);
            double v = channel.v().resolve(q[offset + 2]);
            int rgb = !channel.h().ranged() && !channel.s().ranged() && !channel.v().ranged()
                    ? channel.baseRgb() : fromHsv(h, s, v);
            double mix = channel.texture() == null ? 0 : channel.mix().resolve(q[offset + 3]);
            double scale = mix == 0 && channel.pbr() == null ? 1
                    : channel.scale().resolve(q[PaintState.index(region, 4)]);
            values[region] = new Effective(rgb, mix, scale);
        }
        return new Resolved(values[0], values[1], values[2]);
    }

    public static double[] toHsv(int rgb) {
        double r = ((rgb >>> 16) & 255) / 255.0, g = ((rgb >>> 8) & 255) / 255.0, b = (rgb & 255) / 255.0;
        double max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), delta = max - min;
        double h = delta == 0 ? 0 : max == r ? 60 * ((g - b) / delta % 6)
                : max == g ? 60 * ((b - r) / delta + 2) : 60 * ((r - g) / delta + 4);
        return new double[] {h < 0 ? h + 360 : h, max == 0 ? 0 : delta / max, max};
    }

    public static int fromHsv(double degrees, double saturation, double value) {
        double h = (degrees % 360 + 360) % 360 / 60;
        double c = value * saturation, x = c * (1 - Math.abs(h % 2 - 1)), m = value - c;
        double r, g, b;
        if (h < 1) {
            r = c;
            g = x;
            b = 0;
        }
        else if (h < 2) {
            r = x;
            g = c;
            b = 0;
        }
        else if (h < 3) {
            r = 0;
            g = c;
            b = x;
        }
        else if (h < 4) {
            r = 0;
            g = x;
            b = c;
        }
        else if (h < 5) {
            r = x;
            g = 0;
            b = c;
        }
        else {
            r = c;
            g = 0;
            b = x;
        }
        return ((int) Math.round((r + m) * 255) << 16)
                | ((int) Math.round((g + m) * 255) << 8) | (int) Math.round((b + m) * 255);
    }
}
