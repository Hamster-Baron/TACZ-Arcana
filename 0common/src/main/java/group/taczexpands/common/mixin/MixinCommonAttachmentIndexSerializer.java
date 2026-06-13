package group.taczexpands.common.mixin;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.serialize.CommonAttachmentIndexSerializer;
import group.taczexpands.common.mixin.accessor.IAccessorAttachmentIndexPOJO;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(value = CommonAttachmentIndexSerializer.class, remap = false)
public class MixinCommonAttachmentIndexSerializer {
}
