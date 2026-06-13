package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import group.taczexpands.common.accessor.IAccessorAttachmentData;
import group.taczexpands.common.data.AttachmentExtraHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = AttachmentData.class,remap = false)
public class MixinAttachmentData implements IAccessorAttachmentData {
    @Unique
    @SerializedName("extras")
    private AttachmentExtraHolder taczexpands$extras = new AttachmentExtraHolder();

    @Override
    public AttachmentExtraHolder taczexpands$getExtraHolder() {
        return taczexpands$extras;
    }
}
