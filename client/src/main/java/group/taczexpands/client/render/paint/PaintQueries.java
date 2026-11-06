package group.taczexpands.client.render.paint;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.api.item.attachment.AttachmentType;
import group.taczexpands.client.paint.PaintConfig;
import group.taczexpands.common.paint.PaintConstants;
import java.util.Locale;
import net.minecraft.world.item.ItemStack;

/** Recipe and editable-region metadata for the paint editor. */
public final class PaintQueries {
    private PaintQueries() {}
    static PaintResources.Recipe preparedRecipe(ItemStack stack) {
        IGun gun = IGun.getIGunOrNull(stack);
        if (gun == null || gun.getAttachment(stack, AttachmentType.valueOf(PaintConstants.MODULE)).isEmpty()) return null;
        return PaintResources.recipe(gun.getAttachmentId(stack, AttachmentType.valueOf(PaintConstants.MODULE)));
    }

    public static PaintConfig.Recipe recipe(ItemStack stack) {
        var recipe = preparedRecipe(stack);
        return recipe == null ? null : recipe.config();
    }

    public static boolean[] editableRegions(ItemStack stack) {
        var display = TimelessAPI.getGunDisplay(stack).orElse(null);
        var gun = display == null ? null : PaintResources.gun(display.getDisplayId());
        var recipe = preparedRecipe(stack);
        boolean[] result = new boolean[3];
        if (recipe != null) {
            if (gun != null) addRegions(result, gun.near(), recipe);
            var model = display == null ? null : display.getGunModel();
            if (model != null) {
                var selected = PaintAttachmentSelection.installedNear(stack, type ->
                        model.getNode(type.name().toLowerCase(Locale.ROOT) + "_pos") != null);
                for (var layer : selected) {
                    var attachment = PaintResources.attachment(layer.index());
                    if (attachment != null && attachment.near() != null
                            && attachment.near().original().equals(layer.original())) {
                        addRegions(result, attachment.near(), recipe);
                    }
                }
            }
        }
        return result;
    }

    private static void addRegions(boolean[] result, PaintResources.Surface surface,
                                    PaintResources.Recipe recipe) {
        if (surface == null || !PaintResources.ready(surface, recipe)) return;
        for (int i = 0; i < 3; i++) {
            result[i] |= surface.hasRegion(i) && recipe.config().channel(i) != null;
        }
    }

    /** Returns the current display's translation key or literal text. */
    public static String regionLabel(ItemStack stack, int region) {
        String fallback = PaintConfig.defaultRegionLabel(region);
        var display = TimelessAPI.getGunDisplay(stack).orElse(null);
        var gun = display == null ? null : PaintResources.gun(display.getDisplayId());
        return gun == null ? fallback : gun.config().regionLabel(region);
    }

}
