package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class AttachmentExtraHolder {
    @SerializedName("advanced_rendering")
    public Boolean advancedRendering = null;

    @SerializedName("flashlight")
    public Flashlight flashlight = new Flashlight();

    @SerializedName("thermal_imaging")
    public boolean thermalImaging = false;

    @SerializedName("monochrome")
    public boolean monochrome = false;

    @SerializedName("night_vision")
    public boolean nightVision = false;

    @SerializedName("under_barrel")
    public UnderBarrel underBarrel = null;

    @SerializedName("switch_ammo_condition")
    public SwitchAmmoCondition switchAmmoCondition = SwitchAmmoCondition.NONE;

    @SerializedName("ammo_types")
    public List<GunExtraAmmo> ammoTypes = null;
}
