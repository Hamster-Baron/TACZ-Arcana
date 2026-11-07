package group.taczexpands.server.mixin.compat;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.mafuyu404.taczaddon.refit.RefitExternalInstallCommitSequence;
import com.mafuyu404.taczaddon.refit.RefitExternalInstallTransaction;
import com.mafuyu404.taczaddon.storage.api.CraftingItemSource;
import com.tacz.guns.api.item.attachment.AttachmentType;
import group.taczexpands.common.nbt.AttachmentExtras;
import group.taczexpands.server.util.GunManager;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets = "com.mafuyu404.taczaddon.refit.RefitExternalInstallService", remap = false)
public abstract class MixinAddonExternalRefit {
    @Inject(method = "returnReplacedAttachment", at = @At("HEAD"), cancellable = true)
    private static void taczexpands$generatedAttachment(ServerPlayer player, ItemStack replaced, CallbackInfo ci) {
        if (AttachmentExtras.INSTANCE.getIsGenerated(replaced)) ci.cancel();
    }

    @WrapOperation(method = "installFromSource", at = @At(value = "INVOKE",
            target = "Lcom/mafuyu404/taczaddon/refit/RefitExternalInstallTransaction;execute(Lcom/mafuyu404/taczaddon/storage/api/CraftingItemSource;ILnet/minecraft/resources/ResourceLocation;Lcom/tacz/guns/api/item/attachment/AttachmentType;Lcom/mafuyu404/taczaddon/refit/RefitExternalInstallTransaction$Host;)Lcom/mafuyu404/taczaddon/refit/RefitExternalInstallTransaction$Result;"))
    private static RefitExternalInstallTransaction.Result taczexpands$capture(CraftingItemSource source, int slot,
            ResourceLocation id, AttachmentType type, RefitExternalInstallTransaction.Host host,
            Operation<RefitExternalInstallTransaction.Result> original, @Local(argsOnly = true) ItemStack stack,
            @Share("previousGun") LocalRef<ItemStack> previous) {
        previous.set(stack.copy());
        return original.call(source, slot, id, type, host);
    }

    @WrapOperation(method = "installFromSource", at = @At(value = "INVOKE",
            target = "Lcom/mafuyu404/taczaddon/refit/RefitExternalInstallCommitSequence;complete(Lcom/mafuyu404/taczaddon/refit/RefitExternalInstallCommitSequence$Steps;Lcom/mafuyu404/taczaddon/refit/RefitExternalInstallCommitSequence$FailureListener;)Z"))
    private static boolean taczexpands$complete(RefitExternalInstallCommitSequence.Steps steps,
            RefitExternalInstallCommitSequence.FailureListener listener, Operation<Boolean> original,
            @Local(argsOnly = true) ServerPlayer player, @Local(argsOnly = true) ItemStack stack,
            @Local AttachmentType type, @Share("previousGun") LocalRef<ItemStack> previous) {
        return original.call(new RefitExternalInstallCommitSequence.Steps() {
            @Override
            public void finalizeOwnership() {
                steps.finalizeOwnership();
            }

            @Override
            public void postChangeEvent() {
                GunManager.INSTANCE.completeAttachmentChange(previous.get(), stack, player, type);
            }

            @Override
            public void dropAmmo() {
                // completeAttachmentChange has already handled the old ammunition.
            }

            @Override
            public void synchronizeAuthoritativeState() {
                steps.synchronizeAuthoritativeState();
            }

            @Override
            public void refreshScreen() {
                steps.refreshScreen();
            }
        }, listener);
    }
}
