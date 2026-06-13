package group.taczexpands.client.mixin;

import group.taczexpands.client.gui.CreativeTabManager;
import net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen;
import net.minecraft.world.item.CreativeModeTab;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(CreativeModeInventoryScreen.class)
public class MixinCreativeModeInventoryScreen {
    @Inject(method = "selectTab", at = @At("TAIL"))
    private void taczexpands$postSelectTab(CreativeModeTab pTab, CallbackInfo ci) {
        CreativeTabManager.INSTANCE.onSelectTab(pTab, (CreativeModeInventoryScreen) (Object) this);
    }
}
