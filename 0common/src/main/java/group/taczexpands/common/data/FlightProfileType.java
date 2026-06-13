package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public enum FlightProfileType {
    @SerializedName("pure_pursuit")
    PURE_PURSUIT,
    @SerializedName("height_offset_pure_pursuit")
    HEIGHT_OFFSET_PURE_PURSUIT,
    @SerializedName("proportional_navigation")
    PROPORTIONAL_NAVIGATION,
    @SerializedName("top_attack")
    TOP_ATTACK,
}
