package group.taczexpands.common.accessor;

import com.tacz.guns.resource.pojo.AttachmentIndexPOJO;
import com.tacz.guns.resource.pojo.GunIndexPOJO;

public interface IAccessorMiscPOJO {
    boolean taczexpands$isHidden();
    boolean taczexpands$isMisc();

    static boolean isHidden(AttachmentIndexPOJO instance) {
        return ((IAccessorMiscPOJO)instance).taczexpands$isHidden();
    }

    static boolean isHidden(GunIndexPOJO instance) {
        return ((IAccessorMiscPOJO)instance).taczexpands$isHidden();
    }

    static boolean isMisc(AttachmentIndexPOJO instance) {
        return ((IAccessorMiscPOJO)instance).taczexpands$isMisc();
    }

    static boolean isMisc(GunIndexPOJO instance) {
        return ((IAccessorMiscPOJO)instance).taczexpands$isMisc();
    }
}
