package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.pojo.AmmoIndexPOJO;
import group.taczexpands.common.accessor.IAccessorPreview;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AmmoIndexPOJO.class, remap = false)
public class MixinAmmoIndexPOJO implements IAccessorPreview {
    @Unique
    @SerializedName("preview")
    private boolean taczexpands$preview = false;

    @Unique
    @SerializedName("preview_scale")
    private float taczexpands$previewScale = 30.0f;

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
}