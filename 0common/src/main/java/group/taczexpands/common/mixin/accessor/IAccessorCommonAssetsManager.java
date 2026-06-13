package group.taczexpands.common.mixin.accessor;

import com.tacz.guns.resource.CommonAssetsManager;
import com.tacz.guns.resource.index.CommonAttachmentIndex;
import com.tacz.guns.resource.manager.CommonDataManager;
import com.tacz.guns.resource.pojo.data.attachment.AttachmentData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(value = CommonAssetsManager.class, remap = false)
public interface IAccessorCommonAssetsManager {
    @Accessor("attachmentIndex")
    CommonDataManager<CommonAttachmentIndex> getAttachmentIndex();

    @Accessor("attachmentData")
    CommonDataManager<AttachmentData> getAttachmentData();
}
