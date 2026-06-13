package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class GunExtraHolder {
    @NotNull
    @SerializedName("switch_ammo_condition")
    public SwitchAmmoCondition switchAmmoCondition = SwitchAmmoCondition.NONE;

    @Nullable
    @SerializedName("ammo_types")
    public List<GunExtraAmmo> ammoTypes = null;

    @Nullable
    @SerializedName("under_barrel")
    public UnderBarrel underBarrel = null;

    @SerializedName("apply_builtin_attachments_modifiers")
    public boolean applyBuiltinAttachmentsModifiers = false;

    @SerializedName("durability")
    public int durability = 0;

    @SerializedName("damage_probability")
    public float damageProbability = 1.0f;

    @SerializedName("show_vanilla_damage_bar")
    public boolean showVanillaDamageBar = false;

    @SerializedName("remove_on_damaged")
    public boolean removeOnDamaged = false;

    @SerializedName("remove_delay")
    public int removeDelay = 0;

    @Nullable
    @SerializedName("show_overheat_bar")
    public Boolean showOverheatBar = null;

    @SerializedName("show_hud")
    public boolean showHud = true;

    @SerializedName("disable_scroll_on_aim")
    public boolean disableScrollOnAim = false;

    @SerializedName("only_bolt_on_release")
    public boolean onlyBoltOnRelease = false;

    @Nullable
    @SerializedName("block_aim_while_reloading")
    public Boolean blockAimWhileReloading = null;

    @Nullable
    @SerializedName("shield")
    public Shield shield = null;

    @Nullable
    @SerializedName("aim_shake")
    public AimShake aimShake = null;
}
