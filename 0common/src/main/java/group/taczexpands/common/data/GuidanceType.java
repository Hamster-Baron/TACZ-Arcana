package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public enum GuidanceType {
    @SerializedName("none")
    NONE,
    @SerializedName("semi_active_laser")
    SEMI_ACTIVE_LASER,
    @SerializedName("semi_active_laser_2")
    SEMI_ACTIVE_LASER_2,
    @SerializedName("tv")
    TV(true),
    @SerializedName("drone")
    DRONE(true),
    @SerializedName("active_radar")
    ACTIVE_RADAR;

    GuidanceType() {
    }

    GuidanceType(boolean isRemoteCamera) {
        this.isRemoteCamera = isRemoteCamera;
    }

    private boolean isRemoteCamera = false;

    public boolean isRemoteCamera() {
        return isRemoteCamera;
    }

}