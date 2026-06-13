package group.taczexpands.client.accessor;

import com.tacz.guns.client.resource.GunDisplayInstance;
import com.tacz.guns.client.resource.pojo.display.gun.GunDisplay;

public interface IAccessorGunDisplay {
    static boolean getHideMuzzleFlash(GunDisplay display) {
        return ((IAccessorGunDisplay) display).taczexpands$getHideMuzzleFlash();
    }

    static boolean getHideShell(GunDisplay display) {
        return ((IAccessorGunDisplay) display).taczexpands$getHideShell();
    }

    static boolean getHideMuzzleFlash(GunDisplayInstance display) {
        return ((IAccessorGunDisplay) display).taczexpands$getHideMuzzleFlash();
    }

    static boolean getHideShell(GunDisplayInstance display) {
        return ((IAccessorGunDisplay) display).taczexpands$getHideShell();
    }

    boolean taczexpands$getHideMuzzleFlash();

    boolean taczexpands$getHideShell();
}
