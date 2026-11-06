package group.taczexpands.client.command;

import static net.minecraft.commands.Commands.literal;
import group.taczexpands.client.render.surface.SurfaceEngine;
import group.taczexpands.client.render.surface.SurfaceFeed;
import group.taczexpands.client.render.surface.SurfaceResources;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import java.util.Locale;

/** Provides client diagnostics and delegates explicit reload to SurfaceResources. */
public final class SurfaceCommand {
    private SurfaceCommand() {}

    public static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(literal("arcana_surfaces")
                .then(literal("reload").executes(context -> {
                    SurfaceResources.reload();
                    context.getSource().sendSuccess(() -> Component.literal("Arcana Surfaces reloaded"), false);
                    return 1;
                }))
                .then(literal("stats").executes(context -> {
                    for (var feed : SurfaceEngine.feeds())
                        context.getSource().sendSuccess(() -> statistics(feed), false);
                    return 1;
                })));
    }

    private static Component statistics(SurfaceFeed feed) {
        double averageMs = feed.updates() == 0 ? 0 : feed.renderNanos() / 1e6 / feed.updates();
        String summary = String.format(Locale.ROOT, "%s: %d updates, CPU submission avg %.2f ms",
                feed.id(), feed.updates(), averageMs);
        return Component.literal(feed.cameraFailure() == null ? summary : summary + "; camera unavailable: " + feed.cameraFailure());
    }
}
