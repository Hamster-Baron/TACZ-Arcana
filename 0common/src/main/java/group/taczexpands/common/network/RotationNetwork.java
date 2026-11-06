package group.taczexpands.common.network;

import group.taczexpands.common.network.s2c.S2CRotationState;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Registers the rotation protocol and transports state between the server and client. */
public final class RotationNetwork {
    private static final String PROTOCOL_VERSION = "1";
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
        new ResourceLocation("taczexpands", "rotation_range"),
        () -> PROTOCOL_VERSION,
        RotationNetwork::acceptsVersion,
        RotationNetwork::acceptsVersion
    );
    private static boolean registered;
    private static Consumer<S2CRotationState> clientHandler = packet -> {};

    private RotationNetwork() {}

    public static void registerClientHandler(Consumer<S2CRotationState> handler) {
        clientHandler = handler;
    }

    private static boolean acceptsVersion(String version) {
        return PROTOCOL_VERSION.equals(version) || NetworkRegistry.ABSENT.equals(version);
    }

    /** Registers the rotation state message once from the common mod entry point. */
    public static synchronized void register() {
        if (registered) return;
        CHANNEL.messageBuilder(S2CRotationState.class, 0, NetworkDirection.PLAY_TO_CLIENT)
            .encoder(S2CRotationState::encode)
            .decoder(S2CRotationState::decode)
            .consumerMainThread((packet, context) -> clientHandler.accept(packet))
            .add();
        registered = true;
    }

    public static boolean supported(ServerPlayer player) {
        return CHANNEL.isRemotePresent(player.connection.connection);
    }

    public static void send(ServerPlayer player, S2CRotationState packet) {
        if (supported(player)) CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
