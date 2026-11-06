package group.taczexpands.client.render.paint;

import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.GunDisplayInstance;
import group.taczexpands.client.paint.PaintConfig;
import group.taczexpands.client.paint.PaintParameters;
import group.taczexpands.common.nbt.GunExtras;
import group.taczexpands.common.paint.PaintConstants;
import group.taczexpands.common.paint.PaintState;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Selects the static override or MODULE material for each rendered surface. */
public final class PaintResolver {
    private PaintResolver() {}
    public static ResourceLocation resolve(ItemStack stack, GunDisplayInstance display,
                                            ResourceLocation original, boolean useGunLod) {
        return resolve(stack, display, original, useGunLod, true);
    }
    public static ResourceLocation resolve(ItemStack stack, GunDisplayInstance display,
                                            ResourceLocation original, boolean useGunLod, boolean legacyAllowed) {
        if (display == null || original == null) return original;
        var gun = PaintResources.gun(display.getDisplayId());
        ResourceLocation override = staticOverride(stack);
        boolean independent = independentLod(display, gun, original, useGunLod);
        // An active legacy override suppresses MODULE on a separate LOD UV surface.
        if (override != null) return independent || !legacyAllowed ? original : override;
        var recipe = PaintQueries.preparedRecipe(stack);
        if (gun == null || recipe == null) return original;
        var surface = independent ? gun.far() : gun.near();
        if (surface == null || !surface.original().equals(original)) return original;
        return painted(stack, "gun", gun.displayId(), independent ? gun.lodModel() : gun.model(),
                gun.config(), surface, recipe, original);
    }

    /** The selected base/variant/adapter index owns its UV, original texture and mask. */
    public static ResourceLocation resolveAttachment(ItemStack gun, Object index,
                                                      ResourceLocation original, boolean useLod) {
        if (original == null) return null;
        var attachment = PaintResources.attachment(index);
        var recipe = PaintQueries.preparedRecipe(gun);
        if (attachment == null || recipe == null) return original;
        var surface = useLod ? attachment.far() : attachment.near();
        if (surface == null || !surface.original().equals(original)) return original;
        boolean independent = useLod && attachment.far() != attachment.near();
        return painted(gun, attachment.kind(), attachment.displayId(),
                independent ? attachment.lodModel() : attachment.model(), attachment.config(),
                surface, recipe, original);
    }

    private static ResourceLocation painted(ItemStack stack, String kind, ResourceLocation display,
                                             ResourceLocation model, PaintConfig.Surface config,
                                             PaintResources.Surface surface, PaintResources.Recipe recipe,
                                             ResourceLocation original) {
        if (!PaintResources.ready(surface, recipe)) return original;
        IGun item = IGun.getIGunOrNull(stack);
        var resolved = PaintParameters.resolve(recipe.config(), PaintState.read(
                item.getAttachmentTag(stack, AttachmentType.valueOf(PaintConstants.MODULE))).values());
        PaintParameters.Effective[] effective = new PaintParameters.Effective[3];
        boolean painted = surface.base(recipe.config().base()) != null
                || (recipe.config().base() != null && recipe.config().switchPbr() && surface.pbr() != null);
        for (int region = 0; region < 3; region++) {
            var value = resolved.channel(region);
            if (surface.hasRegion(region) && value != null
                    && (value.mix() == 0 || recipe.patterns()[region] != null)) {
                effective[region] = value;
            }
            painted |= effective[region] != null;
        }
        if (!painted) return original;
        resolved = new PaintParameters.Resolved(effective[0], effective[1], effective[2]);
        PaintInput input = new PaintInput(config, surface, recipe, resolved);
        return PaintPreviewCache.matches(stack)
                ? PaintPreviewCache.resolve(kind, display, model, original, surface.mask(), input)
                : PaintTextureCache.resolve(kind, display, model, original, surface.mask(), recipe.displayId(), input);
    }

    private static boolean independentLod(GunDisplayInstance display, PaintResources.Gun gun,
                                           ResourceLocation original, boolean useGunLod) {
        if (!useGunLod) return false;
        if (!Objects.equals(original, display.getModelTexture())) return true;
        if (gun != null) return gun.far() != gun.near();
        var raw = ClientAssetsManager.INSTANCE.getGunDisplay(display.getDisplayId());
        return raw != null && raw.getGunLod() != null && raw.getGunLod().getModelLocation() != null
                && !Objects.equals(raw.getModelLocation(), raw.getGunLod().getModelLocation());
    }

    private static final Map<String, Optional<ResourceLocation>> overrides = new HashMap<>();

    private static ResourceLocation staticOverride(ItemStack stack) {
        String value = GunExtras.INSTANCE.getOverrideTexture(stack);
        if (value == null) return null;
        return overrides.computeIfAbsent(value, key -> {
            ResourceLocation location = ResourceLocation.tryParse(key);
            return location != null && Minecraft.getInstance().getResourceManager().getResource(location).isPresent()
                    ? Optional.of(location) : Optional.empty();
        }).orElse(null);
    }

    static void clear() {
        overrides.clear();
    }
}