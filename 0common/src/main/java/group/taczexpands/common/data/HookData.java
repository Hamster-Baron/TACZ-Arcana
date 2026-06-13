package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.network.FriendlyByteBuf;

public class HookData {
    @SerializedName("is_hook")
    public boolean isHook = false;

    @SerializedName("max_length")
    public float maxLength = 32f;

    @SerializedName("type")
    public HookType type = HookType.CHASING;

    @SerializedName("force")
    public double force = 1f;

    @SerializedName("run_lock")
    public boolean runLock = false;

    @SerializedName("jump_lock")
    public boolean jumpLock = false;

    @SerializedName("reload_lock")
    public boolean reloadLock = false;

    @SerializedName("inspect_lock")
    public boolean inspectLock = false;

    @SerializedName("move_lock")
    public boolean moveLock = false;

    @SerializedName("inventory_lock")
    public boolean inventoryLock = false;

    @SerializedName("acceleration")
    public float accelerationPerTick = 2.0f;

    public void serialize(FriendlyByteBuf buffer) {
        buffer.writeBoolean(isHook);
        buffer.writeFloat(maxLength);
        buffer.writeEnum(type);
        buffer.writeDouble(force);
        buffer.writeBoolean(runLock);
        buffer.writeBoolean(jumpLock);
        buffer.writeBoolean(reloadLock);
        buffer.writeBoolean(inspectLock);
        buffer.writeBoolean(moveLock);
        buffer.writeBoolean(inventoryLock);
        buffer.writeFloat(accelerationPerTick);
    }

    public static HookData deserialize(FriendlyByteBuf buffer) {
        HookData hookData = new HookData();
        hookData.isHook = buffer.readBoolean();
        hookData.maxLength = buffer.readFloat();
        hookData.type = buffer.readEnum(HookType.class);
        hookData.force = buffer.readDouble();
        hookData.runLock = buffer.readBoolean();
        hookData.jumpLock = buffer.readBoolean();
        hookData.reloadLock = buffer.readBoolean();
        hookData.inspectLock = buffer.readBoolean();
        hookData.moveLock = buffer.readBoolean();
        hookData.inventoryLock = buffer.readBoolean();
        hookData.accelerationPerTick = buffer.readFloat();
        return hookData;
    }
}
