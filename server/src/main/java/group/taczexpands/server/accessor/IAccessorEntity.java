package group.taczexpands.server.accessor;

import net.minecraft.world.entity.Entity;

import java.util.Map;

public interface IAccessorEntity {
    void taczexpands$setIgniteExtraDamage(float damage);

    Map<String, Integer> taczexpands$getHurtCooldownGroups();

    void taczexpands$setOwner(Entity owner);

    Entity taczexpands$getOwner();
}
