package group.taczexpands.common.accessor;

import com.tacz.guns.api.TimelessAPI;
import com.tacz.guns.api.item.IGun;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import group.taczexpands.common.data.GunExtraAmmo;
import group.taczexpands.common.data.GunExtraHolder;
import group.taczexpands.common.data.SwitchAmmoCondition;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;

public interface IAccessorGunData {
    static SwitchAmmoCondition getSwitchAmmoCondition(GunData gunData, ItemStack gunItemStack) {
        var conditionList = IAccessorAttachmentData.getAllExtraHolder(gunItemStack).stream().map(holder -> holder.switchAmmoCondition).collect(Collectors.toCollection(ArrayList::new));
        var holder = getExtraHolder(gunData);
        if (holder != null) {
            conditionList.add(holder.switchAmmoCondition);
        }
        return conditionList.stream().max(Comparator.comparingInt(Enum::ordinal)).orElse(SwitchAmmoCondition.NONE);
    }

    static List<GunExtraAmmo> getExtraAmmoList(GunData gunData, ItemStack gunItemStack) {
        var conditionList = IAccessorAttachmentData.getAllExtraHolder(gunItemStack).stream().map(holder -> holder.ammoTypes).filter(Objects::nonNull).collect(Collectors.toCollection(ArrayList::new));

        var holder = getExtraHolder(gunData);
        if (holder != null && holder.ammoTypes != null) {
            conditionList.add(holder.ammoTypes);
        }
        return conditionList.stream().flatMap(Collection::stream).toList();
    }

    @NotNull
    static GunExtraHolder getExtraHolder(GunData gunData) {
        return ((IAccessorGunData) gunData).taczexpands$getExtraHolder();
    }

    @Nullable
    static GunData getGunData(ItemStack itemStack) {
        var iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) return null;
        var gunIndex = TimelessAPI.getCommonGunIndex(iGun.getGunId(itemStack)).orElse(null);
        if (gunIndex == null || gunIndex.getGunData() == null) return null;
        return gunIndex.getGunData();
    }

    @Nullable
    static GunExtraHolder getExtraHolder(ItemStack itemStack) {
        var iGun = IGun.getIGunOrNull(itemStack);
        if (iGun == null) return null;
        var gunIndex = TimelessAPI.getCommonGunIndex(iGun.getGunId(itemStack)).orElse(null);
        if (gunIndex == null || gunIndex.getGunData() == null) return null;
        return IAccessorGunData.getExtraHolder(gunIndex.getGunData());
    }

    static boolean isValidAmmoId(GunData gunData, ItemStack gunItemStack, ResourceLocation location) {
        if (location == null) return false;
        if (getCurrentBaseAmmo(gunData, gunItemStack) == null && gunData.getAmmoId().equals(location)) {
            return true;
        }
        if (getExtraAmmo(gunData, gunItemStack, location, true) != null) {
            return true;
        }
        return false;
    }

    @Nullable
    static GunExtraAmmo getExtraAmmo(GunData gunData, ItemStack gunItemStack, ResourceLocation location, boolean containsBaseOverride) {
        if (location == null) return null;
        var list = getExtraAmmoList(gunData, gunItemStack);
        if (list == null) return null;
        return list.stream().filter((it) -> {
            return it.getAmmoId().equals(location) && (!it.isOverrideGunBaseAmmo() || (it.isOverrideGunBaseAmmo() && containsBaseOverride));
        }).findFirst().orElse(null);
    }

    @Nullable
    static GunExtraAmmo getCurrentExtraAmmo(GunData gunData, ItemStack gunItem) {
        if (gunItem == null) return null;
        var location = GunExtras.INSTANCE.getCurrentExtraAmmoId(gunItem);
        return getExtraAmmo(gunData, gunItem, location, false);
    }

    @Nullable
    static GunExtraAmmo getCurrentBaseAmmo(GunData gunData, ItemStack gunItem) {
        if (gunItem == null || GunExtras.INSTANCE.getUsingUnderBarrel(gunItem)) return null;
        return getExtraAmmoList(gunData, gunItem).stream().filter(GunExtraAmmo::isOverrideGunBaseAmmo).findFirst().orElse(null);
    }

    @Nullable
    static GunExtraAmmo getCurrentAmmo(GunData gunData, ItemStack gunItem) {
        var extra = getCurrentExtraAmmo(gunData, gunItem);
        if (extra != null) return extra;
        return getCurrentBaseAmmo(gunData, gunItem);
    }

    static ResourceLocation getCurrentAmmoId(GunData gunData, ItemStack gunItem) {
        var extraAmmo = getCurrentExtraAmmo(gunData, gunItem);
        if (extraAmmo == null) {
            var baseAmmo = getCurrentBaseAmmo(gunData, gunItem);
            if (baseAmmo != null) {
                return baseAmmo.getAmmoId();
            }
            return gunData.getAmmoId();
        } else return extraAmmo.getAmmoId();
    }

    static int getCurrentAmmoAmount(GunData gunData, ItemStack gunItem) {
        var extraAmmo = getCurrentExtraAmmo(gunData, gunItem);
        if (extraAmmo == null || extraAmmo.isInherit()) {
            var baseAmmo = getCurrentBaseAmmo(gunData, gunItem);
            if (baseAmmo != null) {
                return baseAmmo.getAmmoAmount();
            }
            return gunData.getAmmoAmount();
        } else return extraAmmo.getAmmoAmount();
    }

    static int[] getCurrentExtendedMagAmmoAmount(GunData gunData, ItemStack gunItem) {
        var extraAmmo = getCurrentExtraAmmo(gunData, gunItem);
        if (extraAmmo == null || extraAmmo.isInherit()) {
            var baseAmmo = getCurrentBaseAmmo(gunData, gunItem);
            if (baseAmmo != null) {
                return baseAmmo.getExtendedMagAmmoAmount();
            }
            return gunData.getExtendedMagAmmoAmount();
        } else return extraAmmo.getExtendedMagAmmoAmount();
    }

    static BulletData getCurrentBulletData(GunData gunData, ItemStack gunItem) {
        var extraAmmo = getCurrentExtraAmmo(gunData, gunItem);
        if (extraAmmo == null || extraAmmo.isInherit()) {
            var baseAmmo = getCurrentBaseAmmo(gunData, gunItem);
            if (baseAmmo != null) {
                return baseAmmo.getBulletData();
            }
            return gunData.getBulletData();
        } else return extraAmmo.getBulletData();
    }


    @NotNull
    GunExtraHolder taczexpands$getExtraHolder();
}
