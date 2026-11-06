package group.taczexpands.common.paint;

import java.util.Arrays;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;

/** Immutable relative adjustments: R/G/B H/S/V/c, then R/G/B pattern scale. */
public final class PaintState {
    public static final int VERSION = 2, SIZE = 15, MAX_Q = 65535;
    public static final PaintState DEFAULT;

    static {
        int[] q = new int[SIZE];
        Arrays.fill(q, -1);
        DEFAULT = new PaintState(q);
    }

    private final int[] q;

    public static int index(int region, int axis) {
        if (region < 0 || region > 2) throw new IndexOutOfBoundsException(region);
        if (axis < 0 || axis > 4) throw new IndexOutOfBoundsException(axis);
        return axis == 4 ? 12 + region : region * 4 + axis;
    }

    public PaintState(int[] values) {
        if (values == null || values.length != SIZE)
            throw new IllegalArgumentException("Paint Q length");
        q = values.clone();
        for (int value : q)
            if (!isValidQ(value)) throw new IllegalArgumentException("Paint Q range");
    }

    public static boolean isValidQ(int value) {
        return value >= -1 && value <= MAX_Q;
    }

    public int q(int index) {
        return q[index];
    }

    public int[] values() {
        return q.clone();
    }

    public PaintState with(int index, int value) {
        int[] next = values();
        next[index] = value;
        return new PaintState(next);
    }

    public boolean isDefault() {
        return equals(DEFAULT);
    }

    public static PaintState read(CompoundTag attachmentTag) {
        if (attachmentTag == null || !attachmentTag.contains("Extras", Tag.TAG_COMPOUND))
            return DEFAULT;
        CompoundTag extras = attachmentTag.getCompound("Extras");
        if (!extras.contains("Paint", Tag.TAG_COMPOUND)) return DEFAULT;
        CompoundTag paint = extras.getCompound("Paint");
        if (!paint.contains("Version", Tag.TAG_INT) || !paint.contains("Q", Tag.TAG_INT_ARRAY))
            return DEFAULT;
        int version = paint.getInt("Version");
        int[] values = paint.getIntArray("Q");
        if (version == 1 && values.length == 12) {
            values = Arrays.copyOf(values, SIZE);
            Arrays.fill(values, 12, SIZE, -1);
        } else if (version != VERSION || values.length != SIZE) {
            return DEFAULT;
        }
        try {
            return new PaintState(values);
        } catch (IllegalArgumentException invalid) {
            return DEFAULT;
        }
    }

    /** Replaces Extras.Paint while preserving sibling tags, including generated/OEM flags. */
    public void write(CompoundTag attachmentTag) {
        if (attachmentTag == null)
            throw new IllegalArgumentException("Missing installed attachment tag");
        if (isDefault()) {
            if (attachmentTag.contains("Extras", Tag.TAG_COMPOUND))
                attachmentTag.getCompound("Extras").remove("Paint");
            return;
        }
        CompoundTag extras = attachmentTag.getCompound("Extras");
        CompoundTag paint = new CompoundTag();
        paint.putInt("Version", VERSION);
        paint.putIntArray("Q", values());
        extras.put("Paint", paint);
        attachmentTag.put("Extras", extras);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof PaintState state && Arrays.equals(q, state.q);
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(q);
    }
}
