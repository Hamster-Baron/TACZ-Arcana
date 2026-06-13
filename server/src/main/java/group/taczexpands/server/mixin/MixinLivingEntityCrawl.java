package group.taczexpands.server.mixin;

import com.tacz.guns.entity.shooter.LivingEntityCrawl;
import com.tacz.guns.entity.shooter.ShooterDataHolder;
import group.taczexpands.server.event.LivingEntityCrawlSetEvent;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = LivingEntityCrawl.class, remap = false)
public class MixinLivingEntityCrawl {
    @Shadow
    @Final
    private ShooterDataHolder data;
    @Shadow
    @Final
    private LivingEntity shooter;
    @Unique
    private boolean taczexpands$lastCrawling = false;

    @Inject(method = "tickCrawling", at = @At("HEAD"))
    private void onPreTickCrawling(CallbackInfo ci) {
        taczexpands$lastCrawling = this.data.isCrawling;
    }

    @Inject(method = "setCrawlPose", at = @At("TAIL"))
    private void onPostSetCrawlPose(CallbackInfo ci) {
        if (taczexpands$lastCrawling != this.data.isCrawling) {
            MinecraftForge.EVENT_BUS.post(new LivingEntityCrawlSetEvent(this.shooter, this.data.isCrawling));
        }
    }
}
