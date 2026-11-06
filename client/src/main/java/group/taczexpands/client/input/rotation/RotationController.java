package group.taczexpands.client.input.rotation;

import group.taczexpands.common.network.s2c.S2CRotationState;
import group.taczexpands.common.rotation.RotationLimit;
import group.taczexpands.common.rotation.RotationMath;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Client-main-thread state for applying the server's resolved rotation limits to local input. */
public final class RotationController {
    private static final int LEASE_TICKS = 60;
    private static final double MOUSE_TURN_SCALE = 0.15;

    private static List<RotationLimit> limits = List.of();
    private static Entity owner;
    private static ClientLevel level;
    private static long revision = -1;
    private static int lease;
    private static boolean applyingTurn;
    private static boolean initialized;

    private RotationController() {}

    /** Called by the client entry point to register rotation lifecycle events. */
    public static synchronized void initialize() {
        if (initialized) return;
        MinecraftForge.EVENT_BUS.addListener(RotationController::tick);
        MinecraftForge.EVENT_BUS.addListener(RotationController::logout);
        initialized = true;
    }

    public static void accept(S2CRotationState packet) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.level == null || packet.revision() <= revision) return;
        if (packet.entityId() != minecraft.player.getId()
                || !packet.dimension().equals(minecraft.level.dimension().location().toString())) return;

        revision = packet.revision();
        limits = packet.limits();
        owner = minecraft.player;
        level = minecraft.level;
        lease = LEASE_TICKS;
    }

    /** Called from the Entity.turn injection after higher-priority action locks. */
    public static boolean turn(Entity entity, double yaw, double pitch) {
        if (applyingTurn || limits.isEmpty() || entity != owner || lease <= 0) return false;
        Minecraft minecraft = Minecraft.getInstance();
        if (entity != minecraft.player || minecraft.level != level || !entity.isAlive()) return false;

        double oldYaw = entity.getYRot();
        double oldPitch = entity.getXRot();
        double[] next = RotationMath.apply(limits, oldYaw, oldPitch,
            oldYaw + yaw * MOUSE_TURN_SCALE, oldPitch + pitch * MOUSE_TURN_SCALE);
        applyingTurn = true;
        try {
            entity.turn((next[0] - oldYaw) / MOUSE_TURN_SCALE, (next[1] - oldPitch) / MOUSE_TURN_SCALE);
        } finally {
            applyingTurn = false;
        }
        return true;
    }

    private static void clear() {
        limits = List.of();
        owner = null;
        level = null;
        lease = 0;
    }

    private static void tick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft minecraft = Minecraft.getInstance();
        if (owner != minecraft.player || level != minecraft.level
                || (owner != null && !owner.isAlive()) || --lease <= 0) clear();
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        clear();
        revision = -1;
    }
}
