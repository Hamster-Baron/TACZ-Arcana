package group.taczexpands.client.mixin;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.gui.GunRefitScreen;
import com.tacz.guns.network.message.ClientMessageRefitGun;
import com.tacz.guns.network.message.ClientMessageUnloadAttachment;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import group.taczexpands.client.accessor.IAccessorPaintRefitScreen;
import group.taczexpands.client.gui.paint.PaintEditorController;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = GunRefitScreen.class, remap = false)
public abstract class MixinGunRefitScreen extends Screen implements IAccessorPaintRefitScreen {
    @Unique private final PaintEditorController taczexpands$paint = new PaintEditorController(this, this);

    protected MixinGunRefitScreen(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"), remap = true)
    private void taczexpands$paintInit(CallbackInfo ci) {
        taczexpands$paint.initialize();
    }

    @Inject(method = "render", at = @At("HEAD"), remap = true)
    private void taczexpands$paintValidate(CallbackInfo ci) {
        taczexpands$paint.validate();
    }

    @Override
    public void setFocused(GuiEventListener listener) {
        if (getFocused() != listener) super.setFocused(listener);
    }

    @Override
    public boolean mouseReleased(double x, double y, int button) {
        if (button == 0 && taczexpands$paint.hasPointerCapture()) {
            setDragging(false);
            return taczexpands$paint.mouseReleased(x, y, button);
        }
        return super.mouseReleased(x, y, button);
    }

    @Override
    public void removed() {
        taczexpands$paint.close();
        super.removed();
    }

    @Override
    public void taczexpands$invalidatePaint() {
        taczexpands$paint.invalidate();
    }

    @Override
    public void taczexpands$addPaintWidget(AbstractWidget widget) {
        addRenderableWidget(widget);
    }

    @Override
    public void taczexpands$removePaintWidget(AbstractWidget widget) {
        removeWidget(widget);
    }

    @Override
    public void taczexpands$rebuildPaintWidgets() {
        rebuildWidgets();
    }

    @WrapOperation(method = "*", at = @At(value = "NEW",
            target = "(IILcom/tacz/guns/api/item/attachment/AttachmentType;)Lcom/tacz/guns/network/message/ClientMessageRefitGun;"))
    private static ClientMessageRefitGun taczexpands$refit(int attachmentSlot, int gunSlot,
            AttachmentType type, Operation<ClientMessageRefitGun> original) {
        PaintEditorController.beforeRefit(gunSlot, type);
        return original.call(attachmentSlot, gunSlot, type);
    }

    @WrapOperation(method = "*", at = @At(value = "NEW",
            target = "(ILcom/tacz/guns/api/item/attachment/AttachmentType;)Lcom/tacz/guns/network/message/ClientMessageUnloadAttachment;"))
    private static ClientMessageUnloadAttachment taczexpands$unload(int gunSlot, AttachmentType type,
            Operation<ClientMessageUnloadAttachment> original) {
        PaintEditorController.beforeRefit(gunSlot, type);
        return original.call(gunSlot, type);
    }

    @Shadow
    @Final
    public static int ICON_UV_SIZE;


    @Inject(method = "getSlotTextureXOffset", at = @At("HEAD"), cancellable = true)
    private static void getSlotTextureXOffset(ItemStack gunItem, AttachmentType attachmentType, CallbackInfoReturnable<Integer> cir) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return;
        } else if (!iGun.allowAttachmentType(gunItem, attachmentType)) {
            return;
        }

        cir.setReturnValue(innerGetSlotTextureXOffset(gunItem, attachmentType));
    }

    @Unique
    private static int innerGetSlotTextureXOffset(ItemStack gunItem, AttachmentType attachmentType) {
        IGun iGun = IGun.getIGunOrNull(gunItem);
        if (iGun == null) {
            return -1;
        }
        if (!iGun.allowAttachmentType(gunItem, attachmentType)) {
            return ICON_UV_SIZE * 6;
        }
        switch (attachmentType) {
            case GRIP -> {
                return 0;
            }
            case LASER -> {
                return ICON_UV_SIZE;
            }
            case MUZZLE -> {
                return ICON_UV_SIZE * 2;
            }
            case SCOPE -> {
                return ICON_UV_SIZE * 3;
            }
            case STOCK -> {
                return ICON_UV_SIZE * 4;
            }
            case EXTENDED_MAG -> {
                return ICON_UV_SIZE * 5;
            }
        }
        return -1;
    }
}
