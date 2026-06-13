package group.taczexpands.server.mixin.accessor;

import net.minecraft.server.level.ServerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ServerEntity.class)
public interface IAccessorServerEntity {
    @Accessor
    @Mutable
    void setUpdateInterval(int updateInterval);
}
