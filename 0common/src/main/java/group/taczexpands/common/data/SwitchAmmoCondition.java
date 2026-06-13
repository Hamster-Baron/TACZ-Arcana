package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public enum SwitchAmmoCondition {
    @SerializedName("none")
    NONE,
    @SerializedName("not_full")
    NOT_FULL,
    @SerializedName("empty")
    EMPTY
}
