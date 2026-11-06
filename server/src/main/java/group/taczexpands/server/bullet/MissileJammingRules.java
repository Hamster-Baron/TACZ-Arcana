package group.taczexpands.server.bullet;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.entity.EntityKineticBullet;
import group.taczexpands.common.accessor.IAccessorGunData;
import java.util.*;
import java.util.function.Predicate;
import kotlin.Pair;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;

public final class MissileJammingRules {
    private MissileJammingRules() {}
    // Cache parsed rule data under weak holder keys to allow resource generations to be collected.
    private static final Map<Object, Map<Predicate<Entity>, Pair<Double, Double>>> CACHE = new WeakHashMap<>();

    public static synchronized void clearCaches() { CACHE.clear(); }

    public static Map<Predicate<Entity>, Pair<Double, Double>> resolve(
            Map<Predicate<Entity>, Pair<Double, Double>> global, EntityKineticBullet bullet) {
        if (bullet.getGunId() == null) return global;
        var index = TimelessAPI.getCommonGunIndex(bullet.getGunId()).orElse(null);
        if (index == null || index.getGunData() == null) return global;
        var holder = IAccessorGunData.getExtraHolder(index.getGunData());
        if (holder == null || holder.jammingEntities == null || holder.jammingEntities.isEmpty()) return global;
        synchronized (MissileJammingRules.class) {
            var cached = CACHE.get(holder);
            if (cached != null) return cached;
            var result = merge(global, holder.jammingEntities);
            CACHE.put(holder, result);
            return result;
        }
    }

    static Map<Predicate<Entity>, Pair<Double, Double>> merge(
            Map<Predicate<Entity>, Pair<Double, Double>> global, List<String> lines) {
        Map<String, Rule> local = new LinkedHashMap<>();
        for (String line : lines) {
            if (line == null) continue;
            String[] parts = line.replace(" ", "").split(",", -1);
            if (parts.length != 3) continue;
            try {
                double efficiency = Double.parseDouble(parts[1]);
                double radius = Double.parseDouble(parts[2]);
                if (!Double.isFinite(efficiency) || !Double.isFinite(radius) || efficiency < 0 || radius < 0) continue;
                boolean tag = parts[0].startsWith("#");
                ResourceLocation id = ResourceLocation.tryParse(tag ? parts[0].substring(1) : parts[0]);
                if (id == null) continue;
                Predicate<Entity> matches;
                if (tag) {
                    TagKey<EntityType<?>> key = TagKey.create(Registries.ENTITY_TYPE, id);
                    matches = entity -> entity.getType().is(key);
                } else {
                    if (!ForgeRegistries.ENTITY_TYPES.containsKey(id)) continue;
                    EntityType<?> type = ForgeRegistries.ENTITY_TYPES.getValue(id);
                    if (type == null) continue;
                    matches = entity -> entity.getType() == type;
                }
                local.put((tag ? "#" : "") + id, new Rule(matches, new Pair<>(efficiency, radius)));
            } catch (NumberFormatException ignored) {
                // Skip malformed optional entries and keep the valid rules.
            }
        }
        if (local.isEmpty()) return global;
        Map<Predicate<Entity>, Pair<Double, Double>> merged = new LinkedHashMap<>(global);
        for (Rule rule : local.values()) {
            Predicate<Entity> matches = rule.matches;
            merged.put(entity -> {
                if (!matches.test(entity)) return false;
                // Global match wins even at zero efficiency, and also through entity tags.
                for (Predicate<Entity> configured : global.keySet()) if (configured.test(entity)) return false;
                return true;
            }, rule.values);
        }
        return Collections.unmodifiableMap(merged);
    }
    private record Rule(Predicate<Entity> matches, Pair<Double, Double> values) {}
}
