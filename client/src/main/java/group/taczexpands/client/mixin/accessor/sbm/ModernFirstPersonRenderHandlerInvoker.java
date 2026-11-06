package group.taczexpands.client.mixin.accessor.sbm;

import net.minecraftforge.event.TickEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.gen.Invoker;

@Pseudo
@Mixin(targets = "com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler", remap = false)
public interface ModernFirstPersonRenderHandlerInvoker {
    @Invoker("reset")
    static void taczexpands$resetHandState() {
        throw new AssertionError();
    }

    @Invoker("onClientTick")
    static void taczexpands$runHandUpdate(TickEvent.ClientTickEvent event) {
        throw new AssertionError();
    }
}
