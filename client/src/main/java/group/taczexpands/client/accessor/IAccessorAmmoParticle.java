package group.taczexpands.client.accessor;

import com.tacz.guns.client.resource.pojo.display.ammo.AmmoParticle;
import org.jetbrains.annotations.Nullable;

public interface IAccessorAmmoParticle {
    @Nullable
    AmmoParticle taczexpands$getNext();

    static AmmoParticle getNext(AmmoParticle ammoParticle) {
        return ((IAccessorAmmoParticle) ammoParticle).taczexpands$getNext();
    }
}
