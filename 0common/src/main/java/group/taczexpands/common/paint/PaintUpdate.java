package group.taczexpands.common.paint;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Bounded paint payload carried by the ordered C2SRaw channel. */
public record PaintUpdate(int slot, String gunUuid, ResourceLocation moduleId, PaintState state) {
    public static final int TYPE_PAINT_UPDATE = 3;
    public static final int PROTOCOL_VERSION = 2;
    public static final int MAX_BYTES = 100000, MAX_ID_CHARS = 32767, MAX_UUID_CHARS = 64;

    public PaintUpdate {
        if (slot < 0
                || slot > 8
                || gunUuid == null
                || gunUuid.isEmpty()
                || gunUuid.length() > MAX_UUID_CHARS
                || moduleId == null
                || moduleId.toString().length() > MAX_ID_CHARS
                || state == null) throw new IllegalArgumentException("Paint target");
    }

    public byte[] encode() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(slot);
            buffer.writeUtf(gunUuid, MAX_UUID_CHARS);
            buffer.writeResourceLocation(moduleId);
            for (int q : state.values()) buffer.writeInt(q);
            byte[] bytes = new byte[buffer.readableBytes()];
            buffer.readBytes(bytes);
            return bytes;
        } finally {
            buffer.release();
        }
    }

    public static PaintUpdate decode(byte[] bytes) {
        if (bytes == null
                || bytes.length < PaintState.SIZE * Integer.BYTES + 4
                || bytes.length > MAX_BYTES) return null;
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {
            if (buffer.readUnsignedByte() != PROTOCOL_VERSION) return null;
            int slot = buffer.readUnsignedByte();
            String uuid = buffer.readUtf(MAX_UUID_CHARS);
            ResourceLocation module = buffer.readResourceLocation();
            if (buffer.readableBytes() != PaintState.SIZE * Integer.BYTES) return null;
            int[] q = new int[PaintState.SIZE];
            for (int i = 0; i < q.length; i++) q[i] = buffer.readInt();
            return new PaintUpdate(slot, uuid, module, new PaintState(q));
        } catch (RuntimeException malformed) {
            return null;
        } finally {
            buffer.release();
        }
    }
}
