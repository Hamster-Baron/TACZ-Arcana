package group.taczexpands.common.accessor;

import com.tacz.guns.resource.pojo.data.gun.BulletData;
import group.taczexpands.common.data.BulletExtraHolder;

public interface IAccessorBulletData {
    static BulletExtraHolder getBulletExtraHolder(BulletData bulletData) {
        return ((IAccessorBulletData) bulletData).taczexpands$getExtraHolder();
    }

    BulletExtraHolder taczexpands$getExtraHolder();
}
