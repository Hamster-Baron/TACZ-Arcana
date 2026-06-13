package group.taczexpands.common.accessor;

import com.tacz.guns.resource.pojo.data.gun.BulletData;
import group.taczexpands.common.data.HookData;

public interface IAccessorBullet {
    BulletData taczexpands$getBulletData();
    boolean taczexpands$isHook();
    HookData taczexpands$getHookData();
}
