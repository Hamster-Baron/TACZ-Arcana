package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public enum ShieldBlockCondition {
    @SerializedName("when_aiming")
    WHEN_AIMING,

    @SerializedName("when_not_aiming")
    WHEN_NOT_AIMING,

    @SerializedName("always")
    ALWAYS
}
