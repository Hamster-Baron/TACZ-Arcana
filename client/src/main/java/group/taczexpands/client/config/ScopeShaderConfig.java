package group.taczexpands.client.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class ScopeShaderConfig {
    private static ForgeConfigSpec.BooleanValue flickerFix;
    private static ForgeConfigSpec.BooleanValue preload;
    public final boolean enabled;
    public final boolean prewarm;

    private ScopeShaderConfig(boolean enabled, boolean prewarm) {
        this.enabled = enabled;
        this.prewarm = prewarm;
    }

    public static ForgeConfigSpec.Builder register(ForgeConfigSpec.Builder builder) {
        if (flickerFix != null) {
            throw new IllegalStateException("Scope settings registered twice");
        }
        builder.push("Scope Shader Settings");
        flickerFix = builder.comment(
                "Keep separate shader histories for the main view and magnified scopes to reduce flickering.",
                "Uses additional VRAM. Disable this if you encounter visual glitches or freezes.",
                "Requires a full game restart.")
                .worldRestart().define("enableScopeShaderFlickerFix", true);
        preload = builder.comment(
                "Preload scope shaders when entering a world or dimension to reduce stuttering when first aiming.",
                "May increase loading time and uses VRAM earlier. Disable this to defer loading until first use.",
                "Requires a full game restart.")
                .worldRestart().define("preloadScopeShaderResources", true);
        builder.pop();
        return builder;
    }

    public static ScopeShaderConfig current() {
        if (flickerFix == null || preload == null) {
            throw new IllegalStateException("Arcana CLIENT config has not registered scope settings");
        }
        return new ScopeShaderConfig(flickerFix.get(), preload.get());
    }
}
