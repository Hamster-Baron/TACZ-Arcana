package group.taczexpands.common.mixin.accessor;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.resource.pojo.data.gun.GunData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.List;

@Mixin(value = GunData.class, remap = false)
public interface IAccessorGunData {
    @Accessor("allowAttachments")
    void taczexpands$setAllowAttachments(List<AttachmentType> value);
}
