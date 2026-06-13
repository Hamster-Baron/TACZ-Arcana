package group.taczexpands.client.mixin;

import com.tacz.guns.api.client.gameplay.IClientPlayerGunOperator;
import com.tacz.guns.api.item.IAnimationItem;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.client.event.InventoryEvent;
import group.taczexpands.client.gui.GunContextManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = InventoryEvent.class, remap = false)
public class MixinInventoryEvent {
    @Shadow
    private static int oldHotbarSelected;

    @Shadow
    private static ItemStack oldHotbarSelectItem;

    @Redirect(method = "isSame", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/IGun;getGunId(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/resources/ResourceLocation;"))
    private static ResourceLocation hookGetGunId(IGun instance, ItemStack itemStack) {
        return GunExtras.INSTANCE.getGunId(itemStack);
    }

    @Redirect(method = "onPlayerChangeSelect", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/gameplay/IClientPlayerGunOperator;draw(Lnet/minecraft/world/item/ItemStack;)V"))
    private static void redirectChangeDraw(IClientPlayerGunOperator instance, ItemStack itemStack) {
        instance.draw(itemStack);

        if (oldHotbarSelected == -1) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;
        ItemStack currentItem = player.getMainHandItem();
        Item item = currentItem.getItem();
        if (item instanceof IAnimationItem animationItem) {
            if (!animationItem.isSame(currentItem, itemStack)) {
                GunContextManager.INSTANCE.onChangeGun();
            }
        }
    }

    @Redirect(method = "onPlayerSwapMainHand", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/client/gameplay/IClientPlayerGunOperator;draw(Lnet/minecraft/world/item/ItemStack;)V"))
    private static void redirectSwapDraw(IClientPlayerGunOperator instance, ItemStack itemStack) {
        instance.draw(itemStack);

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) return;

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getMainHandItem();
        Item mainHandItem = mainHand.getItem();
        Item offHandItem = offHand.getItem();
        IAnimationItem item = null;
        if (mainHandItem instanceof IAnimationItem animationItem) {
            item = animationItem;
        } else if (offHandItem instanceof IAnimationItem animationItem) {
            item = animationItem;
        }

        if (item != null) {
            if (!item.isSame(mainHand, offHand)) {
                GunContextManager.INSTANCE.onChangeGun();
            }
        }
    }
}
