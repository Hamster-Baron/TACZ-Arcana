package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;

public class Flashlight {
    @SerializedName("enable")
    public boolean enable = false;

    @SerializedName("angle")
    public float angle = 30.0f;

    @SerializedName("range")
    public float range = 128.0f;

    @SerializedName("luminance")
    public float luminance = 8.0f;
}
