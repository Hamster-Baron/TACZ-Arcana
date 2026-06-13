package group.taczexpands.common.coremod

import com.tacz.guns.api.DefaultAssets
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IAmmo
import com.tacz.guns.api.item.IAmmoBox
import com.tacz.guns.api.item.IGun
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.api.item.nbt.AmmoBoxItemDataAccessor
import com.tacz.guns.api.item.nbt.AmmoItemDataAccessor
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor
import com.tacz.guns.api.item.nbt.GunItemDataAccessor
import com.tacz.guns.resource.index.CommonGunIndex
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.nbt.AttachmentExtras
import group.taczexpands.common.nbt.GunExtras
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.jvm.optionals.getOrNull

object PluginWrapper {
    @JvmStatic
    fun isAmmoOfGun(instance: AmmoItemDataAccessor, gun: ItemStack, ammo: ItemStack): Boolean {
        val iGun = gun.item
        val iAmmo = ammo.item
        if (iGun is IGun && iAmmo is IAmmo) {
            val gunId: ResourceLocation = iGun.getGunId(gun)
            val ammoId: ResourceLocation = iAmmo.getAmmoId(ammo)
            return TimelessAPI.getCommonGunIndex(gunId).map { gunIndex: CommonGunIndex ->
                IAccessorGunData.getCurrentAmmoId(
                    gunIndex.gunData,
                    gun
                ) == ammoId
            }.orElse(false)

        }
        return false
    }

    @JvmStatic
    fun isAmmoBoxOfGun(instance: AmmoBoxItemDataAccessor, gun: ItemStack, ammoBox: ItemStack): Boolean {
        val iGun = gun.item
        val iAmmoBox = ammoBox.item
        if (iGun is IGun && iAmmoBox is IAmmoBox) {
            if (instance.isAllTypeCreative(ammoBox)) {
                return true
            }
            val ammoId: ResourceLocation = iAmmoBox.getAmmoId(ammoBox)
            if (ammoId == DefaultAssets.EMPTY_AMMO_ID) {
                return false
            }
            val gunId: ResourceLocation = iGun.getGunId(gun)
            return TimelessAPI.getCommonGunIndex(gunId).map { gunIndex: CommonGunIndex ->
                IAccessorGunData.getCurrentAmmoId(
                    gunIndex.gunData,
                    gun
                ) == ammoId
            }.orElse(false)
        }
        return false
    }

    @JvmStatic
    fun getGunId(instance: GunItemDataAccessor, gun: ItemStack): ResourceLocation {
        if (GunExtras.getUsingUnderBarrel(gun)) {
            val underBarrel = GunExtras.getUnderBarrel(gun)
            if (underBarrel != null) {
                if (TimelessAPI.getCommonGunIndex(underBarrel.gunId).getOrNull() != null)
                    return underBarrel.gunId
            }
        }

        val nbt = gun.getOrCreateTag()
        if (nbt.contains(GunItemDataAccessor.GUN_ID_TAG, Tag.TAG_STRING.toInt())) {
            val gunId = ResourceLocation.tryParse(nbt.getString(GunItemDataAccessor.GUN_ID_TAG))
            return Objects.requireNonNullElse(gunId, DefaultAssets.EMPTY_GUN_ID)
        }
        return DefaultAssets.EMPTY_GUN_ID
    }

    @JvmStatic
    fun getOrCreateTag(itemStack: ItemStack): CompoundTag {
        if (GunExtras.getUsingUnderBarrel(itemStack)) {
            return GunExtras.getOrCreateUnderBarrelRootTag(itemStack)
        }
        return itemStack.orCreateTag
    }

    @JvmStatic
    fun getAttachmentTagPreCheck(gunAccessor: GunItemDataAccessor, gun: ItemStack, type: AttachmentType, tag: CompoundTag) {
        val key = GunItemDataAccessor.GUN_ATTACHMENT_BASE + type.name
        if (!tag.contains(key, Tag.TAG_COMPOUND.toInt())) {
            if (gunAccessor.getBuiltInAttachmentId(gun, type) != DefaultAssets.EMPTY_ATTACHMENT_ID) {
                val newTag = CompoundTag()
                ItemStack.EMPTY.save(newTag)
                newTag.put("tag", CompoundTag())
                tag.put(key, newTag)
            }
        }

    }
}