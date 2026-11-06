package group.taczexpands.common.accessor;

import com.tacz.guns.resource.pojo.AmmoIndexPOJO;

public interface IAccessorAmmoIndexPOJO {
    boolean taczexpands$isHidden();

    static boolean isHidden(AmmoIndexPOJO instance) {
        return ((IAccessorAmmoIndexPOJO) instance).taczexpands$isHidden();
    }
}
