package group.taczexpands.client.mixin;

import com.tacz.guns.api.resource.ResourceManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;

@Mixin(value = ResourceManager.class, remap = false)
public class MixinResourceManager {
}
