package group.taczexpands.server.module.rotation;

import group.taczexpands.common.network.RotationNetwork;
import group.taczexpands.common.network.s2c.S2CRotationState;
import group.taczexpands.common.network.v2.s2c.S2CActionLockState;
import group.taczexpands.common.rotation.RotationLimit;
import group.taczexpands.common.rotation.RotationMath;
import group.taczexpands.server.config.action.CancelAction;
import group.taczexpands.server.config.action.base.PrepareData;
import group.taczexpands.server.config.action.rotation.RotationSpec;
import group.taczexpands.server.context.Context;
import group.taczexpands.server.skill.ActionManager;
import group.taczexpands.server.skill.Skill;
import group.taczexpands.server.skill.TriggerType;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;

/** Server-thread ownership of active rotation sources, their expiry and client synchronization. */
public final class RotationManager {
    private static final double MOVEMENT_RELEASE_DISTANCE_SQUARED = 0.0004;
    private static final int SYNC_INTERVAL_TICKS = 20;
    private static final Map<ServerPlayer, LinkedHashMap<SourceKey, Entry>> STATES = new IdentityHashMap<>();
    private static final AtomicLong REVISION = new AtomicLong();
    private static long ticks;
    private static boolean initialized;

    private RotationManager() {}

    private record SourceKey(TriggerType status, String identifier) {}

    private static final class Entry {
        final RotationSpec spec;
        final RotationLimit limit;
        final Vec3 position;
        final Pose pose;
        final ItemStack heldItem;
        final int selectedSlot;
        final ResourceKey<Level> dimension;
        long expiry;

        Entry(ServerPlayer player, RotationSpec spec, long expiry) {
            this.spec = spec;
            this.expiry = expiry;
            position = player.position();
            pose = player.getPose();
            heldItem = player.getMainHandItem();
            selectedSlot = player.getInventory().selected;
            dimension = player.level().dimension();

            double yaw = spec.getYawReference().equals("CURRENT") ? player.getYRot() : spec.getYawCenter();
            double pitch = switch (spec.getPitchReference()) {
                case "CURRENT" -> player.getXRot();
                case "FIXED" -> spec.getPitchCenter();
                default -> 0;
            };
            limit = new RotationLimit(RotationMath.wrap(yaw), spec.getYawMin(), spec.getYawMax(),
                Math.max(-90, Math.min(90, pitch + spec.getPitchMin())),
                Math.max(-90, Math.min(90, pitch + spec.getPitchMax())));
        }

        boolean shouldRelease(ServerPlayer player, SourceKey source) {
            return !player.isAlive() || expiry <= ticks || !dimension.equals(player.level().dimension())
                || (source.status != null && !ActionManager.INSTANCE.hasStatus(player, source.status))
                || (spec.getReleaseOnMove() && player.position().distanceToSqr(position) > MOVEMENT_RELEASE_DISTANCE_SQUARED)
                || (spec.getReleaseOnPoseChange() && player.getPose() != pose)
                || (spec.getReleaseOnItemChange()
                    && (player.getMainHandItem() != heldItem || player.getInventory().selected != selectedSlot));
        }
    }

    /** Called by the server entry point. Both dedicated and integrated servers use these events. */
    public static synchronized void initialize() {
        if (initialized) return;
        var bus = MinecraftForge.EVENT_BUS;
        bus.addListener(RotationManager::tick);
        bus.addListener(RotationManager::logout);
        bus.addListener(RotationManager::dimension);
        bus.addListener(RotationManager::respawn);
        bus.addListener(RotationManager::clonePlayer);
        bus.addListener(RotationManager::death);
        bus.addListener(RotationManager::stop);
        initialized = true;
    }

    /** Returns true when the range handler consumes the complete-lock action. */
    public static boolean handle(CancelAction action, Skill skill, Context context, PrepareData prepared) {
        if (action.getAction() != S2CActionLockState.Action.Rotate) return false;

        RotationSpec spec = action.getRotation();
        TriggerType status = action.getStatus();
        Integer duration = action.getDuration();
        String identifier = action.getIdentifier();
        if (identifier == null) identifier = skill.resolveAnonymousActionLockSourceIdentifier(action);
        boolean remove = action.getRemove();
        if (status == null && duration == null) return spec != null;
        if (status == null && !remove && duration < 0) return spec != null;

        SourceKey key = new SourceKey(status, identifier);
        for (Entity target : prepared.getSelector().getTargets(context)) {
            if (!(target instanceof ServerPlayer player)) continue;
            if (spec == null || remove || (status == null && duration == 0)) {
                remove(player, key);
                continue;
            }
            if (!player.isAlive() || !RotationNetwork.supported(player)) continue;
            if (status != null && !ActionManager.INSTANCE.hasStatus(player, status)) continue;

            LinkedHashMap<SourceKey, Entry> entries = STATES.computeIfAbsent(player, ignored -> new LinkedHashMap<>());
            if (entries.size() >= S2CRotationState.MAX_SOURCES && !entries.containsKey(key)) {
                throw new IllegalStateException("Too many rotation range sources for player");
            }
            long expiry = status == null ? ticks + duration : Long.MAX_VALUE;
            Entry previous = entries.get(key);
            if (previous != null && previous.spec.equals(spec)) previous.expiry = expiry;
            else entries.put(key, new Entry(player, spec, expiry));

            // Switch each source between complete and bounded locks by removing its prior mode.
            if (status == null) ActionManager.INSTANCE.removeCancel(player, action.getAction(), identifier);
            else ActionManager.INSTANCE.removeCancel(player, action.getAction(), status, identifier);
            sync(player);
        }
        return spec != null && !remove && !(status == null && duration == 0);
    }

    private static void remove(ServerPlayer player, SourceKey key) {
        var entries = STATES.get(player);
        if (entries == null || entries.remove(key) == null) return;
        if (entries.isEmpty()) STATES.remove(player);
        sync(player);
    }

    /** Called by ActionManager when a status source leaves. */
    public static void leave(ServerPlayer player, TriggerType status) {
        var entries = STATES.get(player);
        if (entries == null || !entries.keySet().removeIf(key -> Objects.equals(key.status, status))) return;
        if (entries.isEmpty()) STATES.remove(player);
        sync(player);
    }

    public static void clear(ServerPlayer player) {
        if (STATES.remove(player) != null) sync(player);
    }

    public static void clearAll() {
        for (ServerPlayer player : List.copyOf(STATES.keySet())) clear(player);
    }

    private static void sync(ServerPlayer player) {
        var entries = STATES.get(player);
        List<RotationLimit> limits = entries == null ? List.of() : entries.values().stream().map(entry -> entry.limit).toList();
        RotationNetwork.send(player, new S2CRotationState(REVISION.incrementAndGet(), player.getId(),
            player.level().dimension().location().toString(), limits));
    }

    public static void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        ticks++;
        for (ServerPlayer player : List.copyOf(STATES.keySet())) {
            var entries = STATES.get(player);
            boolean changed = entries.entrySet().removeIf(source -> source.getValue().shouldRelease(player, source.getKey()));
            if (entries.isEmpty()) STATES.remove(player);
            if (changed || ticks % SYNC_INTERVAL_TICKS == 0) sync(player);
        }
    }

    private static void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) STATES.remove(player);
    }

    private static void dimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    private static void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    private static void clonePlayer(PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer player) clear(player);
    }

    private static void death(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) clear(player);
    }

    private static void stop(ServerStoppingEvent event) {
        STATES.clear();
    }
}
