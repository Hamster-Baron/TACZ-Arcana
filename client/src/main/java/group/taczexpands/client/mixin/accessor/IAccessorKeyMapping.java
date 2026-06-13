package group.taczexpands.client.mixin.accessor;

import net.minecraft.client.KeyMapping;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(KeyMapping.class)
public interface IAccessorKeyMapping {
    @Invoker("release")
    void release();
}
