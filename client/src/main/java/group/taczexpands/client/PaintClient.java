package group.taczexpands.client;

import group.taczexpands.client.render.paint.PaintBudget;
import group.taczexpands.client.render.paint.PaintResources;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;

/** Paint event wiring is owned by the client composition root. */
public final class PaintClient {
    private PaintClient() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(PaintClient::endFrame);
        MinecraftForge.EVENT_BUS.addListener(PaintClient::logout);
        MinecraftForge.EVENT_BUS.addListener(PaintClient::login);
    }

    private static void endFrame(TickEvent.RenderTickEvent event) {
        if (event.phase == TickEvent.Phase.END) PaintBudget.endFrame();
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        PaintResources.clear();
    }

    private static void login(ClientPlayerNetworkEvent.LoggingIn event) {
        if (event.getConnection() != null && event.getConnection().isMemoryConnection()) {
            PaintResources.reload();
        }
    }
}
