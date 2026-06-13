package group.taczexpands.server.mixin;

import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor;
import com.tacz.guns.entity.shooter.LivingEntityAim;
import group.taczexpands.server.event.LivingEntityZoomSetEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityAim.class, remap = false)
public class MixinLivingEntityAim {
    @Shadow
    @Final
    private LivingEntity shooter;

    @Unique
    private static LivingEntity taczexpands$lastShooter = null;

    @Inject(method = "zoom", at = @At("HEAD"))
    private void onPreZoom(CallbackInfo ci) {
        taczexpands$lastShooter = shooter;
    }

    @Redirect(method = "lambda$zoom$0", at = @At(value = "INVOKE", target = "Lcom/tacz/guns/api/item/nbt/AttachmentItemDataAccessor;setZoomNumberToTag(Lnet/minecraft/nbt/CompoundTag;I)V"))
    private static void redirectSetZoomNumberToTag(CompoundTag nbt, int zoomNumber) {
        if (taczexpands$lastShooter == null) {
            AttachmentItemDataAccessor.setZoomNumberToTag(nbt, zoomNumber);
        } else {
            var shouldCancel = MinecraftForge.EVENT_BUS.post(new LivingEntityZoomSetEvent(taczexpands$lastShooter, zoomNumber));
            if (!shouldCancel) {
                AttachmentItemDataAccessor.setZoomNumberToTag(nbt, zoomNumber);
            }
        }
        taczexpands$lastShooter = null;
    }
}
