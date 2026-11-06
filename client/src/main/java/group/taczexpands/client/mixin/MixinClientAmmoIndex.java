package group.taczexpands.client.mixin;

import com.google.common.base.Preconditions;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.tacz.guns.client.resource.index.ClientAmmoIndex;
import com.tacz.guns.resource.pojo.AmmoIndexPOJO;
import group.taczexpands.client.accessor.IAccessorAmmoParticle;
import group.taczexpands.common.TACZExpandsCommon;
import net.minecraft.commands.arguments.ParticleArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import org.apache.commons.lang3.StringUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;


@Mixin(value = ClientAmmoIndex.class, remap = false)
public class MixinClientAmmoIndex {
    @Inject(method = "getInstance", at = @At("RETURN"))
    private static void taczexpands$postCheckParticle(AmmoIndexPOJO pojo, CallbackInfoReturnable<ClientAmmoIndex> cir) {
        ClientAmmoIndex index = cir.getReturnValue();
        if (index.getParticle() == null) return;
        var current = IAccessorAmmoParticle.getNext(index.getParticle());
        while (current != null) {
            try {
                String name = current.getName();
                if (StringUtils.isNoneBlank(name)) {
                    current.setParticleOptions(ParticleArgument.readParticle(new StringReader(name), BuiltInRegistries.PARTICLE_TYPE.asLookup()));
                    Preconditions.checkArgument(current.getCount() > 0, "particle count must be greater than 0");
                    Preconditions.checkArgument(current.getLifeTime() > 0, "particle life time must be greater than 0");
                }
            } catch (CommandSyntaxException e) {
                TACZExpandsCommon.Companion.getLOGGER().error(
                        "Failed to parse particle '{}' in ammo display '{}'", current.getName(), pojo.getDisplay(), e);
            }

            current = IAccessorAmmoParticle.getNext(current);
        }
    }
}
