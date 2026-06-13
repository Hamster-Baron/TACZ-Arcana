package group.taczexpands.common.nbt

import com.tacz.guns.api.DefaultAssets
import com.tacz.guns.api.TimelessAPI
import com.tacz.guns.api.item.IGun
import com.tacz.guns.api.item.attachment.AttachmentType
import com.tacz.guns.api.item.builder.AttachmentItemBuilder
import com.tacz.guns.api.item.nbt.AttachmentItemDataAccessor
import com.tacz.guns.api.item.nbt.GunItemDataAccessor
import com.tacz.guns.init.ModItems
import com.tacz.guns.resource.index.CommonGunIndex
import com.tacz.guns.resource.pojo.data.gun.InaccuracyType
import group.taczexpands.common.accessor.IAccessorAttachmentData
import group.taczexpands.common.accessor.IAccessorGunData
import group.taczexpands.common.data.UnderBarrel
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import java.util.*
import kotlin.jvm.optionals.getOrNull

object GunExtras {

    val GUN_UUID = "GunUUID"

    fun getOrCreateRootTag(gun: ItemStack): CompoundTag {
        val nbt = gun.orCreateTag
        return nbt.getOrSet(GunKeys.ROOT) { CompoundTag() }!!
    }

    fun getUnderBarrelRootTag(gun: ItemStack): CompoundTag? {
        val nbt = getOrCreateRootTag(gun)
        return nbt.getOrDefault(GunKeys.UNDER_BARREL_ROOT)
    }

    fun getOrCreateUnderBarrelRootTag(gun: ItemStack): CompoundTag {
        val nbt = getOrCreateRootTag(gun)
        return nbt.getOrSet(GunKeys.UNDER_BARREL_ROOT) {
            CompoundTag().also { newRoot ->
                TimelessAPI.getCommonGunIndex(getUnderBarrel(gun)?.gunId).ifPresent {
                    newRoot.putString(GunItemDataAccessor.GUN_FIRE_MODE_TAG, it.gunData.fireModeSet[0].name)
                }
            }
        }!!
    }

    fun deleteUnderBarrelRootTag(gun: ItemStack) {
        val nbt = getOrCreateRootTag(gun)
        if (nbt.has(GunKeys.UNDER_BARREL_ROOT)) {
            nbt.unset(GunKeys.UNDER_BARREL_ROOT)
        }
    }

    fun getRootTag(gun: ItemStack): CompoundTag? {
        val nbt = gun.orCreateTag
        return nbt.getOrDefault(GunKeys.ROOT)
    }

    fun getRootTag(nbt: CompoundTag): CompoundTag? {
        return nbt.getOrDefault(GunKeys.ROOT)
    }

    fun getRootTag(shooter: LivingEntity?): CompoundTag? {
        if (shooter == null) return null
        return PlayerExtras.getPlayerExtraData(shooter)
    }

    fun getUUID(gun: ItemStack): String? {
        val tag = getRootTag(gun) ?: return null
        if (tag.has(GunKeys.GUN_UUID)) {
            return tag.getOrDefault(GunKeys.GUN_UUID)
        } else {
            return null
        }
    }

    fun checkUUID(gun: ItemStack) {
        val tag = getOrCreateRootTag(gun)
        tag.getOrSet(GunKeys.GUN_UUID, { UUID.randomUUID().toString() })
    }

    fun getCurrentExtraAmmoId(gun: ItemStack): ResourceLocation? {
        if (!getUsingUnderBarrel(gun)) {
            val tag = getRootTag(gun) ?: return null
            return tag.getOrDefault(GunKeys.EXTRA_AMMO)
        } else {
            val tag = getUnderBarrelRootTag(gun) ?: return null
            return tag.getOrDefault(GunKeys.EXTRA_AMMO)
        }
    }

    fun setCurrentExtraAmmoId(gun: ItemStack, ammo: ResourceLocation?) {
        if (!getUsingUnderBarrel(gun)) {
            val tag = getOrCreateRootTag(gun)
            tag.set(GunKeys.EXTRA_AMMO, ammo)
        } else {
            val tag = getOrCreateUnderBarrelRootTag(gun)
            tag.set(GunKeys.EXTRA_AMMO, ammo)
        }
    }

