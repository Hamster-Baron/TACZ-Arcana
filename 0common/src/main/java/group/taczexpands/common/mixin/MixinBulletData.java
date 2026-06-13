package group.taczexpands.common.mixin;

import com.google.gson.annotations.SerializedName;
import com.tacz.guns.resource.pojo.data.gun.BulletData;
import group.taczexpands.common.accessor.IAccessorBulletData;
import group.taczexpands.common.data.BulletExtraHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(value = BulletData.class, remap = false)
public class MixinBulletData implements IAccessorBulletData {
    @Unique
    @SerializedName("extras")
    private BulletExtraHolder taczexpands$extras = new BulletExtraHolder();


    @Override
    public BulletExtraHolder taczexpands$getExtraHolder() {
        return taczexpands$extras;
    }
}
