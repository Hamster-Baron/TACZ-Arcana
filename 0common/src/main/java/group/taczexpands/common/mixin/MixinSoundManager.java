package group.taczexpands.common.mixin;

import com.tacz.guns.network.NetworkHandler;
import com.tacz.guns.network.message.ServerMessageSound;
import com.tacz.guns.sound.SoundManager;
import group.taczexpands.common.nbt.GunExtras;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraftforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = SoundManager.class, remap = false)
public class MixinSoundManager {
    @Inject(method = "sendSoundToNearby", at = @At("HEAD"), cancellable = true)
    private static void hookSendSoundToNearby(LivingEntity sourceEntity, int distance, ResourceLocation gunId, ResourceLocation gunDisplayId, String soundName, float volume, float pitch, CallbackInfo ci) {
        if (!(sourceEntity.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        ci.cancel();
        BlockPos pos = sourceEntity.blockPosition();
        var item = sourceEntity.getMainHandItem();

        if (!item.isEmpty()) {
            var newId = GunExtras.INSTANCE.getSoundGunDisplayId(item);
            if (newId != null) {
                gunDisplayId = newId;
            }
        }

        ServerMessageSound soundMessage = new ServerMessageSound(sourceEntity.getId(), gunId, gunDisplayId, soundName, volume, pitch, distance);
        serverLevel.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false).stream()
                .filter(p -> p.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) < distance * distance)
                .filter(p -> p.getId() != sourceEntity.getId())
                .forEach(p -> NetworkHandler.CHANNEL.send(PacketDistributor.PLAYER.with(() -> p), soundMessage));
    }
}