    fun getOverrideBulletAmount(gun: ItemStack): Int {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.OVERRIDE_BULLET_AMOUNT)
    }

    fun setOverrideBulletAmount(gun: ItemStack, value: Int?) {
        val tag = getOrCreateRootTag(gun)
        if (value != null)
            tag.set(GunKeys.OVERRIDE_BULLET_AMOUNT, value)
        else tag.unset(GunKeys.OVERRIDE_BULLET_AMOUNT)
    }

    fun getDurabilityDamage(gun: ItemStack): Int {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.DURABILITY_DAMAGE)
    }

    fun setDurabilityDamage(gun: ItemStack, value: Int) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.DURABILITY_DAMAGE, value)
    }

    fun getOverrideThermalImaging(gun: ItemStack): Boolean? {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.OVERRIDE_THERMAL_IMAGING)
    }

    fun setOverrideThermalImaging(gun: ItemStack, value: Boolean?) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.OVERRIDE_THERMAL_IMAGING, value)
    }

    fun getOverrideNightVision(gun: ItemStack): Boolean? {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.OVERRIDE_NIGHT_VISION)
    }

    fun setOverrideNightVision(gun: ItemStack, value: Boolean?) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.OVERRIDE_NIGHT_VISION, value)
    }

    fun getOverrideMonochrome(gun: ItemStack): Boolean? {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.OVERRIDE_MONOCHROME)

    }

    fun setOverrideMonochrome(gun: ItemStack, value: Boolean?) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.OVERRIDE_MONOCHROME, value)
    }

    fun getOverrideTexture(gun: ItemStack): String? {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.OVERRIDE_TEXTURE)
    }

    fun setOverrideTexture(gun: ItemStack, value: String?) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.OVERRIDE_TEXTURE, value)
    }

    fun getOverrideGunShieldBlockingPower(gun: ItemStack): Float? {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.OVERRIDE_GUN_SHIELD_BLOCKING_POWER)
    }

    fun setOverrideGunShieldBlockingPower(gun: ItemStack, value: Float?) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.OVERRIDE_GUN_SHIELD_BLOCKING_POWER, value)
    }

    fun getLaser(gun: ItemStack): Boolean {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.LASER)
    }

    fun setLaser(gun: ItemStack, value: Boolean) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.LASER, value)
    }

    fun getFlashlight(gun: ItemStack): Boolean {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.FLASHLIGHT)

    }

    fun setFlashlight(gun: ItemStack, value: Boolean) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.FLASHLIGHT, value)
    }

    fun getEnforcingSimpleLocking(gun: ItemStack): Boolean {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.ENFORCING_SIMPLE_LOCK_MODE)
    }

    fun setEnforcingSimpleLocking(gun: ItemStack, value: Boolean) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.ENFORCING_SIMPLE_LOCK_MODE, value)
    }

    fun getMissileFlightProfileType(gun: ItemStack): Int? {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.MISSILE_FLIGHT_PROFILE_TYPE)
    }

    fun setMissileFlightProfileType(gun: ItemStack, value: Int) {
        val tag = getOrCreateRootTag(gun)

        if (value >= 0)
            tag.set(GunKeys.MISSILE_FLIGHT_PROFILE_TYPE, value)
        else
            tag.unset(GunKeys.MISSILE_FLIGHT_PROFILE_TYPE)
    }

    fun getRPMModifier(shooter: LivingEntity?, gun: ItemStack): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.RPM_MODIFIER) * tag.getOrDefault(GunKeys.RPM_MODIFIER)
    }

    fun setRPMModifier(gun: ItemStack, value: Float) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.RPM_MODIFIER, value)
    }

    fun getRecoilModifier(shooter: LivingEntity?, gun: ItemStack, type: InaccuracyType?): Float {
        val key = GunKeys.getRecoilKey(type)
        val allKey = GunKeys.getRecoilKey(null)
        val tag = getRootTag(gun)
        val gunValue = if (tag.has(key)) tag.getOrDefault(key) else tag.getOrDefault(allKey)
        val shooterTag = getRootTag(shooter)
        val shooterValue = if (shooterTag.has(key)) shooterTag.getOrDefault(key) else shooterTag.getOrDefault(allKey)

        return gunValue * shooterValue
    }

    fun setRecoilModifier(gun: ItemStack, type: InaccuracyType?, value: Float) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.getRecoilKey(type), value)
    }

    fun getSpreadModifier(shooter: LivingEntity?, gun: ItemStack, type: InaccuracyType?): Float {
        val key = GunKeys.getSpreadKey(type)
        val allKey = GunKeys.getSpreadKey(null)
        val tag = getRootTag(gun)
        val gunValue = if (tag.has(key)) tag.getOrDefault(key) else tag.getOrDefault(allKey)
        val shooterTag = getRootTag(shooter)
        val shooterValue = if (shooterTag.has(key)) shooterTag.getOrDefault(key) else shooterTag.getOrDefault(allKey)

        return gunValue * shooterValue
    }

    fun setSpreadModifier(gun: ItemStack, type: InaccuracyType?, value: Float) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.getSpreadKey(type), value)
    }

    fun getReloadTimeModifier(shooter: LivingEntity?, gun: ItemStack): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.RELOAD_TIME_MODIFIER) * tag.getOrDefault(GunKeys.RELOAD_TIME_MODIFIER)
    }

    fun getReloadTimeModifier(shooter: LivingEntity?, gun: CompoundTag): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.RELOAD_TIME_MODIFIER) * tag.getOrDefault(GunKeys.RELOAD_TIME_MODIFIER)
    }

    fun setReloadTimeModifier(gun: ItemStack, value: Float) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.RELOAD_TIME_MODIFIER, value)
    }

    fun getAimTimeModifier(shooter: LivingEntity?, gun: ItemStack): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.AIM_TIME_MODIFIER) * tag.getOrDefault(GunKeys.AIM_TIME_MODIFIER)
    }

    fun getAimTimeModifier(shooter: LivingEntity?, gun: CompoundTag): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.AIM_TIME_MODIFIER) * tag.getOrDefault(GunKeys.AIM_TIME_MODIFIER)
    }

    fun setAimTimeModifier(gun: ItemStack, value: Float) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.AIM_TIME_MODIFIER, value)
    }

    fun getDrawTimeModifier(shooter: LivingEntity?, gun: ItemStack): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.DRAW_TIME_MODIFIER) * tag.getOrDefault(GunKeys.DRAW_TIME_MODIFIER)
    }

    fun getDrawTimeModifier(shooter: LivingEntity?, gun: CompoundTag): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.DRAW_TIME_MODIFIER) * tag.getOrDefault(GunKeys.DRAW_TIME_MODIFIER)
    }

    fun setDrawTimeModifier(gun: ItemStack, value: Float) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.DRAW_TIME_MODIFIER, value)
    }

    fun getBoltTimeModifier(shooter: LivingEntity?, gun: ItemStack): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.BOLT_TIME_MODIFIER) * tag.getOrDefault(GunKeys.BOLT_TIME_MODIFIER)
    }

    fun getBoltTimeModifier(shooter: LivingEntity?, gun: CompoundTag): Float {
        val tag = getRootTag(gun)
        return getRootTag(shooter).getOrDefault(GunKeys.BOLT_TIME_MODIFIER) * tag.getOrDefault(GunKeys.BOLT_TIME_MODIFIER)
    }

    fun setBoltTimeModifier(gun: ItemStack, value: Float) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.BOLT_TIME_MODIFIER, value)
    }

    fun getUsingUnderBarrel(gun: ItemStack): Boolean {
        val tag = getRootTag(gun)
        return tag.getOrDefault(GunKeys.USING_UNDER_BARREL)
    }

    fun setUsingUnderBarrel(gun: ItemStack, value: Boolean) {
        val tag = getOrCreateRootTag(gun)
        tag.set(GunKeys.USING_UNDER_BARREL, value)
    }

    fun getExtraUnderBarrel(gun: ItemStack): UnderBarrel? {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (gun.item != gunItem) return null
        val attachmentId = getAttachmentId(gun, AttachmentType.GRIP)
        if (attachmentId == DefaultAssets.EMPTY_ATTACHMENT_ID) return null
        val index = TimelessAPI.getCommonAttachmentIndex(attachmentId).getOrNull() ?: return null
        val holder = IAccessorAttachmentData.getExtraHolder(index.data) ?: return null
        return holder.underBarrel
    }

    fun getBuiltinUnderBarrel(gun: ItemStack): UnderBarrel? {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (gun.item != gunItem) return null
        val attachmentId = getBuiltInAttachmentId(gun, AttachmentType.GRIP)
        if (attachmentId == DefaultAssets.EMPTY_ATTACHMENT_ID) return null
        val index = TimelessAPI.getCommonAttachmentIndex(attachmentId).getOrNull() ?: return null
        val holder = IAccessorAttachmentData.getExtraHolder(index.data) ?: return null
        return holder.underBarrel
    }

    fun getGunDataUnderBarrel(gun: ItemStack): UnderBarrel? {
        val gunItem = ModItems.MODERN_KINETIC_GUN.get()
        if (gun.item != gunItem) return null
        val index = TimelessAPI.getCommonGunIndex(getGunId(gun)).getOrNull() ?: return null
        return IAccessorGunData.getExtraHolder(index.gunData)?.underBarrel
    }

    fun getUnderBarrel(gun: ItemStack): UnderBarrel? {
        return getExtraUnderBarrel(gun) ?: getBuiltinUnderBarrel(gun) ?: getGunDataUnderBarrel(gun)
    }

    fun getAttachmentId(gun: ItemStack, type: AttachmentType): ResourceLocation {
        val attachmentTag: CompoundTag? = getAttachmentTag(gun, type)
        if (attachmentTag != null) {
            return AttachmentItemDataAccessor.getAttachmentIdFromTag(attachmentTag)
        }
        return DefaultAssets.EMPTY_ATTACHMENT_ID
    }

    fun getBuiltInAttachmentId(gun: ItemStack, type: AttachmentType): ResourceLocation {
        val iGun = IGun.getIGunOrNull(gun)
        if (iGun == null) {
            return DefaultAssets.EMPTY_ATTACHMENT_ID
        } else {
            val index = TimelessAPI.getCommonGunIndex(getGunId(gun)).orElse(null)
            if (index != null) {
                val builtin = index.gunData.builtInAttachments
                if (builtin.containsKey(type)) {
                    return builtin[type]!!
                }
            }

            return DefaultAssets.EMPTY_ATTACHMENT_ID
        }
    }

    fun getAttachmentTag(gun: ItemStack, type: AttachmentType): CompoundTag? {
        if (!allowAttachmentType(gun, type)) {
            return null
        }
        val nbt = gun.getOrCreateTag()
        val key = GunItemDataAccessor.GUN_ATTACHMENT_BASE + type.name
        if (nbt.contains(key, Tag.TAG_COMPOUND.toInt())) {
            val allItemStackTag = nbt.getCompound(key)
            if (allItemStackTag.contains("tag", Tag.TAG_COMPOUND.toInt())) {
                return allItemStackTag.getCompound("tag")
            }
        }
        return null
    }

    fun allowAttachmentType(gun: ItemStack, type: AttachmentType): Boolean {
        if (type.name == "BULLET") return true

        val iGun = IGun.getIGunOrNull(gun)
        if (iGun != null) {
            return TimelessAPI.getCommonGunIndex(getGunId(gun)).map { gunIndex: CommonGunIndex ->
                val allowAttachments =
                    gunIndex.gunData.allowAttachments ?: return@map false
                allowAttachments.contains(type)
            }.orElse(false)
        } else {
            return false
        }
    }

    fun getGunId(gun: ItemStack): ResourceLocation {
        val nbt = gun.getOrCreateTag()
        if (nbt.contains(GunItemDataAccessor.GUN_ID_TAG, Tag.TAG_STRING.toInt())) {
            val gunId = ResourceLocation.tryParse(nbt.getString(GunItemDataAccessor.GUN_ID_TAG))
            return Objects.requireNonNullElse(gunId, DefaultAssets.EMPTY_GUN_ID)
        }
        return DefaultAssets.EMPTY_GUN_ID
    }

    private fun getOriginalGunDisplayId(gun: ItemStack): ResourceLocation {
        val nbt: CompoundTag = gun.orCreateTag
        if (nbt.contains("GunDisplayId", 8)) {
            return ResourceLocation.tryParse(nbt.getString("GunDisplayId")) ?: DefaultAssets.DEFAULT_GUN_DISPLAY_ID
        } else {
            return DefaultAssets.DEFAULT_GUN_DISPLAY_ID
        }
    }

    fun getSoundGunDisplayId(gun: ItemStack): ResourceLocation? {
        if (getUnderBarrel(gun) != null && !getUsingUnderBarrel(gun)) {
            return getOriginalGunDisplayId(gun)
        }
        return null
    }
}
