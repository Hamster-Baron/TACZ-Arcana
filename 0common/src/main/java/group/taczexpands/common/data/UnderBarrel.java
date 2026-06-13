package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class UnderBarrel {
    @SerializedName("gun")
    public ResourceLocation gunId = null;

    @SerializedName("gun_display_namespace")
    public String gunDisplayNamespace = null;

    @SerializedName("gun_display_prefix")
    public String gunDisplayPrefix = "sub_";

    @SerializedName("ignore_silencer")
    public boolean ignoreSilencer = true;

    @SerializedName("animation_lock_time")
    public float animationLockTime = 0.0f;

    public ResourceLocation getDisplayId(ResourceLocation original) {
        var location = original;
        var namespace = location.getNamespace();
        if (gunDisplayNamespace != null) {
            namespace = gunDisplayNamespace;
        }
        return new ResourceLocation(namespace, gunDisplayPrefix + location.getPath());
    }
}
