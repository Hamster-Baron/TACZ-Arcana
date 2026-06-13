package group.taczexpands.common.data;

import com.google.gson.annotations.SerializedName;
import net.minecraft.resources.ResourceLocation;

public class BlockEntry {
    @SerializedName("key")
    public String key;

    @SerializedName("resistance")
    public int resistance;

    @SerializedName("destroyable")
    public boolean destroyable;

    @SerializedName("show_particle")
    public boolean showParticle;

    @SerializedName("accumulate_damage")
    public boolean accumulateDamage;

    @SerializedName("deflectable")
    public boolean deflectable;

    @SerializedName("bypass_global_destroy_limit")
    public boolean bypassGlobalDestroyLimit = false;

}