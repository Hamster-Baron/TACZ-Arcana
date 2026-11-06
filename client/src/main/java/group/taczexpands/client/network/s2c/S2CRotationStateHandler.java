package group.taczexpands.client.network.s2c;

import group.taczexpands.client.input.rotation.RotationController;
import group.taczexpands.common.network.s2c.S2CRotationState;

/** Handles rotation state on the client main thread. */
public final class S2CRotationStateHandler {
    private S2CRotationStateHandler() {}

    public static void handle(S2CRotationState packet) {
        RotationController.accept(packet);
    }
}
