package group.taczexpands.common.accessor;

import com.tacz.guns.resource.pojo.GunIndexPOJO;

import java.util.List;

public interface IAccessorGunIndexPOJO {
    List<String> taczexpands$getTooltipHideFlags();

    static List<String> getTooltipHideFlags(GunIndexPOJO gunIndexPOJO) {
        return ((IAccessorGunIndexPOJO) gunIndexPOJO).taczexpands$getTooltipHideFlags();
    }
}
