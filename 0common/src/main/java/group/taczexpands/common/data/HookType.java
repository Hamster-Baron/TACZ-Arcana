package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public enum HookType {
    @SerializedName("pulling")
    PULLING,

    @SerializedName("chasing")
    CHASING,

    @SerializedName("converging")
    CONVERGING
}
