package group.taczexpands.client.mixin.accessor;

import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import org.openjdk.nashorn.internal.objects.annotations.Getter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(value = ClientAmmoIndex.class, remap = false)
public interface IAccessorClientAmmoIndex {
    @Accessor("display")
    AmmoDisplay taczexpands$getDisplay();
}
