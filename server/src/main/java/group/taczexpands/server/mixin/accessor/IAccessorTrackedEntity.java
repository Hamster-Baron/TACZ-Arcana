package group.taczexpands.server.mixin.accessor;

import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "net.minecraft.server.level.ChunkMap$TrackedEntity")
public interface IAccessorTrackedEntity {
    @Accessor
    @Mutable
    void setRange(int range);

    @Accessor
    ServerEntity getServerEntity();
}
