package group.taczexpands.client.mixin;

import com.tacz.guns.GunMod;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.client.gui.components.refit.GunAttachmentSlot;
import group.taczexpands.common.TACZExpandsCommon;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = GunAttachmentSlot.class, remap = false)
public class MixinGunAttachmentSlot {
    @Unique
    private static final ResourceLocation taczexpands$module_icon = new ResourceLocation(TACZExpandsCommon.MODID, "textures/gui/module.png");

    @Redirect(method = "renderWidget", remap = true, at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/GuiGraphics;blit(Lnet/minecraft/resources/ResourceLocation;IIIIFFIIII)V", remap = true))
    public void taczexpands$renderWidget$redirectBlit(GuiGraphics instance, ResourceLocation pAtlasLocation, int pX, int pY, int pWidth, int pHeight, float pUOffset, float pVOffset, int pUWidth, int pVHeight, int pTextureWidth, int pTextureHeight) {
        if (GunRefitScreen.ICONS_TEXTURE.equals(pAtlasLocation) && pUOffset < 0.0f) {
            instance.blit(taczexpands$module_icon, pX, pY, pWidth, pHeight, 0.0f, pVOffset, pUWidth, pVHeight, 32, 32);
            return;
        }
        instance.blit(pAtlasLocation, pX, pY, pWidth, pHeight, pUOffset, pVOffset, pUWidth, pVHeight, pTextureWidth, pTextureHeight);
    }
}
