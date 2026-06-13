package group.taczexpands.client.mixin;

import com.tacz.guns.client.gui.overlay.HeatBarOverlay;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HeatBarOverlay.class, remap = false)
public class MixinHeatBarOverlay {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void preRender(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var mainHand = player.getMainHandItem();
        var gunExtra = IAccessorGunData.getExtraHolder(mainHand);
        if (gunExtra == null) return;
        if (gunExtra.showOverheatBar == null) {
            if (!ClientConfig.INSTANCE.getShowOverheatBarByDefault().get()) ci.cancel();
            return;
        }
        if (!gunExtra.showOverheatBar) ci.cancel();
    }
}
