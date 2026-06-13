package group.taczexpands.common.mixin;

import com.google.gson.JsonParseException;
import com.tacz.guns.GunMod;
import com.tacz.guns.resource.network.CommonNetworkCache;
import com.tacz.guns.resource.network.DataType;
import group.taczexpands.common.accessor.IAccessorCommonDataManager;
import group.taczexpands.common.data.GunExtraAmmo;
import group.taczexpands.common.data.ResistanceTable;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;
import java.util.Map;

@Mixin(value = CommonNetworkCache.class, remap = false)
public abstract class MixinCommonNetworkCache implements IAccessorCommonDataManager {
    @Shadow
    protected abstract <T> T parse(String json, Class<T> dataClass);

    @Unique
    public Map<ResourceLocation, GunExtraAmmo> taczexpands$ammoData = new HashMap<>();

    @Unique
    public Map<ResourceLocation, ResistanceTable> taczexpands$resistanceTable = new HashMap<>();

    @Inject(method = "fromNetwork(Ljava/util/Map;)V", at = @At("HEAD"))
    public void preFromNetwork(Map<DataType, Map<ResourceLocation, String>> cache, CallbackInfo ci) {
        taczexpands$ammoData.clear();
        taczexpands$resistanceTable.clear();

    }

    @Inject(method = "fromNetwork(Lcom/tacz/guns/resource/network/DataType;Ljava/util/Map;)V", at = @At("HEAD"), cancellable = true)
    public void preFromNetwork(DataType type, Map<ResourceLocation, String> data, CallbackInfo ci) {
        if (type == DataType.valueOf("AMMO_DATA")) {
            ci.cancel();
            for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
                try {
                    taczexpands$ammoData.put(entry.getKey(), parse(entry.getValue(), GunExtraAmmo.class));
                } catch (IllegalArgumentException | JsonParseException exception) {
                    GunMod.LOGGER.warn("Failed to parse data from network for {} with id {}", type, entry.getKey(), exception);
                }
            }
        } else if (type == DataType.valueOf("RESISTANCE_TABLE_DATA")) {
            ci.cancel();
            for (Map.Entry<ResourceLocation, String> entry : data.entrySet()) {
                try {
                    taczexpands$resistanceTable.put(entry.getKey(), parse(entry.getValue(), ResistanceTable.class));
                } catch (IllegalArgumentException | JsonParseException exception) {
                    GunMod.LOGGER.warn("Failed to parse data from network for {} with id {}", type, entry.getKey(), exception);
                }
            }
        }


    }

    @Override
    public GunExtraAmmo taczexpands$getAmmoData(ResourceLocation id) {
        return taczexpands$ammoData.get(id);
    }

    @Override
    public ResistanceTable taczexpands$getResistanceTableData(ResourceLocation id) {
        return taczexpands$resistanceTable.get(id);
    }
}
