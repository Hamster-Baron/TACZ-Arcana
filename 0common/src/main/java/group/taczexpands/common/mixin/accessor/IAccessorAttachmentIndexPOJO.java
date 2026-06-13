package group.taczexpands.common.mixin.accessor;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = AttachmentIndexPOJO.class, remap = false)
public interface IAccessorAttachmentIndexPOJO {
    @Accessor("type")
    void setType(AttachmentType type);
}