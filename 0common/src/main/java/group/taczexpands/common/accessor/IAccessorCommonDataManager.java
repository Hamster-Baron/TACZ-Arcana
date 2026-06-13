package group.taczexpands.common.accessor;

import com.tacz.guns.resource.ICommonResourceProvider;
import group.taczexpands.common.data.GunExtraAmmo;
import group.taczexpands.common.data.ResistanceTable;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

public interface IAccessorCommonDataManager {
    GunExtraAmmo taczexpands$getAmmoData(ResourceLocation id);

    ResistanceTable taczexpands$getResistanceTableData(ResourceLocation id);

    static GunExtraAmmo getAmmoData(@NotNull ICommonResourceProvider manager, @NotNull ResourceLocation id) {
        return ((IAccessorCommonDataManager) manager).taczexpands$getAmmoData(id);
    }

    static ResistanceTable getResistanceTableData(@NotNull ICommonResourceProvider manager, @NotNull ResourceLocation id) {
        return ((IAccessorCommonDataManager) manager).taczexpands$getResistanceTableData(id);
    }
}
