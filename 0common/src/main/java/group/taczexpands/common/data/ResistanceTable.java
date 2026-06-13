package group.taczexpands.common.data;


import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.CommonAssetsManager;
import group.taczexpands.common.accessor.IAccessorCommonDataManager;
import group.taczexpands.common.util.BlockResistanceData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

public class ResistanceTable {
    @SerializedName("reference")
    private ResourceLocation reference = null;

    @SerializedName("table")
    private List<BlockEntry> resistanceTable = null;

    @Nullable
    public transient Function<BlockState, BlockResistanceData> blockResistanceCache = null;

    private ResistanceTable getReference() {
        if (reference != null)
            return IAccessorCommonDataManager.getResistanceTableData(CommonAssetsManager.get(), reference);
        return this;
    }

    public List<BlockEntry> getResistanceTable() {
        if (resistanceTable != null)
            return resistanceTable;
        var ref = getReference().resistanceTable;
        if (ref != null)
            return ref;
        return List.of();
    }

}
