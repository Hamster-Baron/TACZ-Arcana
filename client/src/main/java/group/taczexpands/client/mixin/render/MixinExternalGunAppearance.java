package group.taczexpands.client.mixin.render;

import com.tacz.guns.client.renderer.external.ExternalGunAppearance;
import group.taczexpands.client.render.gun.AppearanceKeyExtension;
import group.taczexpands.client.render.paint.PaintResolver;
import group.taczexpands.client.render.paint.PaintAttachmentMaterials;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = ExternalGunAppearance.class, remap = false)
public abstract class MixinExternalGunAppearance {
    @Inject(method = "resolve(Lnet/minecraft/world/item/ItemStack;Z)Lcom/tacz/guns/client/renderer/external/ExternalGunAppearance$Resolution;",
            at = @At("RETURN"), cancellable = true)
    private static void taczexpands$textureSnapshot(ItemStack stack, boolean copyStack,
                                                   CallbackInfoReturnable<ExternalGunAppearance.Resolution> cir) {
        var resolution = cir.getReturnValue();
        if (resolution.status() != ExternalGunAppearance.Status.READY) return;
        var variants = resolution.variants();
        var near = taczexpands$paint(stack, variants.near(), true);
        var far = taczexpands$paint(stack, variants.far(), false);
        if (variants.near() == variants.far() && near.key().equals(far.key())) far = near;
        if (near != variants.near() || far != variants.far())
            cir.setReturnValue(new ExternalGunAppearance.Resolution(ExternalGunAppearance.Status.READY,
                    new ExternalGunAppearance.Variants(near, far)));
    }
    @Unique private static ExternalGunAppearance.Snapshot taczexpands$paint(ItemStack stack,
                                                                           ExternalGunAppearance.Snapshot source, boolean legacyAllowed) {
        // An attachment LOD can produce a far key; select the actual gun body for its texture.
        var lod = source.display().getExternalLodModel();
        boolean gunLod = lod != null && lod.getLeft() == source.model();
        ResourceLocation texture = PaintResolver.resolve(stack, source.display(), source.texture(), gunLod, legacyAllowed);
        if (texture == null) return source;
        ResourceLocation materialKey = PaintAttachmentMaterials.textureKey(stack, source, texture);
        if (texture.equals(source.texture()) && materialKey.equals(source.texture())) return source;
        var old = source.key();
        var key = new ExternalGunAppearance.GeometryKey(old.resourceRevision(), old.displayId(), old.attachments(),
                old.bullet(), old.ammo(), old.useLod());
        ((AppearanceKeyExtension) (Object) key).taczexpands$initTextureKey(materialKey);
        return new ExternalGunAppearance.Snapshot(key, source.stack(), source.display(), source.model(), texture, source.useLod());
    }
}
