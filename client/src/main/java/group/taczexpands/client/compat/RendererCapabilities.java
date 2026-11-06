package group.taczexpands.client.compat;

import com.mojang.logging.LogUtils;
import org.spongepowered.asm.service.MixinService;

/** Adapter availability, independent of mod presence and the current shader toggle. */
public final class RendererCapabilities {
    private static Result detected;
    public record Result(String viewFailure, String terrainFailure, String paintPbrFailure, String surfacePbrFailure) {
        public boolean irisViews() { return viewFailure == null; }
        public boolean terrainBindings() { return irisViews() && terrainFailure == null; }
        public String cameraFailure() { return viewFailure != null ? viewFailure : terrainFailure; }
        public boolean paintPbr() { return paintPbrFailure == null; }
        public boolean surfacePbr() { return paintPbr() && surfacePbrFailure == null; }
    }
    private RendererCapabilities() {}

    public static synchronized Result current() {
        if (detected != null) return detected;
        if (!RendererPresence.OCULUS_PRESENT)
            return detected = new Result("Oculus is not installed", null, "Oculus is not installed", "Oculus is not installed");
        var contract = new RendererContract(name -> MixinService.getService().getBytecodeProvider().getClassNode(name));
        detected = RendererPresence.EMBEDDIUM_PRESENT
                ? new Result(contract.views(), contract.terrain(), contract.paintPbr(), contract.surfacePbr())
                : new Result("No supported terrain adapter is installed", null, contract.paintPbr(), contract.surfacePbr());
        if (detected.cameraFailure() != null || detected.paintPbrFailure() != null || detected.surfacePbrFailure() != null)
            LogUtils.getLogger().warn("Arcana renderer adapter unavailable: world camera/thermal scope={}, paint PBR={}, surface PBR={}. Thermal scopes retain the shader pipeline; HUD/model surfaces and paint colors remain enabled.",
                    detected.cameraFailure(), detected.paintPbrFailure(), detected.surfacePbrFailure());
        return detected;
    }
}
