package group.taczexpands.client;

import group.taczexpands.client.command.SurfaceCommand;
import group.taczexpands.client.render.surface.SurfaceEngine;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;

/** Surface command and lifecycle registration at the client entry point. */
public final class SurfaceClient {
    private SurfaceClient() {}

    public static void initialize() {
        MinecraftForge.EVENT_BUS.addListener(SurfaceCommand::register);
        MinecraftForge.EVENT_BUS.addListener(SurfaceClient::logout);
    }

    private static void logout(ClientPlayerNetworkEvent.LoggingOut event) {
        SurfaceEngine.leaveWorld();
    }
}
