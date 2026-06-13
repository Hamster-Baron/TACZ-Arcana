package group.taczexpands.common.mixin;

import com.google.gson.Gson;
import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.manager.CommonDataManager;
import com.tacz.guns.resource.manager.INetworkCacheReloadListener;
import com.tacz.guns.resource.network.DataType;
import group.taczexpands.common.accessor.IAccessorCommonDataManager;
import group.taczexpands.common.data.GunExtraAmmo;
import group.taczexpands.common.data.ResistanceTable;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = CommonAssetsManager.class, remap = false)
public abstract class MixinCommonAssetsManager implements IAccessorCommonDataManager {
    @Shadow
    protected abstract <T extends INetworkCacheReloadListener> T register(T listener);

    @Shadow
    @Final
    public static Gson GSON;

    @Unique
    private CommonDataManager<GunExtraAmmo> taczexpands$ammoData;

    @Unique
    private CommonDataManager<ResistanceTable> taczexpands$resistanceTable;

    @Inject(method = "reloadAndRegister", at = @At(value = "HEAD"))
    private void injectCustomManager(CallbackInfo ci) {
        taczexpands$ammoData = register(new CommonDataManager<>(DataType.valueOf("AMMO_DATA"), GunExtraAmmo.class, GSON, "data/ammo", "AmmoDataLoader"));
        taczexpands$resistanceTable = register(new CommonDataManager<>(DataType.valueOf("RESISTANCE_TABLE_DATA"), ResistanceTable.class, GSON, "data/resistance_tables", "ResistanceTableDataLoader"));
    }

    @Override
    public GunExtraAmmo taczexpands$getAmmoData(ResourceLocation id) {
        return taczexpands$ammoData.getData(id);
    }

    @Override
    public ResistanceTable taczexpands$getResistanceTableData(ResourceLocation id) {
        return taczexpands$resistanceTable.getData(id);
    }

}
