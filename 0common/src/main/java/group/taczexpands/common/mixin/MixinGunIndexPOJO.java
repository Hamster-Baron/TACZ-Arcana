package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.pojo.GunIndexPOJO;
import group.taczexpands.common.accessor.IAccessorGunIndexPOJO;
import group.taczexpands.common.accessor.IAccessorMiscPOJO;
import group.taczexpands.common.accessor.IAccessorPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.ArrayList;
import java.util.List;

@Mixin(value = GunIndexPOJO.class, remap = false)
public class MixinGunIndexPOJO implements IAccessorMiscPOJO, IAccessorPreview, IAccessorGunIndexPOJO {
    @Unique
    @SerializedName("hidden")
    private boolean taczexpands$hidden = false;

    @Unique
    @SerializedName("misc")
    private boolean taczexpands$misc = false;

    @Unique
    @SerializedName("preview")
    private boolean taczexpands$preview = false;

    @Unique
    @SerializedName("preview_scale")
    private float taczexpands$previewScale = 40.0f;

    @Unique
    @SerializedName("preview_offset_x")
    private float taczexpands$previewOffsetX = 0.0f;

    @Unique
    @SerializedName("preview_offset_y")
    private float taczexpands$previewOffsetY = 0.0f;

    @Unique
    @SerializedName("preview_offset_z")
    private float taczexpands$previewOffsetZ = 0.0f;

    @Unique
    @SerializedName("preview_title")
    private String taczexpands$previewTitle;

    @Unique
    @SerializedName("preview_detail")
    private String taczexpands$previewDetail;

    @Unique
    @SerializedName("tooltip_hide_flags")
    private List<String> taczexpands$tooltipHideFlags = new ArrayList<>();


    @Override
    public boolean taczexpands$isHidden() {
        return taczexpands$hidden;
    }

    @Override
    public boolean taczexpands$isMisc() {
        return taczexpands$misc;
    }

    @Override
    public boolean taczexpands$shouldPreview() {
        return taczexpands$preview;
    }

    @Override
    public float taczexpands$getPreviewScale() {
        return taczexpands$previewScale;
    }

    @Override
    public float[] taczexpands$getPreviewOffset() {
        return new float[]{taczexpands$previewOffsetX, taczexpands$previewOffsetY, taczexpands$previewOffsetZ};
    }

    @Override
    public String taczexpands$getPreviewTitle() {
        return taczexpands$previewTitle;
    }

    @Override
    public String taczexpands$getPreviewDetail() {
        return taczexpands$previewDetail;
    }

    @Override
    public List<String> taczexpands$getTooltipHideFlags() {
        return taczexpands$tooltipHideFlags;
    }

}
