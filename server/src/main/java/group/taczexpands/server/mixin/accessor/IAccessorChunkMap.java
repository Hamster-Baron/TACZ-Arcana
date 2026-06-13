package group.taczexpands.server.mixin.accessor;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.server.level.ChunkMap;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = ChunkMap.class)
public interface IAccessorChunkMap {
    @Accessor
    Int2ObjectMap<Object> getEntityMap();
}
