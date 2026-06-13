package group.taczexpands.client.mixin.accessor;

import com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = FirstPersonRenderHandler.class, remap = false)
public interface IAccessorFirstPersonRenderHandler {
    @Invoker("onItemChangedInSameSlot")
    static void taczexpands$onItemChangedInSameSlot(ItemStack itemStack) {
    }
}
