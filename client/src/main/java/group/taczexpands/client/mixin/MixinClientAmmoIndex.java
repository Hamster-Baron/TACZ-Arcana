package group.taczexpands.client.mixin;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.client.resource.pojo.display.ammo.AmmoDisplay;
import group.taczexpands.client.accessor.IAccessorAmmoParticle;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;


@Mixin(value = ClientAmmoIndex.class, remap = false)
public class MixinClientAmmoIndex {
    @Inject(method = "checkParticle", at = @At("RETURN"))
    private static void taczexpands$postCheckParticle(AmmoDisplay display, ClientAmmoIndex index, CallbackInfo ci) {
        if (index.getParticle() == null) return;
        var current = IAccessorAmmoParticle.getNext(index.getParticle());
        while (current != null) {
            try {
                String name = current.getName();
                if (StringUtils.isNoneBlank()) {
                    current.setParticleOptions(ParticleArgument.readParticle(new StringReader(name), BuiltInRegistries.PARTICLE_TYPE.asLookup()));
                    Preconditions.checkArgument(current.getCount() > 0, "particle count must be greater than 0");
                    Preconditions.checkArgument(current.getLifeTime() > 0, "particle life time must be greater than 0");
                }
            } catch (CommandSyntaxException e) {
                e.fillInStackTrace();
            }

            current = IAccessorAmmoParticle.getNext(current);
        }
    }
}
