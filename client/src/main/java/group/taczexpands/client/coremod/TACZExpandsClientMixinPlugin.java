package group.taczexpands.client.coremod;

import com.mojang.logging.LogUtils;
import group.taczexpands.client.compat.RendererCapabilities;

import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;
import org.spongepowered.asm.service.MixinService;

import java.io.IOException;
import java.util.List;
import java.util.Set;

public final class TACZExpandsClientMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String SBM_TARGET_CLASS =
            "com.github.mcmodderanchor.simplebedrockmodel.v1.client.handler.FirstPersonRenderHandler";
    private static final String SBM_LEGACY_MIXIN =
            "group.taczexpands.client.mixin.accessor.sbm.LegacyFirstPersonRenderHandlerInvoker";
    private static final String SBM_MODERN_MIXIN =
            "group.taczexpands.client.mixin.accessor.sbm.ModernFirstPersonRenderHandlerInvoker";
    private static volatile SbmLayout sbmLayout = SbmLayout.NOT_DETECTED;

    public static SbmLayout getSbmLayout() {
        return TACZExpandsClientMixinPlugin.detectSbmLayout();
    }

    private static synchronized SbmLayout detectSbmLayout() {
        if (sbmLayout != SbmLayout.NOT_DETECTED) {
            return sbmLayout;
        }
        try {
            ClassNode classNode =
                    MixinService.getService().getBytecodeProvider().getClassNode(SBM_TARGET_CLASS);
            boolean hasLegacyMethod =
                    classNode.methods.stream()
                            .anyMatch(
                                    method ->
                                            method.name.equals("onItemChangedInSameSlot")
                                                    && method.desc.equals(
                                                            "(Lnet/minecraft/world/item/ItemStack;)V"));
            boolean hasModernResetMethod =
                    classNode.methods.stream()
                            .anyMatch(
                                    method ->
                                            method.name.equals("reset")
                                                    && method.desc.equals("()V"));
            boolean hasModernUpdateMethod =
                    classNode.methods.stream()
                            .anyMatch(
                                    method ->
                                            method.name.equals("onClientTick")
                                                    && method.desc.equals(
                                                            "(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V"));
            sbmLayout =
                    hasLegacyMethod
                            ? SbmLayout.LEGACY
                            : (hasModernResetMethod && hasModernUpdateMethod
                                    ? SbmLayout.MODERN
                                    : SbmLayout.UNSUPPORTED);
        } catch (IOException | ClassNotFoundException exception) {
            sbmLayout = SbmLayout.UNAVAILABLE;
            LOGGER.error(
                    "Unable to inspect SimpleBedrockModel first-person renderer",
                    (Throwable) exception);
        } catch (LinkageError | RuntimeException exception) {
            sbmLayout = SbmLayout.UNAVAILABLE;
            LOGGER.error("Failed to inspect SimpleBedrockModel first-person renderer", exception);
        }
        if (sbmLayout == SbmLayout.LEGACY || sbmLayout == SbmLayout.MODERN) {
            LOGGER.info("Detected SimpleBedrockModel first-person layout: {}", (Object) sbmLayout);
        } else if (sbmLayout == SbmLayout.UNSUPPORTED) {
            LOGGER.error("Unsupported SimpleBedrockModel first-person renderer layout");
        }
        return sbmLayout;
    }

    public void onLoad(String mixinPackage) {}

    public String getRefMapperConfig() {
        return null;
    }

    private boolean supportsSbmMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.equals(SBM_LEGACY_MIXIN)) {
            return TACZExpandsClientMixinPlugin.detectSbmLayout() == SbmLayout.LEGACY;
        }
        if (mixinClassName.equals(SBM_MODERN_MIXIN)) {
            return TACZExpandsClientMixinPlugin.detectSbmLayout() == SbmLayout.MODERN;
        }
        return true;
    }

    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    public List<String> getMixins() {
        return null;
    }

    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.startsWith("group.taczexpands.client.mixin.compat.iris.")) {
            var capabilities = RendererCapabilities.current();
            if (mixinClassName.endsWith("MixinSurfaceTerrainBindings")
                    || mixinClassName.endsWith("MixinSurfaceTerrainRenderer"))
                return capabilities.terrainBindings();
            return capabilities.irisViews();
        }
        return supportsSbmMixin(targetClassName, mixinClassName);
    }

    public static enum SbmLayout {
        NOT_DETECTED,
        LEGACY,
        MODERN,
        UNSUPPORTED,
        UNAVAILABLE;
    }
}
