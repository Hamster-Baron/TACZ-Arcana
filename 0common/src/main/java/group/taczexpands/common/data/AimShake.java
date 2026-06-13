package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public class AimShake {
    @SerializedName("delay")
    public long delay;

    @SerializedName("amplitude_y_rot")
    public float amplitudeYRot;

    @SerializedName("amplitude_x_rot")
    public float amplitudeXRot;

    @SerializedName("frequency_hz")
    public float frequencyHz = 0.5f;

    @SerializedName("randomness")
    public float randomness = 0.1f;

    @SerializedName("dynamic_phase_offset")
    public boolean dynamicPhaseOffset = true;

    @SerializedName("phase_offset_y_rot")
    public Float phaseOffsetYRot = null;

    @SerializedName("phase_offset_x_rot")
    public Float phaseOffsetXRot = null;
}
