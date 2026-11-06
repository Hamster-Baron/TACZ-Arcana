package group.taczexpands.server.event;

import com.tacz.guns.api.event.common.EntityHurtByGunEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.fml.LogicalSide;

/** Mutable damage carried by Arcana's melee skill context. */
public final class MeleeDamageEvent extends EntityHurtByGunEvent.Pre {
    public MeleeDamageEvent(ServerPlayer player, LivingEntity target, float damage) {
        super(null, target, player, null, null, damage, null, false, 1.0f, LogicalSide.SERVER);
    }

    @Override
    public void postEventToKubeJS(EntityHurtByGunEvent event) {
        // Arcana consumes this context locally; TacZ publishes events for actual gunshots.
    }
}
