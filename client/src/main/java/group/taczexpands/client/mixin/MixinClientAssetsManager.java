package group.taczexpands.client.mixin;

import com.tacz.guns.api.item.attachment.AttachmentType;
import com.tacz.guns.client.resource.ClientAssetsManager;
import com.tacz.guns.client.resource.manager.PackInfoManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import group.taczexpands.client.accessor.IAccessorClientAssetsManager;
import group.taczexpands.client.mixin.accessor.IAccessorPackInfoManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(value = ClientAssetsManager.class, remap = false)
public class MixinClientAssetsManager implements IAccessorClientAssetsManager {
    @Shadow
    private PackInfoManager packInfo;

    @Override
    public Map<String, PackInfo> taczexpands$getAllPackInfo() {
        return ((IAccessorPackInfoManager)packInfo).getDataMap();
    }

}
