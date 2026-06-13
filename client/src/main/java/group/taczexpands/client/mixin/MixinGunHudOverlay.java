package group.taczexpands.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.builder.AmmoItemBuilder;
import com.tacz.guns.client.gui.overlay.GunHudOverlay;
import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.index.ClientGunIndex;
import com.tacz.guns.config.client.RenderConfig;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.client.config.ClientConfig;
import group.taczexpands.common.accessor.IAccessorGunData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.gui.overlay.ForgeGui;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = GunHudOverlay.class, remap = false)
public class MixinGunHudOverlay {
    @Unique
    private ResourceLocation taczexpands$lastAmmoId = null;

    @Unique
    private long taczexpands$lastChangeAmmoId = 0;

    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    public void preRender(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height, CallbackInfo ci) {
        var player = Minecraft.getInstance().player;
        if (player == null) return;
        var mainHand = player.getMainHandItem();
        var gunExtra = IAccessorGunData.getExtraHolder(mainHand);
        if (gunExtra == null) return;
        if (!gunExtra.showHud) ci.cancel();
    }

    @Inject(method = "render", at = @At("TAIL"))
    public void hookRender(ForgeGui gui, GuiGraphics graphics, float partialTick, int width, int height, CallbackInfo ci) {
        if ((Boolean) RenderConfig.GUN_HUD_ENABLE.get()) {
            Minecraft mc = Minecraft.getInstance();
            LocalPlayer player = mc.player;
            if (player instanceof IClientPlayerGunOperator) {
                ItemStack stack = player.getMainHandItem();
                if (stack.getItem() instanceof IGun iGun) {
                    ResourceLocation gunId = iGun.getGunId(stack);
                    GunData gunData = TimelessAPI.getClientGunIndex(gunId).map(ClientGunIndex::getGunData).orElse(null);
                    GunDisplayInstance display = TimelessAPI.getGunDisplay(stack).orElse(null);
                    if (gunData != null && display != null) {
                        var ammoId = IAccessorGunData.getCurrentAmmoId(gunData, stack);
                        if (taczexpands$lastAmmoId != ammoId) {
                            taczexpands$lastAmmoId = ammoId;
                            taczexpands$lastChangeAmmoId = System.currentTimeMillis();
                        }
                        var fadeOut = ClientConfig.INSTANCE.getHudAmmoFadeOut().get();
                        if (fadeOut > 0) {
                            var now = System.currentTimeMillis();
                            if (now > taczexpands$lastChangeAmmoId + fadeOut) {
                                var elapsed = now - taczexpands$lastChangeAmmoId - fadeOut;
                                var alpha = 1.0f - elapsed / 1000.0f;
                                if (alpha > 1.0f) alpha = 1.0f;
                                if (alpha < 0.0f) alpha = 0.0f;
                                RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, alpha);

                            }
                        }
                        TimelessAPI.getCommonAmmoIndex(ammoId).ifPresent(ammo -> {
                            graphics.renderItem(AmmoItemBuilder.create().setId(ammoId).build(), width - 117 - 6 - 16, height - 42);
                        });
                        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f);
                    }
                }
            }
        }
    }
}
