package group.taczexpands.client.mixin.accessor.sbm;

import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler", remap = false)
public interface LegacyFirstPersonRenderHandlerInvoker {
    @Invoker("onItemChangedInSameSlot")
    static void taczexpands$refreshLegacy(ItemStack itemStack) {
        throw new AssertionError();
    }
}
