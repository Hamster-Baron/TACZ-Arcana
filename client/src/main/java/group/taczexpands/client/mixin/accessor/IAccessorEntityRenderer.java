package group.taczexpands.client.mixin.accessor;

import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(EntityRenderer.class)
public interface IAccessorEntityRenderer {
    @Invoker("getBlockLightLevel")
    int getBlockLightLevel(Entity pEntity, BlockPos pPos);
}
