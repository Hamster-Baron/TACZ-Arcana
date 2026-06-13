package group.taczexpands.client.mixin.accessor;

import com.tacz.guns.client.resource.manager.PackInfoManager;
import com.tacz.guns.client.resource.pojo.PackInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(value = PackInfoManager.class, remap = false)
public interface IAccessorPackInfoManager {
    @Accessor("dataMap")
    Map<String, PackInfo> getDataMap();
}
