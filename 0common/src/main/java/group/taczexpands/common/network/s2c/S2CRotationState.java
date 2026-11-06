package group.taczexpands.common.network.s2c;

import group.taczexpands.common.rotation.RotationLimit;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;

/** Encodes and decodes the rotation_range v1 wire format. */
public record S2CRotationState(long revision, int entityId, String dimension, List<RotationLimit> limits) {
    public static final int MAX_SOURCES = 32;

    public S2CRotationState {
        limits = List.copyOf(limits);
        if (limits.size() > MAX_SOURCES) throw new IllegalArgumentException("rotation source count exceeds32");
    }

    public static void encode(S2CRotationState packet, FriendlyByteBuf buffer) {
        buffer.writeLong(packet.revision);
        buffer.writeInt(packet.entityId);
        buffer.writeUtf(packet.dimension);
        buffer.writeVarInt(packet.limits.size());
        for (RotationLimit limit : packet.limits) {
            buffer.writeDouble(limit.yawCenter());
            buffer.writeDouble(limit.yawMin());
            buffer.writeDouble(limit.yawMax());
            buffer.writeDouble(limit.pitchMin());
            buffer.writeDouble(limit.pitchMax());
        }
    }

    public static S2CRotationState decode(FriendlyByteBuf buffer) {
        long revision = buffer.readLong();
        int entityId = buffer.readInt();
        String dimension = buffer.readUtf();
        int count = buffer.readVarInt();
        if (count < 0 || count > MAX_SOURCES) throw new IllegalArgumentException("rotation source count exceeds32");

        List<RotationLimit> limits = new ArrayList<>(count);
        for (int index = 0; index < count; index++) {
            limits.add(new RotationLimit(buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                                         buffer.readDouble(), buffer.readDouble()));
        }
        return new S2CRotationState(revision, entityId, dimension, limits);
    }
}
