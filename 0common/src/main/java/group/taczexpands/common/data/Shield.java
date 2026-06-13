package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public class Shield {
    @SerializedName("blocking_angle")
    public float blockingAngle = 90.0f;

    @SerializedName("blocking_condition")
    public ShieldBlockCondition blockingCondition = ShieldBlockCondition.WHEN_AIMING;

    @SerializedName("can_be_disabled_by_axes")
    public boolean canBeDisabledByAxes = true;

    @SerializedName("blocking_power")
    public float blockingPower = 1.0f;

    @SerializedName("cooldown")
    public int cooldown = 100;

    @SerializedName("base_durability_damage")
    public int baseDurabilityDamage = 1;

    @SerializedName("extra_durability_damage_on_blocked")
    public int extraDurabilityDamageOnBlocked = 1;
}
