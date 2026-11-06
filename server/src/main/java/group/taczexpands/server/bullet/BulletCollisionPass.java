package group.taczexpands.server.bullet;

import com.tacz.guns.entity.EntityKineticBullet;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.util.EntityUtil;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.server.accessor.IAccessorServerBullet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/** Processes a collision segment synchronously in hit order. */
public final class BulletCollisionPass {
    private static final EntityKineticBullet.EntityResult[] EMPTY = new EntityKineticBullet.EntityResult[0];
    private final EntityKineticBullet bullet;
    private final Vec3 start;
    private final Vec3 end;
    private final BooleanSupplier singleHit;
    private final BiPredicate<EntityKineticBullet.EntityResult, Vec3> hit;
    private EntityKineticBullet.EntityResult[] directHits;
    private int next;
    private boolean selected;
    private List<Entity> processed;
    private Vec3 lastHit;
    private int hitCount;
    private boolean stopped;

    public BulletCollisionPass(EntityKineticBullet bullet, Vec3 start, Vec3 end,
                               BooleanSupplier singleHit,
                               BiPredicate<EntityKineticBullet.EntityResult, Vec3> hit) {
        this.bullet = bullet;
        this.start = start;
        this.end = end;
        this.singleHit = singleHit;
        this.hit = hit;
    }

    /** Keep TacZ's public query entry points, including other mods' injections. */
    private EntityKineticBullet.EntityResult[] query(Vec3 limit) {
        if (singleHit.getAsBoolean()) {
            EntityKineticBullet.EntityResult result = EntityUtil.findEntityOnPath(bullet, start, limit);
            return result == null ? EMPTY : new EntityKineticBullet.EntityResult[] {result};
        }
        return EntityUtil.findEntitiesOnPath(bullet, start, limit).toArray(EMPTY);
    }

    private boolean hasProximityDetection() {
        BulletData data = ((IAccessorServerBullet) bullet).taczexpands$getBulletExtraData().getBulletData();
        // EntityUtil applies arming and speed gates. A proximity
        // result reports the target's center, so it must be queried on the clipped
        // segment rather than rejected because that center is beyond the wall.
        return data != null && data.getExplosionData() != null && data.getExplosionData().isExplode()
            && IAccessorBulletData.getBulletExtraHolder(data).proximityFuzeRange > 0;
    }

    private void selectNext(EntityKineticBullet.EntityResult[] results, int index) {
        int nearest = index;
        for (int i = index + 1; i < results.length; i++) {
            if (results[i].getHitPos().distanceToSqr(start)
                    < results[nearest].getHitPos().distanceToSqr(start)) nearest = i;
        }
        EntityKineticBullet.EntityResult swap = results[index];
        results[index] = results[nearest];
        results[nearest] = swap;
    }

    private boolean submit(EntityKineticBullet.EntityResult result, Vec3 limit) {
        lastHit = result.getHitPos();
        hitCount++;
        stopped = hit.test(result, limit);
        if (!stopped) {
            if (processed == null) processed = new ArrayList<>(2);
            processed.add(result.getEntity());
        }
        return stopped;
    }

    public boolean consumeTo(Vec3 limit) {
        if (hasProximityDetection()) {
            // Query each clipped segment so proximity hits respect penetrable block boundaries.
            EntityKineticBullet.EntityResult[] results = query(limit);
            for (int i = 0; i < results.length; i++) {
                selectNext(results, i);
                if (processed != null && processed.contains(results[i].getEntity())) continue;
                if (submit(results[i], limit)) return true;
            }
            return false;
        }
        if (directHits == null) directHits = query(end);
        double distance = start.distanceToSqr(limit);
        while (next < directHits.length) {
            if (!selected) {
                selectNext(directHits, next);
                selected = true;
            }
            EntityKineticBullet.EntityResult result = directHits[next];
            if (start.distanceToSqr(result.getHitPos()) > distance) break;
            next++;
            selected = false;
            if (processed != null && processed.contains(result.getEntity())) continue;
            if (submit(result, limit)) return true;
        }
        return false;
    }

    public int hitCount() { return hitCount; }
    public boolean stopped() { return stopped; }
    public Vec3 lastHit() { return lastHit; }
}
