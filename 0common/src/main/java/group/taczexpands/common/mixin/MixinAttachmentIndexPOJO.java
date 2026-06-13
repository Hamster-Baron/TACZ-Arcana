package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import group.taczexpands.common.accessor.IAccessorMiscPOJO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AttachmentIndexPOJO.class, remap = false)
public class MixinAttachmentIndexPOJO implements IAccessorMiscPOJO {
    @Shadow
    private boolean hidden;
    @Unique
    @SerializedName("misc")
    private boolean taczexpands$misc = false;


    @Override
    public boolean taczexpands$isHidden() {
        return hidden;
    }

    @Override
    public boolean taczexpands$isMisc() {
        return taczexpands$misc;
    }
}
