package group.taczexpands.common.nbt

import com.tacz.guns.resource.pojo.data.gun.InaccuracyType
import java.util.UUID

object GunKeys {
    var defaultLaserStateDelegate: () -> Boolean = { false }

    private val _allKeys = mutableListOf<NBTKey<*>>()
    private val _allModifierKeys = mutableListOf<FloatKey>()

    val allKeys: List<NBTKey<*>> = _allKeys
    val allModifierKeys: List<FloatKey> = _allModifierKeys

    private fun <T : NBTKey<*>> T.register(): T = this.apply { _allKeys.add(this) }

    private fun FloatKey.registerModifier(): FloatKey = this.apply {
        _allKeys.add(this)
        _allModifierKeys.add(this)
    }




    val ROOT = NullableCompoundTagKey("Extras").register()

    val UNDER_BARREL_ROOT = NullableCompoundTagKey("UnderBarrelRoot").register()



    val USING_UNDER_BARREL = BooleanKey("UsingUnderBarrel", { false }).register()
    val EXTRA_AMMO = NullableResourceLocationKey("ExtraAmmo").register()

    val RPM_MODIFIER = FloatKey("RPMModifier", { 1.0f }).registerModifier()
    val RELOAD_TIME_MODIFIER = FloatKey("ReloadTimeModifier", { 1.0f }).registerModifier()
    val AIM_TIME_MODIFIER = FloatKey("AimTimeModifier", { 1.0f }).registerModifier()
    val DRAW_TIME_MODIFIER = FloatKey("DrawSpeedModifier", { 1.0f }).registerModifier()
    val BOLT_TIME_MODIFIER = FloatKey("BoltSpeedModifier", { 1.0f }).registerModifier()
    private val INACCURACY_TYPE_MAP = (listOf(null) + InaccuracyType.entries).associateWith {
        val prefix = it?.name ?: "All"
        FloatKey("${prefix}RecoilModifier", { 1.0f }).registerModifier() to FloatKey("${prefix}SpreadModifier", { 1.0f }).registerModifier()
    }

    fun getRecoilKey(type: InaccuracyType?): FloatKey = INACCURACY_TYPE_MAP[type]!!.first
    fun getSpreadKey(type: InaccuracyType?): FloatKey = INACCURACY_TYPE_MAP[type]!!.second

    val MISSILE_FLIGHT_PROFILE_TYPE = NullableIntKey("MissileFlightProfileType").register()
    val ENFORCING_SIMPLE_LOCK_MODE = BooleanKey("EnforcingSimpleLockMode", { false }).register()

    val OVERRIDE_NIGHT_VISION = NullableBooleanKey("OverrideNightVision").register()
    val OVERRIDE_THERMAL_IMAGING = NullableBooleanKey("OverrideThermalImaging").register()
    val OVERRIDE_MONOCHROME = NullableBooleanKey("OverrideMonochrome").register()
    val OVERRIDE_BULLET_AMOUNT = IntKey("OverrideBulletAmount", { -1 }).register()
    val OVERRIDE_TEXTURE = NullableStringKey("OverrideTexture").register()
    val LASER = BooleanKey("Laser", { defaultLaserStateDelegate() }).register()
    val FLASHLIGHT = BooleanKey("Flashlight", { false }).register()
    val DURABILITY_DAMAGE = IntKey("DurabilityDamage", { 0 }).register()

    val OVERRIDE_GUN_SHIELD_BLOCKING_POWER = NullableFloatKey("OverrideBlockingPower").register()

    val GUN_UUID = NullableStringKey("Gun_UUID").register()
}